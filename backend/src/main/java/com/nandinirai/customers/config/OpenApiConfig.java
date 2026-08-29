package com.nandinirai.customers.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Management API")
                .version("v1")
                .description("Create and retrieve customer records."));
    }
}
