package com.mango.products.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerWebConfiguration implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/swagger-ui").setViewName("forward:/swagger-ui.html");
    registry.addViewController("/swagger-ui/").setViewName("forward:/swagger-ui.html");
  }
}
