package org.example.payment_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String userPublicIdHeader = request.getHeader("X-User-UUID");
        String userRoleHeader = request.getHeader("X-User-Role");

        if(userPublicIdHeader != null && userRoleHeader != null){
            try{
                UUID publicId = UUID.fromString(userPublicIdHeader);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + userRoleHeader)
                );

                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        publicId, null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(token);
            }
            catch(Exception e){
                log.warn("Failed to parse headers: {}", e.getMessage());
            }
        }
        else{
            log.debug("No authentication headers found");
        }

        filterChain.doFilter(request, response);

    }
}
