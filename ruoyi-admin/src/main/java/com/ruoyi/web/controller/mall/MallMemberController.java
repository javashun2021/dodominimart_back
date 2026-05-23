package com.ruoyi.web.controller.mall;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.service.IMallMemberService;

@Controller
@RequestMapping("/mall/member")
public class MallMemberController extends BaseController
{
    private final String prefix = "mall/member";

    @Autowired
    private IMallMemberService memberService;

    @RequiresPermissions("mall:member:view")
    @GetMapping
    public String member()
    {
        return prefix + "/member";
    }

    @RequiresPermissions("mall:member:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallMember member)
    {
        startPage();
        List<MallMember> list = memberService.selectMemberList(member);
        return getDataTable(list);
    }
}
