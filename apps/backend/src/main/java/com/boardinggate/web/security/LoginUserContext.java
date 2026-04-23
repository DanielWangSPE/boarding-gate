package com.boardinggate.web.security;

/**
 * 请求级别的登录用户上下文。
 * <p>
 * 使用 {@link ThreadLocal} 存储；过滤器在进入时写入、在 {@code finally} 中强制清理，
 * 防止 Servlet 容器的线程复用导致脏数据。
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 可能为 null：调用方通常只应在过滤器放行之后的代码里使用。 */
    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String currentSessionId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getSessionId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
