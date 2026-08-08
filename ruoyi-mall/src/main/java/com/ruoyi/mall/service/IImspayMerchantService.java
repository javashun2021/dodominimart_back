package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.ImspayMerchant;

/**
 * 商户管理服务（后台）。
 */
public interface IImspayMerchantService
{
    List<ImspayMerchant> selectMerchantList(ImspayMerchant query);

    ImspayMerchant selectById(String id);

    /** 新增商户：自动生成 id 与 app_secret(MD5密钥) */
    int insertMerchant(ImspayMerchant merchant);

    int updateMerchant(ImspayMerchant merchant);

    int deleteByIds(String ids);

    /** 重置签名密钥，返回新密钥 */
    String resetKey(String id);
}
