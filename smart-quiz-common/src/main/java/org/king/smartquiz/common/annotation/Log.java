package org.king.smartquiz.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解：标注在 Controller 方法上，由 LogAspect 切面记录到 sys_oper_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 模块标题 */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 是否保存请求参数 */
    boolean saveParam() default true;

    /** 是否保存返回结果 */
    boolean saveResult() default false;
}
