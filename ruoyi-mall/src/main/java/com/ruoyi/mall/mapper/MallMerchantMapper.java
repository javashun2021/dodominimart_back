package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallMerchant;

public interface MallMerchantMapper
{
    List<MallMerchant> selectMerchantList(MallMerchant merchant);

    MallMerchant selectMerchantById(Long merchantId);

    /** 所有营业中(status=1)的商家，供买家侧附近排序遍历 */
    List<MallMerchant> selectActiveMerchants();

    /** 某地推员录入的商家（按创建时间倒序） */
    List<MallMerchant> selectByPromoter(Long promoterId);

    /** 会员本人的店铺/开店申请（一人一店，取最新一条非删除记录） */
    MallMerchant selectByOwner(Long ownerMemberId);

    /** 统计某地推员录入的商家总数（业绩） */
    int countByPromoter(Long promoterId);

    int insertMerchant(MallMerchant merchant);

    int updateMerchant(MallMerchant merchant);

    /** 审核状态变更（approve/reject/停业）：status + reject_reason + review_time + reviewer */
    int updateReview(MallMerchant merchant);

    int deleteMerchantById(Long merchantId);
}
