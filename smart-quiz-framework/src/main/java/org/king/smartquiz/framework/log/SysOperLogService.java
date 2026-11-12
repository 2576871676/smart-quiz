package org.king.smartquiz.framework.log;

import org.springframework.stereotype.Service;

/**
 * 操作日志服务。
 * 说明：生产环境建议用 @Async 异步落库，避免阻塞主请求；
 * 异步时 UserContext（ThreadLocal）不跨线程，需在切面中先取好数据再传入。
 */
@Service
public class SysOperLogService {

    private final SysOperLogMapper sysOperLogMapper;

    public SysOperLogService(SysOperLogMapper sysOperLogMapper) {
        this.sysOperLogMapper = sysOperLogMapper;
    }

    public void save(SysOperLog operLog) {
        if (operLog.getOperTime() == null) {
            operLog.setOperTime(java.time.LocalDateTime.now());
        }
        sysOperLogMapper.insert(operLog);
    }
}
