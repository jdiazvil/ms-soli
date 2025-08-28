package pe.crediya.solicitudes.r2dbc.helper;

import org.springframework.dao.DataIntegrityViolationException;
import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.exception.BusinessException;

public final class R2dbcErrorMapper {
    private R2dbcErrorMapper() {}

    private static final String DUPLICATE_KEY = "23505";
    private static final String FK_VIOLATION  = "23503";
    private static final String CHECK_FAILED  = "23514";

    public static Throwable toBusiness(Throwable t) {
        if (!(t instanceof DataIntegrityViolationException)) {
            return new BusinessException(ErrorCode.DATA_INTEGRITY, "Error de base de datos", t);
        }
        String sql = extractSqlState(t);

        if (DUPLICATE_KEY.equals(sql)) {
            return new BusinessException(ErrorCode.DUPLICATE_KEY, "Recurso duplicado", t);
        }
        if (FK_VIOLATION.equals(sql)) {
            return new BusinessException(ErrorCode.FOREIGN_KEY_VIOLATION, "Referencia inválida", t);
        }
        if (CHECK_FAILED.equals(sql)) {
            return new BusinessException(ErrorCode.CHECK_VIOLATION, "Violación de restricción", t);
        }
        return new BusinessException(ErrorCode.DATA_INTEGRITY, "Integridad de datos", t);
    }

    private static String extractSqlState(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            try {
                var m = t.getClass().getMethod("getSqlState");
                Object v = m.invoke(t);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (ReflectiveOperationException ignore) {}
            t = t.getCause();
        }
        return null;
    }
}
