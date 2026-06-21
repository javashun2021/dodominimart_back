package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallPosHeldCart;

/**
 * POS 暂存挂单 服务
 */
public interface IMallPosHeldCartService
{
    /** 暂存当前购物车（label 为空时自动生成"暂存#N"），返回带 id 的记录 */
    MallPosHeldCart hold(MallPosHeldCart cart);

    /** 暂存中列表（status=0，不限本人） */
    List<MallPosHeldCart> listOpen();

    /** 取出某暂存（含明细 cart_json） */
    MallPosHeldCart get(Long id);

    /** 恢复结算后置为已结算(1) */
    int markResolved(Long id);

    /** 作废暂存(2) */
    int voidCart(Long id);
}
