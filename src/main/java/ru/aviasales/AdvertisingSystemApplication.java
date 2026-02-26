package ru.aviasales;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.aviasales.gateway.interceptor.AuthInterceptor;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class AdvertisingSystemApplication implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public static void main(String[] args) {
        SpringApplication.run(AdvertisingSystemApplication.class, args);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/client/**", "/api/moderator/**", "/api/payment/**");
    }
}