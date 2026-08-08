package com.ruoyi.mall.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.PayOrder;
import com.ruoyi.mall.mapper.PayOrderMapper;
import com.ruoyi.mall.service.IPayOrderService;

@Service
public class PayOrderServiceImpl implements IPayOrderService
{
    @Autowired
    private PayOrderMapper payOrderMapper;

    @Override
    public List<PayOrder> selectOrderList(PayOrder query)
    {
        return payOrderMapper.selectOrderList(query);
    }

    @Override
    public PayOrder selectByPlatformNo(String platformNo)
    {
        return payOrderMapper.selectByPlatformNo(platformNo);
    }
}
