package org.king.smartquiz.system.service;

import org.king.smartquiz.system.dto.request.LoginDTO;
import org.king.smartquiz.system.dto.request.RegisterDTO;
import org.king.smartquiz.system.vo.LoginVO;

/**
 * 认证服务：注册 / 登录 / 登出。
 */
public interface AuthService {

    /** 用户注册 */
    void register(RegisterDTO dto);

    /** 用户登录，返回 JWT 令牌与用户信息 */
    LoginVO login(LoginDTO dto);

    /** 退出登录，清除 Redis 登录态 */
    void logout();
}
