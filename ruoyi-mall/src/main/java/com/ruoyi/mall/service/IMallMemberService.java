package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallMember;

public interface IMallMemberService
{
    List<MallMember> selectMemberList(MallMember member);

    MallMember selectMemberById(Long memberId);

    /**
     * Google 登录：存在则返回，不存在则自动注册
     */
    MallMember loginOrRegisterByGoogle(String googleId, String email, String nickName, String avatarUrl);

    /**
     * Apple 登录：存在则返回，不存在则自动注册
     */
    MallMember loginOrRegisterByApple(String appleId, String email, String nickName);

    int updateMember(MallMember member);
}
