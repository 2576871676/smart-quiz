package org.king.smartquiz.framework.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体（由 LogAspect 切面写入），对应表 sys_oper_log。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块标题 */
    private String title;

    /** 业务类型 */
    private String businessType;

    /** 请求方法（类.方法） */
    private String method;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求 URL */
    private String operUrl;

    /** 操作 IP */
    private String operIp;

    /** 操作人用户名 */
    private String operName;

    /** 请求参数 */
    private String operParam;

    /** 返回结果 */
    private String jsonResult;

    /** 操作状态：0-正常，1-异常 */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 消耗时间（毫秒） */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
