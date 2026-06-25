package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallMember;

public interface IMallMemberService
{
    List<MallMember> selectMemberList(MallMember member);

    MallMember selectMemberById(Long memberId);

    /**
     * Google 登录：存在则返回，不存在则自动注册（注册时绑定邀请人、送积分）
     */
    MallMember loginOrRegisterByGoogle(String googleId, String email, String nickName, String avatarUrl, String referralCode);

    /**
     * Apple 登录：存在则返回，不存在则自动注册（注册时绑定邀请人、送积分）
     */
    MallMember loginOrRegisterByApple(String appleId, String email, String nickName, String referralCode);

    /**
     * 生成并存储邮箱验证码（60 秒防刷，10 分钟有效），返回验证码供调用方发送邮件
     */
    String createVerifyCode(String email);

    /**
     * 邮箱注册：校验验证码、格式/重复，BCrypt 哈希密码，写入新会员（注册时绑定邀请人、送积分）
     */
    MallMember registerByEmail(String email, String code, String password, String nickName, String referralCode);

    /**
     * 邮箱登录：校验密码，返回会员
     */
    MallMember loginByEmail(String email, String password);

    int updateMember(MallMember member);

    /** 后台设置会员角色：customer/cashier/admin */
    int updateRole(Long memberId, String role);

    /**
     * 设置 / 修改登录密码。
     * 若会员尚未设置过密码（如 Google/Apple 登录用户），oldPassword 可为空，直接设置；
     * 若已有密码，则必须校验 oldPassword 正确。
     */
    void setPassword(Long memberId, String oldPassword, String newPassword);
}
