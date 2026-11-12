package org.king.smartquiz.system.controller;

import org.king.smartquiz.common.annotation.BusinessType;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.common.core.Result;
import org.king.smartquiz.framework.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 测试控制器：验证多模块工程、统一返回、AOP 日志切面是否正常。
 * /test/** 已在拦截器白名单中，无需登录即可访问。
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("智能出题与批改系统启动成功！");
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", "smart-quiz-system");
        info.put("modules", "common / framework / system / admin");
        info.put("time", LocalDateTime.now().toString());
        return Result.success(info);
    }

    /** 演示 @Log 注解：调用后会在 sys_oper_log 生成一条操作日志 */
    @Log(title = "测试模块", businessType = BusinessType.OTHER, saveResult = true)
    @GetMapping("/log-demo")
    public Result<String> logDemo() {
        Long userId = UserContext.getUserId();
        return Result.success("AOP 日志切面演示，当前登录用户ID=" + userId);
    }
}
