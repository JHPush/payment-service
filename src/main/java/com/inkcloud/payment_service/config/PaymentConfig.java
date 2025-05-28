package com.inkcloud.payment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class PaymentConfig {
    @Value("${SPRING_PORTONE_SECRET}")
    private String portOneSecret;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // 적용할 API 경로
                        .allowedOrigins("http://localhost:3000") // 리액트 주소
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // ExchangeStrategies strategies = ExchangeStrategies.builder()
        //     .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        //     .build();
        return builder.baseUrl("https://api.portone.io/payments")
        .defaultHeaders(header -> {
            header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            header.add("Authorization", "PortOne " + portOneSecret);
        })
        // .exchangeStrategies(strategies)
        .filter((request, next) -> {
            // 요청 전처리
            log.info("Payment-Service Request: " + request.method() + " " + request.url());
            return next.exchange(request);
        })
        .filter(ExchangeFilterFunction.ofResponseProcessor(response -> {
            // 응답 로깅
            log.info("Payment-Service Response status code: " + response.statusCode());

            if (response.statusCode().isError()) {
                log.error("Error response received: " + response.statusCode());
            }

            return Mono.just(response);
        }))
        .build();
    }

}
