package org.niit_project.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtTokenUtil {
    private String secretKey;

    public JwtTokenUtil(){}

    public JwtTokenUtil(final String secretKey){
        this.secretKey = secretKey;
    }

    public void setSecretKey(final String secretKey){
        this.secretKey = secretKey;
    }

    public String extractUserId(String token) {
        // Parse the token and validate the signature
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes()) // Use the same secret used to sign the token
                .parseClaimsJws(token) // Parse and validate the token
                .getBody(); // Extract the payload (claims)

        // Extract the `user_id` claim
        return claims.get("user_id", String.class);
    }

    public String generateToken(final String userId){
        long expirationTime = 1000 * 60 * 60 * 10; // 10 hour expiration time
        return Jwts.builder()
                .setHeaderParam("alg", "HS256")
                .setHeaderParam("typ", "JWT")
                .setSubject(userId)
                .claim("role", "user")
                .claim("userId", userId)
                .claim("user_id", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();

    }
}
