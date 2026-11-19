package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

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
        assertEquals(List.of(new AskController.Message(true, question),
                new AskController.Message(false, "Which pack size should I check?")), messages());
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
        assertEquals(List.of(new AskController.Message(true, question),
                new AskController.Message(false, "Which pack size should I check?")), messages());
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
        return new AskController(catalogue, provider, mock(HelloController.class), messages);
    }

    private void assertPending(String chatId, String question) {
        var model = model(chatId);
        assertEquals(true, model.get("awaitingReply"));
        assertEquals(List.of(new AskController.Message(true, question)), model.get("messages"));
    }

    private ExtendedModelMap model(String chatId) {
        var model = new ExtendedModelMap();
        ask.show(chatId, false, model, session);
        return model;
    }

    private List<AskController.Message> messages() {
        return AskController.history(session).getFirst().messages();
    }
}
