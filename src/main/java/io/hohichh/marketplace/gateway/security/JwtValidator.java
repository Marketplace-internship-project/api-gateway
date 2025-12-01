package io.hohichh.marketplace.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Component
@Slf4j
public class JwtValidator {
    private final SecretKey secretKey;
    private final Clock clock;

    public JwtValidator(@Value("${jwt.access.secret}") String accessSecretStr, Clock clock){
        this.secretKey = Keys.hmacShaKeyFor(accessSecretStr.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public boolean validate(String token) {
        try{
            Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid jwt: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
