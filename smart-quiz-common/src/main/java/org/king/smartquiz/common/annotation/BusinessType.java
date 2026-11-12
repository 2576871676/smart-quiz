package org.king.smartquiz.common.annotation;

/**
 * 业务操作类型（用于 AOP 操作日志）
 */
public enum BusinessType {

    /** 其它 */
    OTHER,
    /** 新增 */
    INSERT,
    /** 修改 */
    UPDATE,
    /** 删除 */
    DELETE,
    /** 授权 */
    GRANT,
    /** 导出 */
    EXPORT,
    /** 导入 */
    IMPORT,
    /** 登录 */
    LOGIN,
    /** 登出 */
    LOGOUT
}
