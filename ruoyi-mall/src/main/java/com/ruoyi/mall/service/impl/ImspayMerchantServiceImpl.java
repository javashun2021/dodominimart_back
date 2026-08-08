package com.ruoyi.mall.service.impl;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.ImspayMerchant;
import com.ruoyi.mall.mapper.ImspayMerchantMapper;
import com.ruoyi.mall.service.IImspayMerchantService;

@Service
public class ImspayMerchantServiceImpl implements IImspayMerchantService
{
    @Autowired
    private ImspayMerchantMapper merchantMapper;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Override
    public List<ImspayMerchant> selectMerchantList(ImspayMerchant query)
    {
        return merchantMapper.selectMerchantList(query);
    }

    @Override
    public ImspayMerchant selectById(String id)
    {
        return merchantMapper.selectById(id);
    }

    @Override
    public int insertMerchant(ImspayMerchant merchant)
    {
        if (merchant.getId() == null || merchant.getId().isEmpty())
        {
            merchant.setId(UUID.randomUUID().toString());
        }
        if (merchant.getAppSecret() == null || merchant.getAppSecret().isEmpty())
        {
            merchant.setAppSecret(genKey());
        }
        if (merchant.getStatus() == null || merchant.getStatus().isEmpty())
        {
            merchant.setStatus("Enable");
        }
        return merchantMapper.insertMerchant(merchant);
    }

    @Override
    public int updateMerchant(ImspayMerchant merchant)
    {
        return merchantMapper.updateMerchant(merchant);
    }

    @Override
    public int deleteByIds(String ids)
    {
        int n = 0;
        for (String id : ids.split(","))
        {
            n += merchantMapper.deleteById(id.trim());
        }
        return n;
    }

    @Override
    public String resetKey(String id)
    {
        ImspayMerchant m = new ImspayMerchant();
        m.setId(id);
        String key = genKey();
        m.setAppSecret(key);
        merchantMapper.updateMerchant(m);
        return key;
    }

    /** 生成 32 位十六进制密钥 */
    private static String genKey()
    {
        char[] c = new char[32];
        for (int i = 0; i < c.length; i++)
        {
            c[i] = HEX[RANDOM.nextInt(16)];
        }
        return new String(c);
    }
}
