package com.mango.products.infrastructure.service;

import com.fasterxml.uuid.Generators;
import com.mango.products.domain.Id;
import com.mango.products.domain.IdGenerator;
import org.springframework.stereotype.Component;

@Component
public class UuidV7IdGenerator implements IdGenerator {

  @Override
  public Id nextIdentity() {
    return new Id(Generators.timeBasedEpochGenerator().generate());
  }
}
