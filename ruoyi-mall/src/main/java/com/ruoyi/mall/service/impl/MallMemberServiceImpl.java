package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallMemberService;

@Service
public class MallMemberServiceImpl implements IMallMemberService
{
    @Autowired
    private MallMemberMapper memberMapper;

    @Override
    public List<MallMember> selectMemberList(MallMember member)
    {
        return memberMapper.selectMemberList(member);
    }

    @Override
    public MallMember selectMemberById(Long memberId)
    {
        return memberMapper.selectMemberById(memberId);
    }

    @Override
    public MallMember loginOrRegisterByGoogle(String googleId, String email, String nickName, String avatarUrl)
    {
        MallMember member = memberMapper.selectMemberByGoogleId(googleId);
        if (member != null)
        {
            // 同步最新头像和昵称
            member.setNickName(nickName);
            member.setAvatarUrl(avatarUrl);
            member.setUpdateTime(new Date());
            memberMapper.updateMember(member);
            return member;
        }
        member = new MallMember();
        member.setGoogleId(googleId);
        member.setEmail(email);
        member.setNickName(nickName);
        member.setAvatarUrl(avatarUrl);
        member.setStatus("0");
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        return member;
    }

    @Override
    public MallMember loginOrRegisterByApple(String appleId, String email, String nickName)
    {
        MallMember member = memberMapper.selectMemberByAppleId(appleId);
        if (member != null)
        {
            return member;
        }
        member = new MallMember();
        member.setAppleId(appleId);
        member.setEmail(email != null ? email : "");
        member.setNickName(nickName != null ? nickName : "Apple User");
        member.setStatus("0");
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        return member;
    }

    @Override
    public int updateMember(MallMember member)
    {
        member.setUpdateTime(new Date());
        return memberMapper.updateMember(member);
    }
}
