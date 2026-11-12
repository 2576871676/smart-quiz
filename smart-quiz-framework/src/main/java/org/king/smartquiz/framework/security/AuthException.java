package org.king.smartquiz.framework.security;

/**
 * 认证异常：Token 缺失 / 无效 / 过期，由全局异常处理器转换为 401。
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
