package io.hohichh.marketplace.gateway.config;

import io.hohichh.marketplace.gateway.dto.in.UserDataWithCredentialsDto;
import io.hohichh.marketplace.gateway.dto.out.UserDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Map<String, Schema> schemas = ModelConverters.getInstance()
                .read(UserDataWithCredentialsDto.class);
        schemas.putAll(ModelConverters.getInstance().read(UserDto.class));

        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace Gateway API")
                        .version("1.0")
                        .description("Documentation for Gateway-specific endpoints (Orchestration)"))
                .components(new Components().schemas(schemas))
                .path("/api/v1/auth/credentials", new PathItem().post(
                        new Operation()
                                .summary("Register new user (Orchestration)")
                                .description("Creates user in User Service and credentials in Auth Service transactionally.")
                                .tags(java.util.List.of("Gateway Handlers"))
                                .requestBody(new RequestBody()
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(new Schema<UserDataWithCredentialsDto>().$ref("#/components/schemas/UserDataWithCredentialsDto")))))
                                .responses(new ApiResponses()
                                        .addApiResponse("201", new ApiResponse().description("User created successfully")
                                                .content(new Content().addMediaType("application/json",
                                                        new MediaType().schema(new Schema<UserDto>().$ref("#/components/schemas/UserDto")))))
                                        .addApiResponse("400", new ApiResponse().description("Invalid input"))
                                        .addApiResponse("409", new ApiResponse().description("User already exists")))
                ))
                .path("/api/v1/users/{id}", new PathItem().delete(
                        new Operation()
                                .summary("Delete user (Orchestration)")
                                .description("Deletes user credentials and profile. Requires OWNER or ADMIN rights.")
                                .tags(java.util.List.of("Gateway Handlers"))
                                .addParametersItem(new PathParameter().name("id").description("User UUID").required(true).schema(new Schema<String>().type("string").format("uuid")))
                                .responses(new ApiResponses()
                                        .addApiResponse("204", new ApiResponse().description("User deleted"))
                                        .addApiResponse("202", new ApiResponse().description("Accepted (Partial deletion)"))
                                        .addApiResponse("403", new ApiResponse().description("Forbidden")))
                ));
    }
}
