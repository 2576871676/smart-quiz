package org.king.smartquiz.framework.security;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息，缓存于 Redis（login:token:{uuid}）。
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Integer role;
    /** 关联 Redis 登录态的 uuid */
    private String uuid;
    private Long loginTime;
    private Long expireTime;
}
