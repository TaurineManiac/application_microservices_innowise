package org.example.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class InternalApiFIlter extends OncePerRequestFilter {

    @Value("${internal.service.token}")
    private String internalToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if(
                path.equals("/api/v1/users") && request.getMethod().equals("POST") ||
                        path.startsWith("/api/v1/users/internal/") && request.getMethod().equals("DELETE")
        ){
            String providedToken = request.getHeader("X-Internal-Token");
            if(!internalToken.equals(providedToken)){
                log.warn("Invalid or missing Internal Token for {}", path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid internal token\"}");
                return;
            }
            log.debug("Internal token validated for {}", path);
        }
        filterChain.doFilter(request, response);
    }
}
