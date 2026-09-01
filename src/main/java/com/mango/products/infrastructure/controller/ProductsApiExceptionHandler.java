package com.mango.products.infrastructure.controller;

import com.mango.products.domain.DomainException;
import com.mango.products.domain.DomainException.DuplicateProductNameException;
import com.mango.products.domain.DomainException.PriceValidityOverlapException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductsApiExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ProblemDetail handleDomainException(DomainException ex) {
    return switch (ex) {
      case DuplicateProductNameException dne -> {
        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, dne.getMessage());
        problemDetail.setTitle("Duplicate Product Name");
        if (dne.conflictingProductId() != null) {
          problemDetail.setProperty(
              "conflictingProductId", dne.conflictingProductId().value().toString());
        }
        yield problemDetail;
      }
      case PriceValidityOverlapException poe -> {
        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, poe.getMessage());
        problemDetail.setTitle("Price Overlap Conflict");
        yield problemDetail;
      }
      default -> {
        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Request");
        yield problemDetail;
      }
    };
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Invalid Request");
    return problemDetail;
  }
}
