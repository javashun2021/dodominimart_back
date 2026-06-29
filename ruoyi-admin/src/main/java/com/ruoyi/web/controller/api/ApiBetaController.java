package com.ruoyi.web.controller.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.framework.mail.MailService;
import com.ruoyi.mall.domain.MallBetaTester;
import com.ruoyi.mall.mapper.MallBetaTesterMapper;
import com.ruoyi.mall.service.TelegramNotifyService;

/**
 * Android 封闭测试「内测申请」接口（公开，Shiro anon）。
 * 配合 https://dodominimart.com/download.html 使用：
 *   POST /api/v1/beta/apply   {email}        顾客申请 -> 落库 pending + 纸飞机通知管理员（含审批链接）
 *   GET  /api/v1/beta/status?email=...        顾客回查状态 none/pending/approved
 *   GET  /api/v1/beta/approve?email=&token=   管理员点纸飞机里的链接审批通过（带签名校验）
 */
@RestController
@RequestMapping("/api/v1/beta")
public class ApiBetaController
{
    @Autowired
    private MallBetaTesterMapper betaMapper;

    @Autowired
    private TelegramNotifyService telegram;

    @Autowired
    private MailService mailService;

    /** 审批链接签名盐，可用环境变量 MALL_BETA_APPROVE_SECRET 覆盖 */
    @Value("${mall.beta.approve-secret:dodo-beta-7Qk2pX}")
    private String approveSecret;

    /** 对外站点根，用于拼审批链接 */
    @Value("${mall.beta.public-base-url:https://dodominimart.com}")
    private String publicBaseUrl;

    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // ---------------------------------------------------------------- apply

    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody Map<String, Object> body, HttpServletRequest request)
    {
        String email = normalize(body == null ? null : body.get("email"));
        if (email == null || !EMAIL.matcher(email).matches())
        {
            return AjaxResult.error(400, "Please enter a valid email address.");
        }

        MallBetaTester existing = betaMapper.selectByEmail(email);
        if (existing != null)
        {
            // 已存在：幂等返回当前状态，不重复打扰管理群
            return AjaxResult.success("ok").put("status", existing.getStatus());
        }

        MallBetaTester t = new MallBetaTester();
        t.setEmail(email);
        t.setStatus(MallBetaTester.STATUS_PENDING);
        t.setSource("web");
        t.setIp(clientIp(request));
        t.setCreateTime(new Date());
        betaMapper.insertTester(t);

        // 纸飞机通知管理员（英文），附带一键审批链接
        String link = approveLink(email);
        telegram.notify("📱 New Android Tester Request\n"
                + "Email: " + email + "\n\n"
                + "Add this email in Play Console -> Closed testing -> Testers,\n"
                + "then tap to confirm:\n" + link);

        return AjaxResult.success("ok").put("status", MallBetaTester.STATUS_PENDING);
    }

    // --------------------------------------------------------------- status

    @GetMapping("/status")
    public AjaxResult status(@RequestParam("email") String emailRaw)
    {
        String email = normalize(emailRaw);
        if (email == null)
        {
            return AjaxResult.success("ok").put("status", "none");
        }
        MallBetaTester t = betaMapper.selectByEmail(email);
        return AjaxResult.success("ok").put("status", t == null ? "none" : t.getStatus());
    }

    // -------------------------------------------------------------- approve

    @GetMapping(value = "/approve", produces = "text/html; charset=UTF-8")
    @ResponseBody
    public String approve(@RequestParam("email") String emailRaw,
                          @RequestParam("token") String token)
    {
        String email = normalize(emailRaw);
        if (email == null || token == null || !token.equals(sign(email)))
        {
            return page("Invalid link", "#d93636",
                    "This approval link is invalid or has expired.");
        }

        MallBetaTester t = betaMapper.selectByEmail(email);
        if (t == null)
        {
            return page("Not found", "#d93636",
                    "No application found for <b>" + esc(email) + "</b>.");
        }

        boolean justApproved = false;
        if (!MallBetaTester.STATUS_APPROVED.equals(t.getStatus()))
        {
            MallBetaTester upd = new MallBetaTester();
            upd.setEmail(email);
            upd.setApproveTime(new Date());
            betaMapper.approveByEmail(upd);
            justApproved = true;
        }

        // 仅在「本次刚审批通过」时给会员发一封 Google Play 内测邀请邮件（与 Batch Invite 同一封）。
        // 重复点审批链接不会重复发邮件；邮件失败不影响审批结果（已落库）。
        boolean mailSent = false;
        if (justApproved)
        {
            try
            {
                mailService.sendTesterInvitation(email);
                mailSent = true;
            }
            catch (Exception ex)
            {
                // 吞掉异常：审批已成功，仅邮件没发出去
            }

            telegram.notify("✅ Added as tester\n"
                    + "Email: " + email + "\n"
                    + (mailSent ? "Invitation email sent ✉️\n" : "⚠️ Invitation email FAILED to send\n")
                    + "This member can now download on Google Play.");
        }

        return page("Approved ✓", "#1a9d52",
                "<b>" + esc(email) + "</b> has been approved as a tester.<br>"
                        + (justApproved
                            ? (mailSent
                                ? "An invitation email has been sent to them."
                                : "Note: the invitation email could not be sent — please invite manually.")
                            : "They can now download the app on Google Play."));
    }

    // --------------------------------------------------------------- helpers

    private String approveLink(String email)
    {
        return publicBaseUrl + "/api/v1/beta/approve?email="
                + urlEncode(email) + "&token=" + sign(email);
    }

    /** token = sha256(email:secret) 取前16位 hex，避免任意人乱点审批 */
    private String sign(String email)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((email + ":" + approveSecret).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                sb.append(Character.forDigit((d[i] >> 4) & 0xF, 16));
                sb.append(Character.forDigit(d[i] & 0xF, 16));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            return "x";
        }
    }

    private static String normalize(Object v)
    {
        if (v == null) return null;
        String s = v.toString().trim().toLowerCase();
        return s.isEmpty() ? null : s;
    }

    private static String clientIp(HttpServletRequest req)
    {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty())
        {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }

    private static String urlEncode(String s)
    {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private static String esc(String s)
    {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String page(String title, String color, String msg)
    {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + esc(title) + "</title></head>"
                + "<body style=\"font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;"
                + "background:#f6f7f9;margin:0;padding:60px 20px;text-align:center;color:#1f2328\">"
                + "<div style=\"max-width:420px;margin:0 auto;background:#fff;border-radius:18px;"
                + "padding:36px 28px;box-shadow:0 6px 20px rgba(0,0,0,.08)\">"
                + "<div style=\"font-size:44px;line-height:1;margin-bottom:14px;color:" + color + "\">"
                + esc(title) + "</div>"
                + "<p style=\"font-size:15px;color:#5b6470;line-height:1.6\">" + msg + "</p>"
                + "</div></body></html>";
    }
}
