package io.github.yuyeol3.yachtbackend.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET;
    private final Long EXPIRATION;
    private final Long REFRESH_EXPIRATION;
    private final SecureRandom secureRandom;


    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") Long expiration,
            @Value("${jwt.refresh_expiration}") Long refreshExpiration
    ) {
        this.SECRET = secret;
        this.EXPIRATION = expiration * 1000;
        this.REFRESH_EXPIRATION = refreshExpiration ;
        this.secureRandom = new SecureRandom();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public Long getRefreshExpiration() {
        return REFRESH_EXPIRATION;

    }

    public String generateAccessToken(Long userId, String nickname) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("nickname", nickname)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    public byte[] generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    public byte[] hashToken(byte[] tokenBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(tokenBytes); // 32바이트
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    public Long getUserIdFromToken(String token) {
        return Long.valueOf(
                Jwts.parser()
                        .verifyWith(getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
