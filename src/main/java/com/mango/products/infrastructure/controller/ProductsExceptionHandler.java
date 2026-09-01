package com.mango.products.infrastructure.controller;

import com.mango.products.domain.DuplicateProductNameException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductsExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Invalid Request");
    return problemDetail;
  }

  @ExceptionHandler(DuplicateProductNameException.class)
  public ProblemDetail handleDuplicateProductNameException(DuplicateProductNameException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Duplicate Product Name");
    problemDetail.setProperty("conflictingProductId", ex.conflictingProductId().value().toString());
    return problemDetail;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    String message = ex.getMessage();
    if (message != null
        && (message.contains("ex_product_currency_validity") || message.contains("23P01"))) {
      ProblemDetail problemDetail =
          ProblemDetail.forStatusAndDetail(
              HttpStatus.CONFLICT,
              "Price validity period overlaps with an existing price for this product and currency");
      problemDetail.setTitle("Price Overlap Conflict");
      return problemDetail;
    }
    if (message != null && message.contains("uk_product_name_lower")) {
      ProblemDetail problemDetail =
          ProblemDetail.forStatusAndDetail(
              HttpStatus.CONFLICT, "A product with this name already exists");
      problemDetail.setTitle("Duplicate Product Name");
      return problemDetail;
    }
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data integrity violation occurred");
    problemDetail.setTitle("Conflict");
    return problemDetail;
  }
}
