package com.pricewatch.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {HelloController.class, TrackingController.class, AlertCenterController.class})
class AskReturnLinks {
    @ModelAttribute
    void addAskReturnLink(HttpServletRequest request, Model model) {
        String chatId = request.getParameter("askChatId");
        String messageId = request.getParameter("askMessageId");
        if (chatId == null || messageId == null) return;

        var chats = AskController.history(request.getSession(false));
        for (var chat : chats) {
            if (chat.id().equals(chatId) && chat.messages().stream().anyMatch(message -> message.id().equals(messageId))) {
                model.addAttribute("askReturnChatId", chatId);
                model.addAttribute("askReturnMessageId", messageId);
                model.addAttribute("askReturnUrl", request.getContextPath() + "/ask?chatId=" + chatId + "#ask-message-" + messageId);
                return;
            }
        }
    }
}
