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

//    @Bean
//    public RequestInterceptor requestInterceptor() {
//        return requestTemplate -> {
//            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            if (attributes == null) {
//                log.debug("No request attributes, skipping header propagation");
//                return;
//            }
//            HttpServletRequest request = attributes.getRequest();
//            String userUuid = request.getHeader("X-User-UUID");
//            String userRole = request.getHeader("X-User-Role");
//
//            if (userUuid != null) {
//                requestTemplate.header("X-User-UUID", userUuid);
//            }
//            if (userRole != null) {
//                requestTemplate.header("X-User-Role", userRole);
//            }
//            log.debug("Propagating headers: X-User-UUID={}, X-User-Role={}", userUuid, userRole);
//        };
//    }
}