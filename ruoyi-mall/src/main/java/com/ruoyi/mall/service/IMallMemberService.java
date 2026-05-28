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

    /**
     * 生成并存储邮箱验证码（60 秒防刷，10 分钟有效），返回验证码供调用方发送邮件
     */
    String createVerifyCode(String email);

    /**
     * 邮箱注册：校验验证码、格式/重复，BCrypt 哈希密码，写入新会员
     */
    MallMember registerByEmail(String email, String code, String password, String nickName);

    /**
     * 邮箱登录：校验密码，返回会员
     */
    MallMember loginByEmail(String email, String password);

    int updateMember(MallMember member);
}
