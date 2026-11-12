package org.king.smartquiz.system.vo;

import lombok.Data;

/**
 * 登录成功返回信息（不含密码等敏感字段）。
 */
@Data
public class LoginVO {

    /** JWT 令牌，前端后续请求需携带 Authorization: Bearer {token} */
    private String token;

    private Long userId;
    private String username;
    private String nickname;
    /** 角色：0-学生，1-管理员 */
    private Integer role;
}
