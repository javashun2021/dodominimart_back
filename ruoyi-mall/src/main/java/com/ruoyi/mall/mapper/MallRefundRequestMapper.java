package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallRefundRequest;

public interface MallRefundRequestMapper
{
    int insertRefundRequest(MallRefundRequest req);

    MallRefundRequest selectById(Long requestId);

    /** 某订单最新一条申请（用于 app 详情展示 / 防重复申请） */
    MallRefundRequest selectLatestByOrderId(Long orderId);

    /** 后台列表（连表订单号/金额/会员昵称） */
    List<MallRefundRequest> selectList(MallRefundRequest query);

    /** 标记处理结果（通过/驳回） */
    int updateHandle(MallRefundRequest req);
}
