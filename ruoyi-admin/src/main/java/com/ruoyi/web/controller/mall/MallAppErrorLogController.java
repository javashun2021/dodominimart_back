package com.ruoyi.web.controller.mall;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.AppErrorLog;
import com.ruoyi.mall.service.IAppErrorLogService;

/**
 * App 客户端错误日志（后台查看）
 */
@Controller
@RequestMapping("/mall/apperror")
public class MallAppErrorLogController extends BaseController
{
    private final String prefix = "mall/apperror";

    @Autowired
    private IAppErrorLogService errorLogService;

    @RequiresPermissions("mall:apperror:view")
    @GetMapping
    public String apperror()
    {
        return prefix + "/apperror";
    }

    @RequiresPermissions("mall:apperror:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AppErrorLog log)
    {
        startPage();
        return getDataTable(errorLogService.selectErrorLogList(log));
    }

    @RequiresPermissions("mall:apperror:view")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, ModelMap mmap)
    {
        mmap.put("log", errorLogService.selectErrorLogById(id));
        return prefix + "/detail";
    }

    @RequiresPermissions("mall:apperror:remove")
    @Log(title = "App错误日志", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        String[] arr = ids.split(",");
        Long[] longIds = new Long[arr.length];
        for (int i = 0; i < arr.length; i++)
        {
            longIds[i] = Long.parseLong(arr[i].trim());
        }
        return toAjax(errorLogService.deleteErrorLogByIds(longIds));
    }

    @RequiresPermissions("mall:apperror:remove")
    @Log(title = "App错误日志", businessType = BusinessType.CLEAN)
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean(@RequestParam(defaultValue = "30") int days)
    {
        int rows = errorLogService.cleanErrorLogBefore(days);
        return AjaxResult.success("Deleted " + rows + " logs older than " + days + " days");
    }
}
