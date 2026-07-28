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

    /** 会员本人的店铺/开店申请（一人一店，无则 null） */
    MallMerchant getMyMerchant(Long ownerMemberId);

    /**
     * 会员自助开店：提交/更新开店申请（进后台待审核 status=0）。
     * 已有营业中(status=1)的店则拒绝；待审(0)可编辑，被拒(2)/停业(3)可重新提交。
     * 服务端强制 ownerMemberId=当前会员、promoterId=null、status=0。
     */
    MallMerchant applyMerchant(MallMerchant draft, Long ownerMemberId);

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
