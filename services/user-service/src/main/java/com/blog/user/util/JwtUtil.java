package com.blog.user.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String createToken(Long userId, String username) {
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("username", username)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
                .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT verify(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public Long getUserId(String token) {
        DecodedJWT jwt = verify(token);
        return jwt != null ? Long.parseLong(jwt.getSubject()) : null;
    }
}
