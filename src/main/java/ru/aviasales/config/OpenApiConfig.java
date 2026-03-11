package ru.aviasales.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Advertising System API")
                        .description("Advertising Management System")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("ApiKey"))
                .components(new Components()
                        .addSecuritySchemes("ApiKey",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }

    @Bean
    public OpenApiCustomizer hideAuthorizationHeaderParameter() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getParameters() == null) {
                            return;
                        }

                        operation.setParameters(operation.getParameters().stream()
                                .filter(parameter -> !isAuthorizationHeader(parameter))
                                .toList());

                        if (operation.getParameters().isEmpty()) {
                            operation.setParameters(null);
                        }
                    }));
        };
    }

    private boolean isAuthorizationHeader(Parameter parameter) {
        return parameter != null
                && "header".equalsIgnoreCase(parameter.getIn())
                && "Authorization".equalsIgnoreCase(parameter.getName());
    }
}
