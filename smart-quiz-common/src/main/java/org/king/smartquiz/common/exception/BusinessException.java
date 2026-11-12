package org.king.smartquiz.common.exception;

import org.king.smartquiz.common.core.ResultCode;

/**
 * 业务异常：由 GlobalExceptionHandler 统一捕获并转换为 Result。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BusinessException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
