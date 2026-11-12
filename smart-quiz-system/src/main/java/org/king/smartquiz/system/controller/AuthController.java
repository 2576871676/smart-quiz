package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.LoginDTO;
import org.king.smartquiz.system.dto.request.RegisterDTO;
import org.king.smartquiz.system.service.AuthService;
import org.king.smartquiz.system.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器：注册 / 登录 / 登出。
 * /auth/login 与 /auth/register 在拦截器白名单中，无需令牌；
 * /auth/logout 需携带令牌（据此清除对应 Redis 登录态）。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Log(title = "认证", businessType = BusinessType.LOGIN, saveParam = false)
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO dto) {
        authService.register(dto);
        return Result.success("注册成功", null);
    }

    @Log(title = "认证", businessType = BusinessType.LOGIN, saveParam = false)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.success("登录成功", vo);
    }

    @Log(title = "认证", businessType = BusinessType.LOGOUT)
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功", null);
    }
}
