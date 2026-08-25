package com.ruoyi.web.controller.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.framework.mail.MailService;
import com.ruoyi.framework.web.base.BaseController;

/**
 * Google Play 测试者批量邀请
 *
 * @author dodo
 */
@Controller
@RequestMapping("/tool/invite")
public class TesterInviteController extends BaseController
{
    private String prefix = "tool/invite";

    /** 邮箱格式校验 */
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private MailService mailService;

    @RequiresPermissions("tool:invite:view")
    @GetMapping()
    public String invite()
    {
        return prefix + "/invite";
    }

    /**
     * 批量发送邀请邮件。emails 支持换行 / 逗号 / 分号 / 空格分隔，自动去重去空。
     */
    @RequiresPermissions("tool:invite:send")
    @Log(title = "测试者邀请", businessType = BusinessType.OTHER)
    @PostMapping("/send")
    @ResponseBody
    public AjaxResult send(@RequestParam("emails") String emails)
    {
        if (emails == null || emails.trim().isEmpty())
        {
            return error("请至少输入一个邮箱地址。");
        }

        // 拆分 + 去重（保持顺序）
        Set<String> set = new LinkedHashSet<>();
        for (String part : emails.split("[\\s,;]+"))
        {
            String e = part.trim();
            if (!e.isEmpty())
            {
                set.add(e);
            }
        }

        List<String> invalid = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        int sent = 0;

        for (String email : set)
        {
            if (!EMAIL.matcher(email).matches())
            {
                invalid.add(email);
                continue;
            }
            try
            {
                mailService.sendTesterInvitation(email);
                sent++;
            }
            catch (Exception ex)
            {
                failed.add(email);
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Sent ").append(sent).append(" invitation(s).");
        if (!invalid.isEmpty())
        {
            msg.append(" Invalid: ").append(String.join(", ", invalid)).append('.');
        }
        if (!failed.isEmpty())
        {
            msg.append(" Failed: ").append(String.join(", ", failed)).append('.');
        }

        AjaxResult result = (sent > 0) ? success(msg.toString()) : error(msg.toString());
        result.put("sent", sent);
        result.put("invalid", invalid);
        result.put("failed", failed);
        return result;
    }
}
