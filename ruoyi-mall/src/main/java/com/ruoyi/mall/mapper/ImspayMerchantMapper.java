package com.ruoyi.mall.mapper;

import com.ruoyi.mall.domain.ImspayMerchant;

public interface ImspayMerchantMapper
{
    /** 按商户号/站点码查商户 */
    ImspayMerchant selectByCode(String code);

    /** 按商户id查商户 */
    ImspayMerchant selectById(String id);
}
