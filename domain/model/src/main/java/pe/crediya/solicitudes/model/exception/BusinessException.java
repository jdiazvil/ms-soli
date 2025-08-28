package pe.crediya.solicitudes.model.exception;

import pe.crediya.solicitudes.model.common.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
