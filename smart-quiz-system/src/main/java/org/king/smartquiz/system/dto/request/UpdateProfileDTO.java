package org.king.smartquiz.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求参数。仅允许改昵称/头像/邮箱/手机号，用户名与角色不可改。
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称最长 50 位")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 位")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
