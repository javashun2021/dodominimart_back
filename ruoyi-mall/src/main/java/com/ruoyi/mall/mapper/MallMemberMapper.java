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

    int insertMember(MallMember member);

    int updateMember(MallMember member);

    int updateFcmToken(@Param("memberId") Long memberId, @Param("fcmToken") String fcmToken);
}
