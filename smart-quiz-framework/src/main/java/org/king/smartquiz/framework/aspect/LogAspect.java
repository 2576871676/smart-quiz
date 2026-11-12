package org.king.smartquiz.framework.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.king.smartquiz.common.annotation.Log;
import org.king.smartquiz.framework.log.SysOperLog;
import org.king.smartquiz.framework.log.SysOperLogService;
import org.king.smartquiz.framework.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志切面：拦截所有标注 @Log 的方法，记录调用信息并写入 sys_oper_log。
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private static final int MAX_LEN = 2000;

    private final SysOperLogService sysOperLogService;

    public LogAspect(SysOperLogService sysOperLogService) {
        this.sysOperLogService = sysOperLogService;
    }

    @Pointcut("@annotation(org.king.smartquiz.common.annotation.Log)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Exception ex = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                handleLog(point, ex, cost, result);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    private void handleLog(ProceedingJoinPoint point, Exception ex, long cost, Object result) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Log logAnno = signature.getMethod().getAnnotation(Log.class);
        if (logAnno == null) {
            return;
        }

        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(logAnno.title());
        operLog.setBusinessType(logAnno.businessType().name());
        operLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName() + "()");
        operLog.setCostTime(cost);
        operLog.setStatus(ex == null ? 0 : 1);
        operLog.setOperName(UserContext.getUsername());
        if (ex != null) {
            operLog.setErrorMsg(StrUtil.sub(ex.getMessage(), 0, MAX_LEN));
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(getIp(request));
        }

        if (logAnno.saveParam()) {
            operLog.setOperParam(StrUtil.sub(JSONUtil.toJsonStr(filterArgs(point.getArgs())), 0, MAX_LEN));
        }
        if (logAnno.saveResult() && result != null) {
            operLog.setJsonResult(StrUtil.sub(JSONUtil.toJsonStr(result), 0, MAX_LEN));
        }

        sysOperLogService.save(operLog);
    }

    /** 过滤不可序列化参数（请求/响应对象、上传文件等） */
    private List<Object> filterArgs(Object[] args) {
        if (args == null) {
            return List.of();
        }
        return Arrays.stream(args)
                .filter(a -> !(a instanceof ServletRequest)
                        && !(a instanceof ServletResponse)
                        && !(a instanceof MultipartFile))
                .collect(Collectors.toList());
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能含多个 IP，取第一个
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
