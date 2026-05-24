package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallMemberService;

@Service
public class MallMemberServiceImpl implements IMallMemberService
{
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}$");
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

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
    public MallMember registerByEmail(String email, String password, String nickName)
    {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches())
        {
            throw new RuntimeException("Invalid email format");
        }
        if (password == null || password.length() < 8)
        {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (nickName == null || nickName.trim().isEmpty())
        {
            throw new RuntimeException("nickName is required");
        }

        MallMember existing = memberMapper.selectMemberByEmail(email.trim());
        if (existing != null)
        {
            if (existing.getPasswordHash() != null)
            {
                throw new RuntimeException("Email already registered");
            }
            // Google 或 Apple 账号
            throw new RuntimeException("Email already linked to a social account. Please use Google or Apple Sign-In.");
        }

        MallMember member = new MallMember();
        member.setEmail(email.trim());
        member.setNickName(nickName.trim());
        member.setAvatarUrl("");
        member.setStatus("0");
        member.setPasswordHash(BCRYPT.encode(password));
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        return member;
    }

    @Override
    public MallMember loginByEmail(String email, String password)
    {
        final String ERR = "Invalid email or password";
        if (email == null || password == null)
        {
            throw new RuntimeException(ERR);
        }
        MallMember member = memberMapper.selectMemberByEmail(email.trim());
        if (member == null || member.getPasswordHash() == null)
        {
            throw new RuntimeException(ERR);
        }
        if (!BCRYPT.matches(password, member.getPasswordHash()))
        {
            throw new RuntimeException(ERR);
        }
        if (!"0".equals(member.getStatus()))
        {
            throw new RuntimeException("Account is disabled");
        }
        return member;
    }

    @Override
    public int updateMember(MallMember member)
    {
        member.setUpdateTime(new Date());
        return memberMapper.updateMember(member);
    }
}
