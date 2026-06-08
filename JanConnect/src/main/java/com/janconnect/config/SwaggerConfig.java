package com.janconnect.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .addSecurityItem(new SecurityRequirement()
                        .addList("BearerAuth")
                        .addList("ApiKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", bearerScheme())
                        .addSecuritySchemes("ApiKeyAuth", apiKeyScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("JanConnect API")
                .description("JanConnect Platform — Connecting citizens to government services")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("JanConnect Support")
                        .email("support@janconnect.com")
                        .url("https://www.janconnect.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Server> servers() {
        return switch (activeProfile) {
            case "prod" -> List.of(
                    new Server().url("https://api.janconnect.com").description("Production")
            );
            case "uat" -> List.of(
                    new Server().url("https://uat-api.janconnect.com").description("UAT"),
                    new Server().url("http://localhost:9090").description("Local")
            );
            default -> List.of(
                    new Server().url("http://localhost:9090").description("Local Dev")
            );
        };
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT access token obtained from POST /api/v1/auth/login");
    }

    private SecurityScheme apiKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY")
                .description("Provide a valid API key in the X-API-KEY request header");
    }
}
