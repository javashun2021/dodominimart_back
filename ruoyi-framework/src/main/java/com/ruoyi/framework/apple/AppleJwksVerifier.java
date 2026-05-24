package com.ruoyi.framework.apple;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * Apple Sign-In identityToken 验签器
 *
 * 流程：
 *   1. 从 JWT header 取 kid
 *   2. 从 Apple JWKS 端点拉取公钥列表（本地缓存 1 小时）
 *   3. 用 RSA 公钥验证 JWT 签名（jjwt 自动验 exp）
 *   4. 校验 iss / aud 声明
 */
@Component
public class AppleJwksVerifier
{
    private static final Logger log = LoggerFactory.getLogger(AppleJwksVerifier.class);

    private static final String JWKS_URL     = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final long   CACHE_TTL_MS = 3_600_000L; // 1 小时

    @Value("${mall.apple.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper  = new ObjectMapper();

    // kid -> RSA PublicKey
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile long cacheLoadedAt = 0;

    /**
     * 验证 Apple identityToken，返回 JWT Claims（含 sub / email）
     *
     * @throws Exception token 非法、签名不符、过期、iss/aud 不匹配等
     */
    public Claims verify(String identityToken) throws Exception
    {
        String kid = extractKid(identityToken);
        PublicKey publicKey = resolvePublicKey(kid);

        // jjwt 自动验签 + 验 exp
        Claims claims = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(identityToken)
                .getBody();

        if (!APPLE_ISSUER.equals(claims.getIssuer()))
        {
            throw new Exception("Apple token issuer mismatch: " + claims.getIssuer());
        }
        if (!bundleId.equals(claims.getAudience()))
        {
            throw new Exception("Apple token audience mismatch (expected " + bundleId
                    + ", got " + claims.getAudience() + ")");
        }
        return claims;
    }

    // ── private ─────────────────────────────────────────────

    private String extractKid(String token) throws Exception
    {
        String[] parts = token.split("\\.");
        if (parts.length < 3)
        {
            throw new Exception("Invalid JWT format");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(padBase64(parts[0])));
        @SuppressWarnings("unchecked")
        Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
        String kid = (String) header.get("kid");
        if (kid == null || kid.isEmpty())
        {
            throw new Exception("Missing kid in JWT header");
        }
        return kid;
    }

    private PublicKey resolvePublicKey(String kid) throws Exception
    {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS || !keyCache.containsKey(kid))
        {
            refreshCache();
        }
        PublicKey key = keyCache.get(kid);
        if (key == null)
        {
            // kid 不在缓存里，强制刷新一次（苹果偶尔轮换密钥）
            refreshCache();
            key = keyCache.get(kid);
        }
        if (key == null)
        {
            throw new Exception("Unknown Apple key id: " + kid);
        }
        return key;
    }

    private synchronized void refreshCache() throws Exception
    {
        // 避免并发时重复刷新
        if (System.currentTimeMillis() - cacheLoadedAt < 30_000L && !keyCache.isEmpty())
        {
            return;
        }
        log.info("Refreshing Apple JWKS from {}", JWKS_URL);
        String json = restTemplate.getForObject(JWKS_URL, String.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> jwks = objectMapper.readValue(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

        Map<String, PublicKey> newCache = new ConcurrentHashMap<>();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        for (Map<String, Object> jwk : keys)
        {
            String kid = (String) jwk.get("kid");
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("n")));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("e")));
            PublicKey pub = keyFactory.generatePublic(new RSAPublicKeySpec(n, e));
            newCache.put(kid, pub);
        }

        keyCache.clear();
        keyCache.putAll(newCache);
        cacheLoadedAt = System.currentTimeMillis();
        log.info("Apple JWKS cached {} keys", newCache.size());
    }

    private static String padBase64(String base64url)
    {
        int mod = base64url.length() % 4;
        return mod == 0 ? base64url : base64url + "====".substring(mod);
    }
}
