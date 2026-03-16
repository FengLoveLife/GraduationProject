package com.saul.common.utils;

import com.saul.user.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

/**
 * JWT 工具类。
 * <p>
 * 说明（重要）：
 * - 这里使用 HS256 对称加密签名；secret 必须足够长（至少 32 字节）。
 * - 目前只做后端登录模块演示；更完善的企业做法是引入刷新 Token、黑名单/注销、权限体系等。
 */
@Component
public class JwtUtils {

    /**
     * JWT 密钥（建议使用环境变量/配置中心注入，避免硬编码）
     */
    @Value("${jwt.secret:SupermarketSalesForecast-ChangeMe-To-A-Long-Secret-Key-2026}")
    private String secret;

    /**
     * 过期时间（毫秒）
     */
    @Value("${jwt.expire-ms:86400000}")
    private long expireMs;

    /**
     * 生成 Token：把用户关键信息写入 claims（不包含密码）。
     */
    public String generateToken(SysUser user) {
        Objects.requireNonNull(user, "user 不能为空");
        if (user.getId() == null || user.getUsername() == null) {
            throw new IllegalArgumentException("生成 Token 需要 user.id 与 user.username");
        }

        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);
        SecretKey key = buildKey(secret);

        return Jwts.builder()
                // 标准字段
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(exp)
                // 自定义字段（建议只放不敏感信息）
                .claim("userId", user.getId())
                .claim("realName", user.getRealName())
                .claim("status", user.getStatus())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token 并返回 Claims（会校验签名与过期时间）。
     *
     * @throws JwtException 当 Token 无效/过期/签名不匹配时抛出
     */
    public Claims parseClaims(String token) throws JwtException {
        SecretKey key = buildKey(secret);
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /**
     * 校验 Token 是否有效（签名正确 + 未过期）。
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 从请求头中提取 Token。
     * <p>
     * 支持两种常见方式：
     * - Authorization: Bearer <token>
     * - token: <token>
     */
    public String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String t = auth.substring(7).trim();
            return t.isEmpty() ? null : t;
        }

        String token = request.getHeader("token");
        if (token != null) {
            token = token.trim();
        }
        return (token == null || token.isEmpty()) ? null : token;
    }

    /**
     * 构造 HS256 密钥。
     * <p>
     * - secret 如果长度不足，直接抛错会影响开发体验；
     * - 这里做一个折中：若 secret 不足 32 字节，则对其做 SHA-256 生成固定 32 字节 key。
     *   这样能保证签名算法要求，并避免因为配置过短导致启动后所有请求都失败。
     */
    private SecretKey buildKey(String rawSecret) {
        if (rawSecret == null) {
            throw new IllegalArgumentException("jwt.secret 不能为空");
        }

        byte[] bytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = sha256(bytes);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            // 理论上不会发生（JDK 必带 SHA-256），这里兜底处理
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}