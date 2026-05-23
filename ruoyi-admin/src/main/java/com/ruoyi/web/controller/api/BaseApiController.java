package com.ruoyi.web.controller.api;

import javax.servlet.http.HttpServletRequest;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.framework.shiro.web.filter.jwt.JwtAuthFilter;

/**
 * App REST API 基类，提供 JWT memberId 读取和分页工具
 */
public abstract class BaseApiController
{
    protected Long getCurrentMemberId(HttpServletRequest request)
    {
        Object memberId = request.getAttribute(JwtAuthFilter.ATTR_MEMBER_ID);
        if (memberId == null)
        {
            throw new RuntimeException("Not authenticated");
        }
        return (Long) memberId;
    }

    protected void startPage(int pageNum, int pageSize)
    {
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
