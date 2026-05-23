package com.ruoyi.web.controller.mall;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import com.ruoyi.mall.domain.MallGroupActivity;
import com.ruoyi.mall.domain.MallGroupTier;
import com.ruoyi.mall.service.IMallGroupService;

@Controller
@RequestMapping("/mall/group")
public class MallGroupActivityController extends BaseController
{
    private final String prefix = "mall/group";

    @Autowired
    private IMallGroupService groupService;

    @RequiresPermissions("mall:group:view")
    @GetMapping
    public String group()
    {
        return prefix + "/group";
    }

    @RequiresPermissions("mall:group:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallGroupActivity activity)
    {
        startPage();
        return getDataTable(groupService.selectActivityList(activity));
    }

    @RequiresPermissions("mall:group:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("mall:group:add")
    @Log(title = "拼团活动", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallGroupActivity activity,
                               @RequestParam(value = "tiersJson", required = false) String tiersJson)
    {
        activity.setTiers(parseTiers(tiersJson));
        return toAjax(groupService.insertActivity(activity));
    }

    @RequiresPermissions("mall:group:edit")
    @GetMapping("/edit/{activityId}")
    public String edit(@PathVariable Long activityId, ModelMap mmap)
    {
        mmap.put("activity", groupService.selectActivityById(activityId));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:group:edit")
    @Log(title = "拼团活动", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallGroupActivity activity,
                                @RequestParam(value = "tiersJson", required = false) String tiersJson)
    {
        activity.setTiers(parseTiers(tiersJson));
        return toAjax(groupService.updateActivity(activity));
    }

    @RequiresPermissions("mall:group:remove")
    @Log(title = "拼团活动", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(Long activityId)
    {
        return toAjax(groupService.deleteActivityById(activityId));
    }

    /** 将前端传入的 tiersJson（格式：minQty,maxQty,price;...）解析为 MallGroupTier 列表 */
    private List<MallGroupTier> parseTiers(String tiersJson)
    {
        List<MallGroupTier> tiers = new ArrayList<>();
        if (tiersJson == null || tiersJson.trim().isEmpty()) return tiers;
        for (String row : tiersJson.split(";"))
        {
            String[] parts = row.split(",");
            if (parts.length < 2) continue;
            MallGroupTier t = new MallGroupTier();
            t.setMinQuantity(Integer.parseInt(parts[0].trim()));
            t.setMaxQuantity(parts[1].trim().isEmpty() ? null : Integer.parseInt(parts[1].trim()));
            t.setPrice(new BigDecimal(parts[2].trim()));
            tiers.add(t);
        }
        return tiers;
    }
}
