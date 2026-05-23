package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallMember;

public interface MallMemberMapper
{
    List<MallMember> selectMemberList(MallMember member);

    MallMember selectMemberById(Long memberId);

    MallMember selectMemberByGoogleId(String googleId);

    MallMember selectMemberByAppleId(String appleId);

    MallMember selectMemberByEmail(String email);

    int insertMember(MallMember member);

    int updateMember(MallMember member);
}
