package org.king.smartquiz.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.king.smartquiz.common.constant.CacheConstants;
import org.king.smartquiz.common.constant.TokenConstants;
import org.king.smartquiz.common.utils.JwtUtil;
import org.king.smartquiz.common.utils.RedisUtil;
import org.king.smartquiz.framework.security.AuthException;
import org.king.smartquiz.framework.security.LoginUser;
import org.king.smartquiz.framework.security.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * JWT 登录拦截器：
 * 1. 解析 Authorization 头中的 JWT，校验签名与有效期；
 * 2. 用 token 中的 uuid 从 Redis 取登录态（支持主动登出 / 续期）；
 * 3. 写入 UserContext，供业务层获取当前用户；
 * 4. afterCompletion 清理 ThreadLocal。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 登录态滑动续期时长（分钟） */
    private static final long RENEW_MINUTES = 30;

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil, RedisUtil redisUtil) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(TokenConstants.AUTHENTICATION);
        if (StrUtil.isBlank(header) || !header.startsWith(TokenConstants.PREFIX)) {
            throw new AuthException("未提供登录令牌");
        }

        String token = header.substring(TokenConstants.PREFIX.length());
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            throw new AuthException("登录令牌无效或已过期");
        }

        String uuid = claims.get(TokenConstants.CLAIM_UUID, String.class);
        LoginUser loginUser = redisUtil.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + uuid);
        if (loginUser == null) {
            throw new AuthException("登录状态已失效，请重新登录");
        }

        // 滑动续期
        redisUtil.expire(CacheConstants.LOGIN_TOKEN_KEY + uuid, RENEW_MINUTES, TimeUnit.MINUTES);
        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
