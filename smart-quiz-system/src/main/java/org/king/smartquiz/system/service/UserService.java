package org.king.smartquiz.system.service;

import org.king.smartquiz.system.dto.request.UpdatePasswordDTO;
import org.king.smartquiz.system.dto.request.UpdateProfileDTO;
import org.king.smartquiz.system.vo.UserVO;

/**
 * 用户信息服务：查看 / 修改个人资料 / 修改密码。
 */
public interface UserService {

    /** 获取当前登录用户信息 */
    UserVO getCurrentUserInfo();

    /** 修改当前登录用户的个人资料 */
    void updateProfile(UpdateProfileDTO dto);

    /** 修改当前登录用户的密码（需验证原密码） */
    void updatePassword(UpdatePasswordDTO dto);
}
