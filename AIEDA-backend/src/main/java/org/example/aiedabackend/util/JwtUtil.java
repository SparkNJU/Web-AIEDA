package org.example.aiedabackend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration; // 默认24小时

    private SecretKey getSigningKey() {
        // 注意：HS512 算法要求密钥长度至少 512 位（64 字节），否则会报错
        // 若你的密钥较短，需先更新为长密钥（如随机生成 64 位字符串）
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }


    // 从token中获取用户ID
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return Integer.valueOf(claims.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }


    // 生成 token（更新签名方法）
    public String generateToken(Integer userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return doGenerateToken(claims);
    }

    private String doGenerateToken(Map<String, Object> claims) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 从 token 中获取 Claims（更新解析方法）
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder() // 注意：新版本用 parserBuilder() 替代 parser()
                .setSigningKey(getSigningKey()) // 设置签名密钥
                .build() // 构建解析器
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 验证token是否有效
    public boolean validateToken(String token) {
        try {
            // 验证token是否过期
            final Date expiration = getExpirationDateFromToken(token);
            return !expiration.before(new Date());
        } catch (Exception e) {
            // 解析失败或其他异常表示token无效
            return false;
        }
    }

    // 从token中获取过期时间
    private Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 从token中获取指定类型的声明
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
}