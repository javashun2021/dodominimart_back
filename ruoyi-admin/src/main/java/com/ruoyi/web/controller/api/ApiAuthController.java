package com.ruoyi.web.controller.api;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.framework.jwt.JwtUtils;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.service.IMallMemberService;

/**
 * App 会员鉴权接口（无需登录）
 * POST /api/v1/auth/google  — Google 登录
 * POST /api/v1/auth/apple   — Sign in with Apple
 * POST /api/v1/auth/refresh — 刷新 JWT
 */
@RestController
@RequestMapping("/api/v1/auth")
public class ApiAuthController
{
    private static final String GOOGLE_TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    @Value("${mall.google.clientId}")
    private String googleClientId;

    @Autowired
    private IMallMemberService memberService;

    @Autowired
    private JwtUtils jwtUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Google 登录 / 自动注册
     * Body: { "idToken": "..." }
     */
    @PostMapping("/google")
    public AjaxResult googleLogin(@RequestBody Map<String, String> body)
    {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isEmpty())
        {
            return AjaxResult.error("idToken is required");
        }

        Map<String, Object> claims = verifyGoogleToken(idToken);
        if (claims == null)
        {
            return AjaxResult.error("Invalid Google token");
        }

        // 验证 aud 是否匹配本应用 client ID
        String aud = String.valueOf(claims.get("aud"));
        if (!googleClientId.equals(aud))
        {
            return AjaxResult.error("Token audience mismatch");
        }

        String googleId = String.valueOf(claims.get("sub"));
        String email    = String.valueOf(claims.getOrDefault("email", ""));
        String name     = String.valueOf(claims.getOrDefault("name", ""));
        String picture  = String.valueOf(claims.getOrDefault("picture", ""));

        MallMember member = memberService.loginOrRegisterByGoogle(googleId, email, name, picture);
        if (!"0".equals(member.getStatus()))
        {
            return AjaxResult.error("Account is disabled");
        }

        return AjaxResult.success("Login successful").put("data", buildLoginResult(member));
    }

    /**
     * Sign in with Apple（iOS App Store 上架强制要求）
     * Body: { "identityToken": "...", "user": "...", "fullName": "..." }
     */
    @PostMapping("/apple")
    public AjaxResult appleLogin(@RequestBody Map<String, String> body)
    {
        // Apple identityToken 是 JWT，sub 字段是 apple_id
        // 完整验证需用 Apple 公钥验证签名，此处先做基础解析（production 请接入完整 JWKS 验证）
        String identityToken = body.get("identityToken");
        if (identityToken == null || identityToken.isEmpty())
        {
            return AjaxResult.error("identityToken is required");
        }

        String[] parts = identityToken.split("\\.");
        if (parts.length < 2)
        {
            return AjaxResult.error("Invalid Apple token format");
        }

        try
        {
            // Base64 解码 payload（无需验证签名即可读取 sub/email）
            String payload = new String(java.util.Base64.getUrlDecoder().decode(
                    parts[1].length() % 4 == 0 ? parts[1] : parts[1] + "====".substring(parts[1].length() % 4)));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            String appleId = String.valueOf(claims.get("sub"));
            String email   = String.valueOf(claims.getOrDefault("email", ""));
            String fullName = body.getOrDefault("fullName", "Apple User");

            MallMember member = memberService.loginOrRegisterByApple(appleId, email, fullName);
            if (!"0".equals(member.getStatus()))
            {
                return AjaxResult.error("Account is disabled");
            }
            return AjaxResult.success("Login successful").put("data", buildLoginResult(member));
        }
        catch (Exception e)
        {
            return AjaxResult.error("Failed to parse Apple token");
        }
    }

    /**
     * 刷新 JWT
     * Body: { "token": "..." }
     */
    @PostMapping("/refresh")
    public AjaxResult refresh(@RequestBody Map<String, String> body)
    {
        String oldToken = body.get("token");
        if (oldToken == null || !jwtUtils.validateToken(oldToken))
        {
            return AjaxResult.error("Invalid or expired token");
        }
        Long memberId = jwtUtils.getMemberIdFromToken(oldToken);
        String newToken = jwtUtils.generateToken(memberId);
        return AjaxResult.success("ok").put("data", newToken);
    }

    // ─────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken)
    {
        try
        {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    GOOGLE_TOKENINFO_URL, Map.class, idToken);
            if (resp.getStatusCode().is2xxSuccessful())
            {
                return resp.getBody();
            }
        }
        catch (Exception e)
        {
            // token 无效时 Google 返回 4xx
        }
        return null;
    }

    private Map<String, Object> buildLoginResult(MallMember member)
    {
        String token = jwtUtils.generateToken(member.getMemberId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("memberId", member.getMemberId());
        memberInfo.put("nickName", member.getNickName());
        memberInfo.put("email", member.getEmail());
        memberInfo.put("avatarUrl", member.getAvatarUrl());
        result.put("member", memberInfo);
        return result;
    }
}
