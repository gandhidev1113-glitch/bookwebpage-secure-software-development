package com.library.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            SecretKey generatedKey = Jwts.SIG.HS256.key().build();
            secretKey = Encoders.BASE64.encode(generatedKey.getEncoded());
            log.warn("jwt.secret is not configured. Generated an ephemeral key for this runtime.");
            return;
        }

        validateConfiguredSecret();
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration)
                .before(new Date());
    }

    private <T> T extractClaim(String token,
                               Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void validateConfiguredSecret() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("jwt.secret must decode to at least 256 bits.");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("jwt.secret must be a valid Base64 string.", ex);
        }
    }
}
