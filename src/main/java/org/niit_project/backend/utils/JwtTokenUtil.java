package org.niit_project.backend.utils;

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

    public String generateToken(final String userId){
        long expirationTime = 1000 * 60 * 60; // 1 hour expiration time

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

    }
}
