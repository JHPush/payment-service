package com.inkcloud.payment_service.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class SyncPaymentException{
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, Exception ex){
      log.error("----------------------- Sync Payment Exception ----------------------");
      log.error("uri : {}, method : {}", request.getRequestURI(), request.getMethod());
      log.error("error : {} ", ex.getMessage());
      
      ErrorResponse response = ErrorResponse.builder().code("500").message(ex.getMessage()).build();
      return ResponseEntity.ok().body(response);
    }
}
