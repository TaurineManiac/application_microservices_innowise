package org.example.order_service.config;

import org.example.order_service.filter.HeaderAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<HeaderAuthenticationFilter> headerAuthenticationFilterRegistration(HeaderAuthenticationFilter filter) {
        FilterRegistrationBean<HeaderAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}