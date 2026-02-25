package io.hohichh.marketplace.gateway.config;

import io.hohichh.marketplace.gateway.dto.in.UserDataWithCredentialsDto;
import io.hohichh.marketplace.gateway.dto.out.UserDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server; 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @SuppressWarnings("rawtypes")
    @Bean
    public OpenAPI customOpenAPI() {
        Map<String, Schema> schemas = ModelConverters.getInstance()
                .read(UserDataWithCredentialsDto.class);
        schemas.putAll(ModelConverters.getInstance().read(UserDto.class));

        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Gateway Server")
                ))
                .info(new Info()
                        .title("Marketplace Gateway API")
                        .version("1.0")
                        .description("Documentation for Gateway-specific endpoints (Orchestration)"))
                .components(new Components().schemas(schemas));
    }
}