package com.dlight.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI minipayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MiniPay - Payment & Notification Microservice")
                        .description("Mock payment processing with async SMS notifications")
                        .version("v1"));
    }
}
