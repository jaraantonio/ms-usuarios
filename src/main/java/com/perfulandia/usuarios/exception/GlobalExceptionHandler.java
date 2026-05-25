package com.perfulandia.usuarios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));
        
        return new ResponseEntity<>(
            new ErrorResponse(LocalDateTime.now(), 400, "Error de Validacion", errores), 
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciales(CredencialesInvalidasException ex) {
        return new ResponseEntity<>(
            new ErrorResponse(LocalDateTime.now(), 401, "No Autorizado", ex.getMessage()), 
            HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(RecursoDuplicadoException ex) {
        return new ResponseEntity<>(
            new ErrorResponse(LocalDateTime.now(), 409, "Conflicto", ex.getMessage()), 
            HttpStatus.CONFLICT
        );
    }
}