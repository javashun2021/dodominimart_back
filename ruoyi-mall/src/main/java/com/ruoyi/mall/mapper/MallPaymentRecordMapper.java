package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallPaymentRecord;

public interface MallPaymentRecordMapper
{
    int insertPaymentRecord(MallPaymentRecord record);

    MallPaymentRecord selectByPaymentNo(String paymentNo);

    MallPaymentRecord selectByOrderId(Long orderId);

    int updateStatus(@Param("recordId") Long recordId, @Param("status") String status, @Param("gcashRaw") String gcashRaw);

    List<MallPaymentRecord> selectList(MallPaymentRecord query);
}
