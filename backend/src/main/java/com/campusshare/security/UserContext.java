package com.campusshare.security;

public class UserContext {

    private static final ThreadLocal<UserPrincipal> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal principal) {
        CONTEXT.set(principal);
    }

    public static UserPrincipal get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserPrincipal principal = CONTEXT.get();
        return principal == null ? null : principal.getId();
    }

    public static String getRole() {
        UserPrincipal principal = CONTEXT.get();
        return principal == null ? null : principal.getRole();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
