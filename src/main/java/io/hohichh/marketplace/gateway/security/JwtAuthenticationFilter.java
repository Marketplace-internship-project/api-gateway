package io.hohichh.marketplace.gateway.security;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {
    private final JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                                WebFilterChain chain){

        String token = extractToken(exchange.getRequest());

        if (token != null && jwtValidator.validate(token)) {
            try{
                Claims claims = jwtValidator.getClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                Authentication auth = getAuthentication(role, userId);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            } catch (Exception e){
                log.error("Cannot set user authentication: {}", e.getMessage());
            }
        }
        return chain.filter(exchange);
    }

    private Authentication getAuthentication(String role, String userId) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if(role != null && !role.isEmpty()) {
            String securityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
            authorities.add(new SimpleGrantedAuthority(securityRole));
        }

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities);
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
