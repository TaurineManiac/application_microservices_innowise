package org.example.order_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return (RequestTemplate template) -> {
            if (template.headers().containsKey("Authorization")) {
                return;
            }

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null) {
                template.header("Authorization", authorizationHeader);
            } else {
                log.debug("No Authorization header found on incoming request to forward via Feign");
            }
        };
    }
}