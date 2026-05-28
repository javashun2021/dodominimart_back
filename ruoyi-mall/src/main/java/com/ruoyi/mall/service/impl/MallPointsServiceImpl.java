package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallPointsLog;
import com.ruoyi.mall.mapper.MallPointsMapper;
import com.ruoyi.mall.service.IMallPointsService;

@Service
public class MallPointsServiceImpl implements IMallPointsService
{
    @Autowired
    private MallPointsMapper pointsMapper;

    @Override
    @Transactional
    public void earn(Long memberId, int delta, int source, String sourceId, String remark)
    {
        if (delta <= 0) return;
        pointsMapper.addPoints(memberId, delta);
        int balance = pointsMapper.selectBalance(memberId);
        MallPointsLog log = new MallPointsLog();
        log.setMemberId(memberId);
        log.setDelta(delta);
        log.setBalanceAfter(balance);
        log.setSource(source);
        log.setSourceId(sourceId);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        pointsMapper.insertLog(log);
    }

    @Override
    @Transactional
    public int deduct(Long memberId, int points, String sourceId)
    {
        if (points <= 0) return 0;
        int affected = pointsMapper.addPoints(memberId, -points);
        if (affected == 0) return 0; // 余额不足，WHERE 条件未命中
        int balance = pointsMapper.selectBalance(memberId);
        MallPointsLog log = new MallPointsLog();
        log.setMemberId(memberId);
        log.setDelta(-points);
        log.setBalanceAfter(balance);
        log.setSource(4);
        log.setSourceId(sourceId);
        log.setRemark("Points redeemed for order #" + sourceId);
        log.setCreateTime(new Date());
        pointsMapper.insertLog(log);
        return points;
    }

    @Override
    public int getBalance(Long memberId)
    {
        return pointsMapper.selectBalance(memberId);
    }

    @Override
    public List<MallPointsLog> getHistory(Long memberId)
    {
        return pointsMapper.selectLogsByMemberId(memberId);
    }

    @Override
    public List<MallPointsLog> listAllLogs(Long memberId)
    {
        return pointsMapper.selectAllLogs(memberId);
    }
}
