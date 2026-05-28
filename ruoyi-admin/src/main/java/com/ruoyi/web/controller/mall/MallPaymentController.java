package com.ruoyi.web.controller.mall;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallPaymentRecord;
import com.ruoyi.mall.mapper.MallPaymentRecordMapper;

@Controller
@RequestMapping("/mall/payment")
public class MallPaymentController extends BaseController
{
    private final String prefix = "mall/payment";

    @Autowired
    private MallPaymentRecordMapper paymentMapper;

    @RequiresPermissions("mall:payment:view")
    @GetMapping
    public String payment()
    {
        return prefix + "/payment";
    }

    @RequiresPermissions("mall:payment:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallPaymentRecord query)
    {
        startPage();
        return getDataTable(paymentMapper.selectList(query));
    }
}
