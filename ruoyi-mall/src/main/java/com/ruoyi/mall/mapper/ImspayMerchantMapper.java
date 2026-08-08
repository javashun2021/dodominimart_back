package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.ImspayMerchant;

public interface ImspayMerchantMapper
{
    /** 按商户号/站点码查商户 */
    ImspayMerchant selectByCode(String code);

    /** 按商户id查商户 */
    ImspayMerchant selectById(String id);

    /** 后台列表查询 */
    List<ImspayMerchant> selectMerchantList(ImspayMerchant query);

    int insertMerchant(ImspayMerchant merchant);

    int updateMerchant(ImspayMerchant merchant);

    int deleteById(String id);
}
