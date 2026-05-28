package com.ruoyi.mall.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallVerifyCode;

public interface MallVerifyCodeMapper
{
    void insert(MallVerifyCode record);

    MallVerifyCode selectLatestByEmail(@Param("email") String email);

    void markUsed(@Param("id") Long id);

    void incrementAttempts(@Param("id") Long id);
}
