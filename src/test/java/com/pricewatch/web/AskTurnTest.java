package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskTurnTest {
    private final CatalogService catalogue = mock(CatalogService.class);
    private final DeepSeekChatClient provider = mock(DeepSeekChatClient.class);
    private final AskShoppingTools shoppingTools = mock(AskShoppingTools.class);
    private final AskShoppingTools.Turn shoppingTurn = mock(AskShoppingTools.Turn.class);
    private final MockHttpSession session = new MockHttpSession();
    private final AskController ask = controller();

    @Test
    void acceptsMessageBeforeProviderCallAndRepliesOnceAcrossTwoRequests() throws Exception {
        var providerStarted = new CountDownLatch(1);
        var finishProvider = new CountDownLatch(1);
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenAnswer(invocation -> {
            providerStarted.countDown();
            assertTrue(finishProvider.await(5, TimeUnit.SECONDS));
            return Optional.of("Which pack size should I check?");
        });

        String question = "Could you check the milk?";
        String location = ask.send(question, "", session);
        String chatId = location.substring(location.indexOf('=') + 1);
        verifyNoInteractions(provider);
        assertPending(chatId, question);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> ask.send("Please also check the bread", chatId, session)).getStatusCode().value());

        try (var executor = Executors.newSingleThreadExecutor()) {
            var reply = executor.submit(() -> ask.replyToTurn(chatId, session));
            try {
                assertTrue(providerStarted.await(5, TimeUnit.SECONDS));
                assertEquals(location, ask.replyToTurn(chatId, session));
                assertPending(chatId, question);
            } finally {
                finishProvider.countDown();
            }
            assertEquals(location, reply.get(5, TimeUnit.SECONDS));
        }
        assertEquals(location, ask.replyToTurn(chatId, session));
        assertEquals(false, model(chatId).get("awaitingReply"));
        assertEquals(List.of(question, "Which pack size should I check?"), messageTexts());
        verify(provider).reply(anyString(), argThat(history -> history.equals(List.of(
                new DeepSeekChatClient.ChatMessage("user", question)))), anyList(), any());
    }

    @Test
    void replyCanBeRetriedWithoutResendingQuestion() {
        String question = "P000000124";
        when(provider.reply(anyString(), anyList(), anyList(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of("Which pack size should I check?"));
        String location = ask.send(question, "", session);
        String chatId = location.substring(location.indexOf('=') + 1);

        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> ask.replyToTurn(chatId, session)).getStatusCode().value());
        verifyNoInteractions(catalogue);
        assertPending(chatId, question);
        assertEquals(location, ask.replyToTurn(chatId, session));
        assertEquals(List.of(question, "Which pack size should I check?"), messageTexts());
    }

    @Test
    void providerRepliesNaturallyInTheRequestedLanguageWithoutSearching() {
        String introduction = "I'm Market Desk. I can check saved grocery prices.";
        when(provider.reply(anyString(), anyList(), anyList(), any()))
                .thenReturn(Optional.of(introduction))
                .thenReturn(Optional.of("可以，我可以用繁體中文幫你比較價格。"));
        String location = ask.ask("who are u", "", session);
        String chatId = location.substring(location.indexOf('=') + 1);
        assertEquals(introduction, messages().getLast().text());
        ask.ask("Can you speak Traditional Chinese?", chatId, session);
        assertEquals("可以，我可以用繁體中文幫你比較價格。", messages().getLast().text());
        verify(provider, times(2)).reply(anyString(), anyList(), anyList(), any());
        verify(provider).reply(anyString(), argThat(history -> history.equals(List.of(
                new DeepSeekChatClient.ChatMessage("user", "who are u"),
                new DeepSeekChatClient.ChatMessage("assistant", introduction),
                new DeepSeekChatClient.ChatMessage("user", "Can you speak Traditional Chinese?")))), anyList(), any());
        verifyNoInteractions(catalogue);
    }

    private AskController controller() {
        var messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        when(shoppingTools.open(any(), org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(AskShoppingTools.Draft.class))).thenReturn(shoppingTurn);
        return new AskController(catalogue, provider, mock(HelloController.class), messages, shoppingTools, Clock.systemUTC());
    }

    private void assertPending(String chatId, String question) {
        var model = model(chatId);
        assertEquals(true, model.get("awaitingReply"));
        assertEquals(List.of(question), messageTexts());
        assertTrue(messages().getFirst().shopper());
    }

    private ExtendedModelMap model(String chatId) {
        var model = new ExtendedModelMap();
        ask.show(chatId, false, model, session);
        return model;
    }

    private List<AskController.Message> messages() {
        return AskController.history(session).getFirst().messages();
    }

    private List<String> messageTexts() {
        return messages().stream().map(AskController.Message::text).toList();
    }

    @Test
    void preparedAddIsOnlyAppliedAfterSuccessfulReply() {
        var addition = mock(AskShoppingTools.Operation.class);
        when(shoppingTurn.operations()).thenReturn(List.of(addition));
        when(shoppingTools.apply(any(), any())).thenReturn(new AskShoppingTools.Card("receipt", "Added", "", List.of(), null));
        when(provider.reply(anyString(), anyList(), anyList(), any()))
                .thenReturn(Optional.empty()).thenReturn(Optional.of("Ready to add."));
        String chatId = ask.send("Add one pack", "", session).split("=")[1];

        assertThrows(ResponseStatusException.class, () -> ask.replyToTurn(chatId, session));
        verify(shoppingTools, times(0)).apply(any(), any());
        ask.replyToTurn(chatId, session);
        ask.replyToTurn(chatId, session);
        verify(shoppingTools, times(1)).apply(any(), any());
        assertEquals("Added", messages().getLast().cards().getFirst().title());
    }

    @Test
    void confirmationIsBoundToItsChatAndConsumesTheActionOnce() {
        var removal = mock(AskShoppingTools.Operation.class);
        when(removal.requiresConfirmation()).thenReturn(true);
        when(removal.preview()).thenReturn(new AskShoppingTools.Card("confirmation", "Remove", "", List.of(), null));
        when(shoppingTurn.operations()).thenReturn(List.of(removal));
        when(shoppingTools.apply(any(), any())).thenReturn(new AskShoppingTools.Card("receipt", "Removed", "", List.of(), null));
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenReturn(Optional.of("Ready to remove."));
        String chatId = ask.ask("Remove a pack", "", session).split("=")[1];
        String actionId = messages().getLast().actions().getFirst().id();
        ask.action("different-chat", actionId, "confirm", session);
        verify(shoppingTools, times(0)).apply(any(), any());
        ask.action(chatId, actionId, "confirm", session);
        ask.action(chatId, actionId, "confirm", session);
        verify(shoppingTools, times(1)).apply(any(), any());
        assertEquals("Removed", messages().getLast().cards().getFirst().title());
    }

    @Test
    void cancelConsumesTheProposalWithoutApplyingIt() {
        var removal = mock(AskShoppingTools.Operation.class);
        when(removal.requiresConfirmation()).thenReturn(true);
        when(removal.preview()).thenReturn(new AskShoppingTools.Card("confirmation", "Remove", "", List.of(), null));
        when(shoppingTurn.operations()).thenReturn(List.of(removal));
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenReturn(Optional.of("Ready to remove."));
        String chatId = ask.ask("Remove a pack", "", session).split("=")[1];
        String actionId = messages().getLast().actions().getFirst().id();

        ask.action(chatId, actionId, "cancel", session);
        ask.action(chatId, actionId, "confirm", session);
        verify(shoppingTools, times(0)).apply(any(), any());
        assertEquals("CANCELLED", messages().get(1).actions().getFirst().state());
    }

    @Test
    void failedConfirmationShowsAnErrorWithoutAReceiptAndCannotBeReplayed() {
        var removal = mock(AskShoppingTools.Operation.class);
        when(removal.requiresConfirmation()).thenReturn(true);
        when(removal.preview()).thenReturn(new AskShoppingTools.Card("confirmation", "Remove", "", List.of(), null));
        when(shoppingTurn.operations()).thenReturn(List.of(removal));
        when(provider.reply(anyString(), anyList(), anyList(), any())).thenReturn(Optional.of("Ready to remove."));
        when(shoppingTools.apply(any(), any())).thenThrow(new DataIntegrityViolationException("Test write failure"));
        String location = ask.ask("Remove a pack", "", session);
        String chatId = AskController.history(session).getFirst().id();
        String actionId = messages().getLast().actions().getFirst().id();

        assertEquals(location, ask.action(chatId, actionId, "confirm", session));
        assertEquals("FAILED", messages().get(1).actions().getFirst().state());
        assertEquals("chat.actionFailed", messages().getLast().text());
        assertTrue(messages().getLast().cards().isEmpty());
        int messageCount = messages().size();
        assertEquals(location, ask.action(chatId, actionId, "confirm", session));
        assertEquals(messageCount, messages().size());
        verify(shoppingTools).apply(any(), any());
    }
}
