package com.ruoyi.mall.mapper;

import com.ruoyi.mall.domain.MallPaymentRecord;

public interface MallPaymentRecordMapper
{
    int insertPaymentRecord(MallPaymentRecord record);

    MallPaymentRecord selectByPaymentNo(String paymentNo);

    MallPaymentRecord selectByOrderId(Long orderId);

    int updateStatus(Long recordId, String status, String gcashRaw);
}
