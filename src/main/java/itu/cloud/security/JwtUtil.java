package itu.cloud.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    public JwtUtil() {
    }

    private Key keyFromSecret(String secret) {
        // Use SecretKeySpec to avoid dependency on io.jsonwebtoken.security.Keys
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    public String generateToken(String secret, Map<String, Object> claims, int expirationMinutes) {
        Key key = keyFromSecret(secret);
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationMinutes * 60L)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String secret, String token) {
        Key key = keyFromSecret(secret);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public boolean isTokenExpired(String secret, String token) {
        try {
            Jws<Claims> jws = parseToken(secret, token);
            return jws.getBody().getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
