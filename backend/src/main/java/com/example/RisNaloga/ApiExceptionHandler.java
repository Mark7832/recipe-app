package com.example.RisNaloga;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Enotna oblika napak: { status, error, message }. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Npr. podvojeno ime kategorije (unique omejitev v bazi). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "Operacija krši omejitve podatkovne baze (npr. podvojeno ime)");
    }

    /** Napake, ki jih sami sprožimo (404, 400, 409 ...), da odgovor vsebuje message. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Napaka";
        return body(ex.getStatusCode(), message);
    }

    /** Neveljavno telo zahtevka (validacija @Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Neveljavni podatki" : message);
    }

    /** Pokvarjen JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Telo zahtevka ni veljaven JSON");
    }

    /** Npr. ?page=abc ali ?categoryId=x. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, "Neveljavna vrednost parametra »" + ex.getName() + "«");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatusCode status, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status.value());
        map.put("error", status instanceof HttpStatus hs ? hs.getReasonPhrase() : "Error");
        map.put("message", message);
        return ResponseEntity.status(status).body(map);
    }
}
