package org.king.smartquiz.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息返回对象（不含密码等敏感字段）。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    /** 角色：0-学生，1-管理员 */
    private Integer role;
    /** 状态：0-禁用，1-正常 */
    private Integer status;
    private LocalDateTime createTime;
}
