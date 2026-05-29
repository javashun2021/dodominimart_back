package com.ruoyi.web.controller.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.framework.apple.AppleJwksVerifier;
import com.ruoyi.framework.jwt.JwtUtils;
import com.ruoyi.framework.mail.MailService;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.service.IMallMemberService;
import io.jsonwebtoken.Claims;

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

    private static final String GOOGLE_USERINFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${mall.google.clientIds}")
    private String googleClientIds;

    @Autowired
    private IMallMemberService memberService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AppleJwksVerifier appleJwksVerifier;

    @Autowired
    private MailService mailService;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }

    /**
     * Google 登录 / 自动注册
     * Body: { "idToken": "..." }
     */
    @PostMapping("/google")
    public AjaxResult googleLogin(@RequestBody Map<String, String> body)
    {
        String idToken     = body.get("idToken");
        String accessToken = body.get("accessToken");

        Map<String, Object> claims;

        if (idToken != null && !idToken.isEmpty())
        {
            // 移动端（Android/iOS）：用 idToken 验证，并校验 aud
            claims = verifyGoogleIdToken(idToken);
            if (claims == null)
            {
                return AjaxResult.error("Invalid Google idToken");
            }
            String aud = String.valueOf(claims.get("aud"));
            List<String> allowedIds = Arrays.asList(googleClientIds.split(","));
            if (!allowedIds.contains(aud.trim()))
            {
                return AjaxResult.error("Token audience mismatch");
            }
        }
        else if (accessToken != null && !accessToken.isEmpty())
        {
            // Web 端：用 accessToken 换取用户信息（Google 负责验证，无需校验 aud）
            claims = verifyGoogleAccessToken(accessToken);
            if (claims == null)
            {
                return AjaxResult.error("Invalid Google accessToken");
            }
        }
        else
        {
            return AjaxResult.error("idToken or accessToken is required");
        }

        String googleId     = String.valueOf(claims.get("sub"));
        String email        = String.valueOf(claims.getOrDefault("email", ""));
        String name         = String.valueOf(claims.getOrDefault("name", ""));
        String picture      = String.valueOf(claims.getOrDefault("picture", ""));
        String referralCode = body.get("referralCode");

        MallMember member = memberService.loginOrRegisterByGoogle(googleId, email, name, picture, referralCode);
        if (!"0".equals(member.getStatus()))
        {
            return AjaxResult.error("Account is disabled");
        }

        return AjaxResult.success("Login successful").put("data", buildLoginResult(member));
    }

    /**
     * Sign in with Apple（iOS App Store 上架强制要求）
     * Body: { "identityToken": "...", "fullName": "..." }
     */
    @PostMapping("/apple")
    public AjaxResult appleLogin(@RequestBody Map<String, String> body)
    {
        String identityToken = body.get("identityToken");
        if (identityToken == null || identityToken.isEmpty())
        {
            return AjaxResult.error("identityToken is required");
        }

        Claims claims;
        try
        {
            claims = appleJwksVerifier.verify(identityToken);
        }
        catch (Exception e)
        {
            return AjaxResult.error("Apple token verification failed");
        }

        String appleId      = claims.getSubject();
        String email        = claims.get("email", String.class);
        if (email == null) email = "";
        String fullName     = body.getOrDefault("fullName", "Apple User");
        String referralCode = body.get("referralCode");

        MallMember member = memberService.loginOrRegisterByApple(appleId, email, fullName, referralCode);
        if (!"0".equals(member.getStatus()))
        {
            return AjaxResult.error("Account is disabled");
        }
        return AjaxResult.success("Login successful").put("data", buildLoginResult(member));
    }

    /**
     * 登出（无状态 JWT，服务端直接返回成功，客户端负责清除本地 token）
     *
     */
    @PostMapping("/logout")
    public AjaxResult logout()
    {
        return AjaxResult.success("退出成功");
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
    private Map<String, Object> verifyGoogleIdToken(String idToken)
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleAccessToken(String accessToken)
    {
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    GOOGLE_USERINFO_URL, HttpMethod.GET, entity, Map.class);
            if (resp.getStatusCode().is2xxSuccessful())
            {
                return resp.getBody();
            }
        }
        catch (Exception e)
        {
            // accessToken 无效时 Google 返回 401
        }
        return null;
    }

    /**
     * 发送邮箱验证码
     * Body: { "email": "..." }
     */
    @PostMapping("/send-code")
    public AjaxResult sendCode(@RequestBody Map<String, String> body)
    {
        String email = body.get("email");
        try
        {
            String code = memberService.createVerifyCode(email);
            mailService.sendVerifyCode(email.trim(), code);
            return AjaxResult.success("Verification code sent to your email");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 邮箱注册
     * Body: { "email": "...", "code": "...", "password": "...", "nickName": "..." }
     */
    @PostMapping("/register")
    public AjaxResult register(@RequestBody Map<String, String> body)
    {
        String email        = body.get("email");
        String code         = body.get("code");
        String password     = body.get("password");
        String nickName     = body.get("nickName");
        String referralCode = body.get("referralCode");
        try
        {
            MallMember member = memberService.registerByEmail(email, code, password, nickName, referralCode);
            return AjaxResult.success("Registration successful").put("data", buildLoginResult(member));
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 邮箱登录
     * Body: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body)
    {
        String email    = body.get("email");
        String password = body.get("password");
        try
        {
            MallMember member = memberService.loginByEmail(email, password);
            return AjaxResult.success("Login successful").put("data", buildLoginResult(member));
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
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
