package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.mall.domain.MallMerchant;

public interface IMallMerchantService
{
    List<MallMerchant> selectMerchantList(MallMerchant merchant);

    MallMerchant selectMerchantById(Long merchantId);

    int insertMerchant(MallMerchant merchant);

    int updateMerchant(MallMerchant merchant);

    int deleteMerchantById(Long merchantId);

    /** 审核通过：status=1 营业 */
    void approveMerchant(Long merchantId, String reviewer);

    /** 审核拒绝：status=2 + 原因 */
    void rejectMerchant(Long merchantId, String reviewer, String rejectReason);

    /** 某地推员录入的商家 */
    List<MallMerchant> selectByPromoter(Long promoterId);

    /** 某地推员录入的商家数（业绩） */
    int countByPromoter(Long promoterId);

    /**
     * 附近商家：只取营业中(status=1)，按经纬度真实距离升序，distanceKm 已填充。
     * lat/lng 为空则按 sort 返回（不算距离）。category/keyword 可选过滤。
     */
    List<MallMerchant> selectNearby(BigDecimal lat, BigDecimal lng, String category, String keyword);
}
