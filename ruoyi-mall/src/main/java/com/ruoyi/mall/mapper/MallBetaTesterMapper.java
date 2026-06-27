package com.ruoyi.mall.mapper;

import com.ruoyi.mall.domain.MallBetaTester;

/**
 * Android 内测申请 Mapper
 */
public interface MallBetaTesterMapper
{
    /** 按邮箱查（唯一） */
    MallBetaTester selectByEmail(String email);

    /** 新增申请 */
    int insertTester(MallBetaTester tester);

    /** 审批通过：按邮箱置 approved + approveTime */
    int approveByEmail(MallBetaTester tester);
}
