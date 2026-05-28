package com.ruoyi.web.controller.api;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallPointsLog;
import com.ruoyi.mall.service.IMallPointsService;

/**
 * 积分接口（需 JWT）
 * GET /api/v1/points  → { balance, logs }
 */
@RestController
@RequestMapping("/api/v1/points")
public class ApiPointsController extends BaseApiController
{
    @Autowired
    private IMallPointsService pointsService;

    @GetMapping
    public AjaxResult getPoints(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        int balance = pointsService.getBalance(memberId);
        List<MallPointsLog> logs = pointsService.getHistory(memberId);
        return AjaxResult.success()
                .put("balance", balance)
                .put("logs", logs);
    }
}
