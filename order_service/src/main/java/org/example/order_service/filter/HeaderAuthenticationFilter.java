package org.example.order_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String publicIdHeader = request.getHeader("X-User-UUID");
        String roleHeader =  request.getHeader("X-User-Role");

        if(publicIdHeader!=null && roleHeader!=null){
            try{
                UUID publicId = UUID.fromString(publicIdHeader);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + roleHeader)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(publicId, null, authorities);


                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user from Gateway headers: {}", publicId);
            }
            catch(Exception e){
                log.warn("Failed to parse Gateway headers: {}", e.getMessage());
            }
        }
        else{
            log.debug("No Gateway authentication headers found");
        }

        filterChain.doFilter(request, response);
    }
}
