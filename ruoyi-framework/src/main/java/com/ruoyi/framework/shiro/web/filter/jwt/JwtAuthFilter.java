package com.ruoyi.framework.shiro.web.filter.jwt;

import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.web.filter.AccessControlFilter;
import com.ruoyi.framework.jwt.JwtUtils;

/**
 * JWT 鉴权过滤器，挂载在 /api/** 路径。
 * 从 Authorization: Bearer <token> 读取 JWT，验证后将 memberId 写入请求属性。
 * 验证失败直接返回 401 JSON，不跳转登录页。
 */
public class JwtAuthFilter extends AccessControlFilter
{
    public static final String ATTR_MEMBER_ID = "jwtMemberId";

    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    public JwtAuthFilter(JwtUtils jwtUtils)
    {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
    {
        String token = resolveToken((HttpServletRequest) request);
        if (token == null || !jwtUtils.validateToken(token))
        {
            return false;
        }
        Long memberId = jwtUtils.getMemberIdFromToken(token);
        request.setAttribute(ATTR_MEMBER_ID, memberId);
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException
    {
        HttpServletResponse httpResp = (HttpServletResponse) response;
        httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResp.setContentType("application/json;charset=UTF-8");
        httpResp.getWriter().write("{\"code\":401,\"msg\":\"Unauthorized: missing or invalid token\"}");
        return false;
    }

    private String resolveToken(HttpServletRequest request)
    {
        String header = request.getHeader(HEADER_AUTH);
        if (header != null && header.startsWith(TOKEN_PREFIX))
        {
            return header.substring(TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
