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
    public RequestInterceptor gatewayHeaderInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if(attributes != null) {
                    log.debug("No request attributes, skipping header propagation");
                    return;
                }

                HttpServletRequest request = attributes.getRequest();

                String publicIdHeader = request.getHeader("X-Public-Id");
                String roleHeader = request.getHeader("X-User-Role");

                if (publicIdHeader != null) {
                    requestTemplate.header("X-User-UUID", publicIdHeader);
                }
                if (roleHeader != null) {
                    requestTemplate.header("X-User-Role", roleHeader);
                }

            }
        };
    }
}