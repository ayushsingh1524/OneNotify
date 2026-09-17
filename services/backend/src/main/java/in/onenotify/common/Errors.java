package in.onenotify.common;

import java.time.Instant;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class Errors {
  private ResponseEntity<?> response(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "timestamp",
                Instant.now().toString(),
                "status",
                status,
                "code",
                code,
                "message",
                message,
                "correlationId",
                Objects.toString(MDC.get("correlationId"), "unknown")));
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> api(ApiException e) {
    return response(e.status, e.code, e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> validation(Exception e) {
    return response(400, "VALIDATION_ERROR", "Check the supplied fields and try again.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(Exception e) {
    return response(409, "CONFLICT", "This item already exists or is in use.");
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<?> size(Exception e) {
    return response(413, "FILE_TOO_LARGE", "Choose a file smaller than 10 MB.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unknown(Exception e) {
    org.slf4j.LoggerFactory.getLogger(Errors.class)
        .error("Unhandled failure type={}", e.getClass().getName());
    return response(500, "INTERNAL_ERROR", "We could not complete this action. Please try again.");
  }
}
