package com.ruoyi.framework.jwt;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtils
{
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private static final String CLAIM_MEMBER_ID = "memberId";

    @Value("${mall.jwt.secret}")
    private String secret;

    @Value("${mall.jwt.expiration}")
    private long expiration;

    public String generateToken(Long memberId)
    {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .claim(CLAIM_MEMBER_ID, memberId)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    public boolean validateToken(String token)
    {
        try
        {
            getClaims(token);
            return true;
        }
        catch (ExpiredJwtException e)
        {
            log.debug("JWT expired: {}", e.getMessage());
        }
        catch (Exception e)
        {
            log.debug("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    public Long getMemberIdFromToken(String token)
    {
        Claims claims = getClaims(token);
        return claims.get(CLAIM_MEMBER_ID, Long.class);
    }

    private Claims getClaims(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
}
