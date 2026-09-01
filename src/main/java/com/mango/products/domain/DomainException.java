package com.mango.products.domain;

public abstract sealed class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  public static final class BlankIdException extends DomainException {
    public BlankIdException() {
      super("Id must not be blank");
    }
  }

  public static final class InvalidUuidV7Exception extends DomainException {
    public InvalidUuidV7Exception() {
      super("Id must be a valid UUIDv7");
    }
  }

  public static final class BlankNameException extends DomainException {
    public BlankNameException() {
      super("Name cannot be blank");
    }
  }

  public static final class BlankDescriptionException extends DomainException {
    public BlankDescriptionException() {
      super("Description cannot be blank");
    }
  }

  public static final class AmountMustBePositiveException extends DomainException {
    public AmountMustBePositiveException() {
      super("Amount must be greater than zero");
    }
  }

  public static final class InvalidCurrencyCodeException extends DomainException {
    public InvalidCurrencyCodeException() {
      super("Currency must be a valid ISO-4217 code");
    }
  }

  public static final class NullInitDateException extends DomainException {
    public NullInitDateException() {
      super("Init date must not be null");
    }
  }

  public static final class InitDateNotBeforeEndDateException extends DomainException {
    public InitDateNotBeforeEndDateException() {
      super("Init date must be before end date");
    }
  }

  public static final class BlankDateException extends DomainException {
    public BlankDateException() {
      super("Date must not be blank");
    }
  }

  public static final class InvalidDateFormatException extends DomainException {
    public InvalidDateFormatException() {
      super("Date must be in ISO-8601 format (YYYY-MM-DD)");
    }
  }

  public static final class OpenEndedPriceConflictException extends DomainException {
    public OpenEndedPriceConflictException() {
      super("Cannot create a new price while the current price has an open-ended end date");
    }
  }

  public static final class NonSequentialPriceDateException extends DomainException {
    public NonSequentialPriceDateException() {
      super("New price init date must be after the last price end date");
    }
  }

  public static final class InactivePriceUpdateException extends DomainException {
    public InactivePriceUpdateException() {
      super("Only currently active prices can be updated");
    }
  }

  public static final class PriceValidityOverlapException extends DomainException {
    public PriceValidityOverlapException() {
      super("Price validity period overlaps with an existing price for this product and currency");
    }
  }

  public static final class DuplicateProductNameException extends DomainException {
    private final Id conflictingProductId;
    private final Name name;

    public DuplicateProductNameException(Id conflictingProductId, Name name) {
      super("A product with the name '" + name.value() + "' already exists");
      this.conflictingProductId = conflictingProductId;
      this.name = name;
    }

    public DuplicateProductNameException(Name name) {
      this(null, name);
    }

    public Id conflictingProductId() {
      return conflictingProductId;
    }

    public Name name() {
      return name;
    }
  }
}
