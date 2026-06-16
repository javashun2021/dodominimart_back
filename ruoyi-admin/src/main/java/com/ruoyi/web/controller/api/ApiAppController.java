package com.ruoyi.web.controller.api;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.utils.IpUtils;
import com.ruoyi.framework.jwt.JwtUtils;
import com.ruoyi.mall.domain.AppErrorLog;
import com.ruoyi.mall.service.IAppErrorLogService;

/**
 * App 客户端错误/崩溃上报接口（无需登录，匿名也能上报）
 * POST /api/v1/app/error-log
 *
 * 上报内容写入独立日志文件 app-error.log（见 logback.xml 的 APP_CLIENT_ERROR logger），
 * 方便后续排查 App 端发生了什么错。
 */
@RestController
@RequestMapping("/api/v1/app")
public class ApiAppController
{
    /** 专用 logger，对应 logback.xml 里的 app-error.log 文件 */
    private static final Logger APP_LOG = LoggerFactory.getLogger("APP_CLIENT_ERROR");

    // 防御性截断，避免恶意/异常超长内容刷爆日志（App 端已截断，这里再兜一层）
    private static final int MAX_MSG = 2000;
    private static final int MAX_STACK = 8000;
    private static final int MAX_CTX = 1000;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private IAppErrorLogService errorLogService;

    @PostMapping("/error-log")
    public AjaxResult reportError(@RequestBody(required = false) Map<String, Object> body,
                                  HttpServletRequest request)
    {
        if (body == null)
        {
            return AjaxResult.success("ok");
        }

        String platform   = str(body, "platform",   MAX_MSG);
        String appVersion = str(body, "appVersion", MAX_MSG);
        String type       = str(body, "type",       MAX_MSG);
        String message    = str(body, "message",    MAX_MSG);
        String stack      = str(body, "stack",      MAX_STACK);
        String context    = str(body, "context",    MAX_CTX);
        String time       = str(body, "time",       MAX_MSG);

        Long memberId = resolveMemberId(request);
        String ip = IpUtils.getIpAddr(request);

        // 单条记录：抬头一行便于 grep，堆栈换行跟随其后
        APP_LOG.error("App客户端错误 platform={} ver={} type={} member={} ip={} time={} ctx={} | {}\n{}",
                platform, appVersion, type, memberId, ip, time, context, message, stack);

        // 落库：后台「App错误日志」可查；DB 异常绝不影响上报接口（已有日志兜底）
        try
        {
            AppErrorLog log = new AppErrorLog();
            log.setPlatform(platform);
            log.setAppVersion(appVersion);
            log.setType(type);
            log.setMessage(message);
            log.setStack(stack);
            log.setContext(context);
            log.setMemberId(memberId);
            log.setIp(ip);
            log.setClientTime(time);
            errorLogService.insertErrorLog(log);
        }
        catch (Exception e)
        {
            APP_LOG.warn("App错误日志落库失败（已写入日志文件，可忽略）：{}", e.getMessage());
        }

        return AjaxResult.success("ok");
    }

    /** best-effort 解析上报者 memberId（匿名上报时为 null，绝不抛异常） */
    private Long resolveMemberId(HttpServletRequest request)
    {
        try
        {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer "))
            {
                String token = header.substring(7).trim();
                if (jwtUtils.validateToken(token))
                {
                    return jwtUtils.getMemberIdFromToken(token);
                }
            }
        }
        catch (Exception ignored)
        {
            // 解析失败不影响错误日志落库
        }
        return null;
    }

    private static String str(Map<String, Object> body, String key, int max)
    {
        Object v = body.get(key);
        if (v == null)
        {
            return "";
        }
        String s = v.toString();
        return s.length() <= max ? s : s.substring(0, max) + "…[truncated]";
    }
}
