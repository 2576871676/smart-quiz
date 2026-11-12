package org.king.smartquiz.system.controller;

import jakarta.validation.Valid;
import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.system.dto.request.UpdatePasswordDTO;
import org.king.smartquiz.system.dto.request.UpdateProfileDTO;
import org.king.smartquiz.system.service.UserService;
import org.king.smartquiz.system.vo.UserVO;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息控制器（均为受保护接口，需携带有效 JWT）。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.success(userService.getCurrentUserInfo());
    }

    /** 修改个人资料 */
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public Result<Void> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success("资料修改成功", null);
    }

    /** 修改密码（不记录入参，避免密码泄露到操作日志） */
    @Log(title = "用户管理", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody @Valid UpdatePasswordDTO dto) {
        userService.updatePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
