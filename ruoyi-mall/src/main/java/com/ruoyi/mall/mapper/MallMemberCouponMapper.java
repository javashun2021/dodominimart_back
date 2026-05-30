package com.ruoyi.mall.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallMemberCoupon;

public interface MallMemberCouponMapper
{
    int insertMemberCoupon(MallMemberCoupon coupon);

    /** 查会员所有券（JOIN mall_coupon 填充模板字段），按状态+到期时间排序 */
    List<MallMemberCoupon> selectByMemberId(Long memberId);

    /** SELECT ... FOR UPDATE 防并发重复使用 */
    MallMemberCoupon selectByIdForUpdate(Long id);

    int markUsed(@Param("id") Long id, @Param("orderNo") String orderNo, @Param("usedTime") Date usedTime);

    /** 批量过期已超时的券，供定时任务调用 */
    int expireOverdue();

    /** 管理后台分页查询（JOIN mall_coupon + mall_member） */
    List<MallMemberCoupon> selectMemberCouponList(MallMemberCoupon query);
}
