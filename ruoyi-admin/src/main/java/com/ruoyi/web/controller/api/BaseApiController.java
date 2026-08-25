package com.ruoyi.web.controller.api;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.framework.jwt.JwtUtils;
import com.ruoyi.framework.shiro.web.filter.jwt.JwtAuthFilter;

/**
 * App REST API 基类，提供 JWT memberId 读取和分页工具
 */
public abstract class BaseApiController
{
    @Autowired
    private JwtUtils jwtUtils;

    protected Long getCurrentMemberId(HttpServletRequest request)
    {
        // 正常路径：JwtAuthFilter 已解析并写入 attribute
        Object memberId = request.getAttribute(JwtAuthFilter.ATTR_MEMBER_ID);
        if (memberId != null)
        {
            return (Long) memberId;
        }
        // 兜底：请求经过 Shiro anon 路由（如 POST 与 GET 共享同一 URL），直接从 header 解析
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
        {
            String token = header.substring(7).trim();
            if (jwtUtils.validateToken(token))
            {
                return jwtUtils.getMemberIdFromToken(token);
            }
        }
        throw new RuntimeException("未登录或登录已过期");
    }

    private static final int MAX_PAGE_SIZE = 200;

    protected void startPage(int pageNum, int pageSize)
    {
        if (pageNum < 1)  pageNum  = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;
        PageHelper.startPage(pageNum, pageSize);
    }

    protected <T> AjaxResult pageResult(PageInfo<T> page)
    {
        return AjaxResult.success("ok")
                .put("total", page.getTotal())
                .put("pageNum", page.getPageNum())
                .put("pageSize", page.getPageSize())
                .put("list", page.getList());
    }
}
