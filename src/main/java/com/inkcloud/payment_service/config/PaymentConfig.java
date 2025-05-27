package com.inkcloud.payment_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PaymentConfig {
    @Value("${SPRING_PORTONE_SECRET}")
    private String portOneSecret;
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")  // 적용할 API 경로
                        .allowedOrigins("http://localhost:3000")  // 리액트 주소
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder){
        return builder.baseUrl("https://api.portone.io/payments").defaultHeaders(header->{
            header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            header.add("Authorization", "PortOne " + portOneSecret);
        }).build();
    }

}
