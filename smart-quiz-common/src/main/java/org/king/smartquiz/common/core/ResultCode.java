package org.king.smartquiz.common.core;

/**
 * 状态码枚举
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),

    USER_ERROR(1001, "用户相关错误"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USERNAME_EXISTS(1004, "用户名已存在"),

    QUESTION_ERROR(2001, "题目相关错误"),

    AI_ERROR(3001, "AI 调用失败"),
    AI_PARSE_ERROR(3002, "AI 返回结果解析失败"),

    FILE_ERROR(4001, "文件相关错误"),
    REPEAT_SUBMIT(4002, "请勿重复提交"),
    TOO_MANY_REQUESTS(4003, "请求过于频繁，请稍后再试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
