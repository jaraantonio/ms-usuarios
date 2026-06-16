package com.perfulandia.usuarios.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 400, "Error de Validacion", errores),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciales(CredencialesInvalidasException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 401, "No Autorizado", ex.getMessage()),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(RecursoDuplicadoException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 409, "Conflicto", ex.getMessage()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 404, "No Encontrado", ex.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), 400, "Error de negocio", ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }
}