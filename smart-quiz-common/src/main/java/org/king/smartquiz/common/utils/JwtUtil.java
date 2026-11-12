package org.king.smartquiz.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.king.smartquiz.common.constant.TokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成 / 解析令牌。
 * 采用 HS256 签名，密钥长度需 ≥ 256bit（32 字节）。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** 过期时间（秒） */
    @Value("${jwt.expire:1800}")
    private long expire;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌
     *
     * @param uuid 关联 Redis 登录态的唯一标识
     */
    public String createToken(Long userId, String username, Integer role, String uuid) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire * 1000);
        return Jwts.builder()
                .claim(TokenConstants.CLAIM_USER_ID, userId)
                .claim(TokenConstants.CLAIM_USERNAME, username)
                .claim(TokenConstants.CLAIM_ROLE, role)
                .claim(TokenConstants.CLAIM_UUID, uuid)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key())
                .compact();
    }

    /**
     * 解析令牌，失败（签名错误 / 过期）抛出异常。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpire() {
        return expire;
    }
}
