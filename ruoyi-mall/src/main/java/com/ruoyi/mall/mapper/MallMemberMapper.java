package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallMember;

public interface MallMemberMapper
{
    List<MallMember> selectMemberList(MallMember member);

    MallMember selectMemberById(Long memberId);

    List<MallMember> selectMembersByIds(@Param("ids") List<Long> ids);

    MallMember selectMemberByGoogleId(String googleId);

    MallMember selectMemberByAppleId(String appleId);

    MallMember selectMemberByEmail(String email);

    /** 按手机号查会员（POS 收银台关联会员用，取最早一个） */
    MallMember selectMemberByPhone(String phone);

    int insertMember(MallMember member);

    int updateMember(MallMember member);

    /** 后台设置会员角色：customer/cashier/admin */
    int updateRole(@Param("memberId") Long memberId, @Param("role") String role);

    int updateFcmToken(@Param("memberId") Long memberId, @Param("fcmToken") String fcmToken);

    /** 设置/修改登录密码（BCrypt 哈希） */
    int updatePassword(@Param("memberId") Long memberId, @Param("passwordHash") String passwordHash);

    MallMember selectByInviteCode(String inviteCode);

    int updateInviteCode(@Param("memberId") Long memberId, @Param("inviteCode") String inviteCode);

    int updateReferrerId(@Param("memberId") Long memberId, @Param("referrerId") Long referrerId);

    /** 查询被某会员邀请注册的所有人，含已完成订单数 */
    List<MallMember> selectReferredMembers(Long referrerId);
}
