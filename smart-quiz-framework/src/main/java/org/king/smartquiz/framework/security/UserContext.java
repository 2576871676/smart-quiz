package org.king.smartquiz.framework.security;

/**
 * 基于 ThreadLocal 的用户上下文。
 * 由 JwtAuthInterceptor 在 preHandle 写入、afterCompletion 清理，
 * 业务层可随时获取当前登录用户。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getUsername() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    public static Integer getRole() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRole();
    }

    /** 必须在拦截器 afterCompletion 中调用，防止线程复用导致内存泄漏 / 用户串号 */
    public static void clear() {
        HOLDER.remove();
    }
}
