package org.example.order_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.client.AuthenticationValidationProvider;
import org.example.order_service.dto.ValidateResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationConfig extends OncePerRequestFilter {

    private final AuthenticationValidationProvider authServiceClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                ValidateResponse validation = authServiceClient.validateToken("Bearer " + token);

                if (validation.isValid()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    validation.getPublicId().toString(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + validation.getRole()))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {} with role: {}", validation.getPublicId(), validation.getRole());
                } else {
                    log.warn("Invalid token provided");
                }
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}