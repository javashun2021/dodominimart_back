package com.ruoyi.framework.mail;

import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class MailService
{
    @Autowired
    private JavaMailSender mailSender;

    @Value("${mall.mail.from}")
    private String from;

    @Value("${mall.mail.from-name}")
    private String fromName;

    public void sendVerifyCode(String toEmail, String code)
    {
        SimpleMailMessage msg = new SimpleMailMessage();
        try
        {
            msg.setFrom(new InternetAddress(from, fromName, "UTF-8").toString());
        }
        catch (UnsupportedEncodingException e)
        {
            msg.setFrom(from);
        }
        msg.setTo(toEmail);
        msg.setSubject("星智商城 - 验证码");
        msg.setText("您的验证码是：" + code
                + "\n\n验证码 10 分钟内有效，请勿泄露给他人。");
        mailSender.send(msg);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param toEmail 收件人
     * @param subject 主题
     * @param html    HTML 正文
     */
    public void sendHtml(String toEmail, String subject, String html) throws MessagingException
    {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        try
        {
            helper.setFrom(new InternetAddress(from, fromName, "UTF-8").toString());
        }
        catch (UnsupportedEncodingException e)
        {
            helper.setFrom(from);
        }
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(mime);
    }

    /** Google Play 内测邀请页指引 */
    private static final String GUIDE_URL = "https://dodominimart.com/tester-guide.html";

    /** Google Play 内测加入链接 */
    private static final String TEST_URL = "https://play.google.com/apps/testing/com.dodominimart.app";

    /**
     * 发送 Google Play 内测邀请邮件
     *
     * @param toEmail 收件人邮箱
     */
    public void sendTesterInvitation(String toEmail) throws MessagingException
    {
        String html = ""
            + "<div style=\"font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;color:#1f2328\">"
            +   "<div style=\"background:linear-gradient(135deg,#E85D04,#c44e02);color:#fff;text-align:center;padding:32px 24px;border-radius:16px 16px 0 0\">"
            +     "<div style=\"font-size:40px\">&#128722;</div>"
            +     "<h1 style=\"margin:8px 0 0;font-size:22px\">You're Invited to Test DodoMiniMart</h1>"
            +   "</div>"
            +   "<div style=\"background:#fff;border:1px solid #ececec;border-top:none;padding:28px 28px;border-radius:0 0 16px 16px\">"
            +     "<p style=\"font-size:15px;line-height:1.65\">Hi there,</p>"
            +     "<p style=\"font-size:15px;line-height:1.65\">Thank you for helping test <b>DodoMiniMart</b> &#127881; "
            +       "Please follow the steps below to install the app from Google Play.</p>"
            +     "<div style=\"text-align:center;margin:26px 0\">"
            +       "<a href=\"" + TEST_URL + "\" style=\"display:inline-block;background:#E85D04;color:#fff;text-decoration:none;font-weight:700;font-size:16px;padding:14px 34px;border-radius:999px\">&#9654; Join the Test</a>"
            +     "</div>"
            +     "<ol style=\"font-size:15px;line-height:1.8;padding-left:20px\">"
            +       "<li>Open the link above and sign in with your <b>Google (Gmail)</b> account.</li>"
            +       "<li>Tap <b>\"Become a Tester\"</b> &mdash; this step is required.</li>"
            +       "<li>Tap <b>\"Download it on Google Play\"</b>, then <b>Install</b>.</li>"
            +       "<li>Open the app and start testing!</li>"
            +     "</ol>"
            +     "<div style=\"background:#fff5f5;border:1px solid #ffd9d9;border-radius:10px;padding:12px 14px;font-size:14px;color:#7a1f1f;margin:18px 0\">"
            +       "&#9888;&#65039; <b>Important:</b> You must tap \"Become a Tester\", otherwise you won't be able to download the app."
            +     "</div>"
            +     "<p style=\"font-size:14px;line-height:1.65\">Need the full step-by-step guide with screenshots? "
            +       "<a href=\"" + GUIDE_URL + "\" style=\"color:#c44e02;font-weight:600\">View the Tester Guide &rarr;</a></p>"
            +     "<p style=\"font-size:14px;color:#5b6470\">Testing link:<br><a href=\"" + TEST_URL + "\" style=\"color:#c44e02;word-break:break-all\">" + TEST_URL + "</a></p>"
            +     "<hr style=\"border:none;border-top:1px solid #ececec;margin:22px 0\">"
            +     "<p style=\"font-size:13px;color:#9aa3ad;text-align:center\">Please do not share this link publicly. &mdash; The DodoMiniMart Team</p>"
            +   "</div>"
            + "</div>";
        sendHtml(toEmail, "You're invited to test DodoMiniMart on Google Play", html);
    }
}
