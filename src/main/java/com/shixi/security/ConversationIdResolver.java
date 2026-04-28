package com.shixi.security;

import org.springframework.stereotype.Component;

@Component
public class ConversationIdResolver {

    public String resolve(String requestedChatId) {
        String requested = normalize(requestedChatId);
        return CurrentUserContext.get()
                .map(user -> "user-" + user.userId() + "-" + requested)
                .orElse(requested);
    }

    private String normalize(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return "default";
        }
        return chatId.trim();
    }
}
