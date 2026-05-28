package com.ruoyi.web.controller.mall;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallProductReview;
import com.ruoyi.mall.service.IMallReviewService;

@Controller
@RequestMapping("/mall/review")
public class MallReviewController extends BaseController
{
    private final String prefix = "mall/review";

    @Autowired
    private IMallReviewService reviewService;

    @RequiresPermissions("mall:review:view")
    @GetMapping
    public String review()
    {
        return prefix + "/review";
    }

    @RequiresPermissions("mall:review:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallProductReview query)
    {
        startPage();
        List<MallProductReview> list = reviewService.listAllReviews(query);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:review:remove")
    @Log(title = "评价管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        for (String id : ids.split(","))
        {
            reviewService.deleteReview(Long.parseLong(id.trim()));
        }
        return success();
    }
}
