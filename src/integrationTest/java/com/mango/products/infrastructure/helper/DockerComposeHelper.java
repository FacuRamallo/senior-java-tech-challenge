package com.mango.products.infrastructure.helper;

import java.io.File;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.ComposeContainer;

public class DockerComposeHelper {

  private static final String POSTGRES = "db";
  private static final int POSTGRES_PORT = 5432;

  @SuppressWarnings("resource")
  public static ComposeContainer create() {
    return new ComposeContainer(new File("docker-compose.test.yml"))
        .withExposedService(POSTGRES, POSTGRES_PORT);
  }

  public static void setDynamicProperties(
      ComposeContainer container, DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> {
          String host = container.getServiceHost(POSTGRES, POSTGRES_PORT);
          Integer port = container.getServicePort(POSTGRES, POSTGRES_PORT);
          return "jdbc:postgresql://" + host + ":" + port + "/products";
        });
  }
}
