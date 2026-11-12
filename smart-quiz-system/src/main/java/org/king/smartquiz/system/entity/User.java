package org.king.smartquiz.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体，对应表 sys_user。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    /** 角色：0-学生，1-管理员 */
    private Integer role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;
}
