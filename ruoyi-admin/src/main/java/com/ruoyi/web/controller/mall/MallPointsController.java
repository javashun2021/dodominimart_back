package com.ruoyi.web.controller.mall;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.service.IMallPointsService;

@Controller
@RequestMapping("/mall/points")
public class MallPointsController extends BaseController
{
    private final String prefix = "mall/points";

    @Autowired
    private IMallPointsService pointsService;

    @RequiresPermissions("mall:points:view")
    @GetMapping
    public String points()
    {
        return prefix + "/points";
    }

    @RequiresPermissions("mall:points:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(@RequestParam(required = false) Long memberId)
    {
        startPage();
        return getDataTable(pointsService.listAllLogs(memberId));
    }
}
