package com.shixi.security;

import java.util.Optional;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static CurrentUser require() {
        return get().orElseThrow(() -> new UnauthorizedException("请先登录"));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
