package org.example.aiedabackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI EDA API")
                        .description("AI EDA Web平台API文档")
                        .version("v1")
                        .contact(new Contact()
                                .name("SparkNJU")
                                .email("231098201@smail.nju.edu.cn")
                                .url("https://github.com/SparkNJU")));
    }
}