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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskTurnTest {
    private final ChatIntentClient provider = mock(ChatIntentClient.class);
    private final MockHttpSession session = new MockHttpSession();
    private final AskController ask = controller();

    @Test
    void acceptsMessageBeforeProviderCallAndRepliesOnceAcrossTwoRequests() throws Exception {
        var providerStarted = new CountDownLatch(1);
        var finishProvider = new CountDownLatch(1);
        when(provider.interpret(anyString(), anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            providerStarted.countDown();
            assertTrue(finishProvider.await(5, TimeUnit.SECONDS));
            return Optional.of(new ChatIntentClient.Intent("clarify", "", "", "pack"));
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
                new AskController.Message(false, "chat.clarify.pack")), messages());
        verify(provider).interpret(question, "", List.of(), List.of());
    }

    @Test
    void replyCanBeRetriedWithoutResendingQuestion() {
        String question = "What is the price for the red apples?";
        when(provider.interpret(anyString(), anyString(), anyList(), anyList()))
                .thenThrow(new IllegalStateException("Provider fixture interrupted"))
                .thenReturn(Optional.of(new ChatIntentClient.Intent("clarify", "", "", "pack")));
        String location = ask.send(question, "", session);
        String chatId = location.substring(location.indexOf('=') + 1);

        assertThrows(IllegalStateException.class, () -> ask.replyToTurn(chatId, session));
        assertPending(chatId, question);
        assertEquals(location, ask.replyToTurn(chatId, session));
        assertEquals(List.of(new AskController.Message(true, question),
                new AskController.Message(false, "chat.clarify.pack")), messages());
    }

    @Test
    void greetsIntroducesItselfAndAcknowledgesThanksWithoutSearching() {
        String location = ask.ask("who are u", "", session);
        String chatId = location.substring(location.indexOf('=') + 1);
        assertEquals("chat.conversation.identity", messages().getLast().text());
        ask.ask("hello!", chatId, session);
        assertEquals("chat.conversation.greeting", messages().getLast().text());
        ask.ask("thank you.", chatId, session);
        assertEquals("chat.conversation.thanks", messages().getLast().text());
        verifyNoInteractions(provider);
    }

    private AskController controller() {
        var messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        return new AskController(mock(CatalogService.class), provider, mock(HelloController.class), messages);
    }

    private void assertPending(String chatId, String question) {
        var model = model(chatId);
        assertEquals(true, model.get("awaitingReply"));
        assertEquals(List.of(new AskController.Message(true, question)), model.get("messages"));
    }

    private ExtendedModelMap model(String chatId) {
        var model = new ExtendedModelMap();
        ask.show(chatId, model, session);
        return model;
    }

    private List<AskController.Message> messages() {
        return AskController.history(session).getFirst().messages();
    }
}
