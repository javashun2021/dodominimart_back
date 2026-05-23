package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.mapper.MallAddressMapper;
import com.ruoyi.mall.service.IMallAddressService;

@Service
public class MallAddressServiceImpl implements IMallAddressService
{
    @Autowired
    private MallAddressMapper addressMapper;

    @Override
    public List<MallAddress> selectAddressByMemberId(Long memberId)
    {
        return addressMapper.selectAddressByMemberId(memberId);
    }

    @Override
    public MallAddress selectAddressById(Long addressId)
    {
        return addressMapper.selectAddressById(addressId);
    }

    @Override
    @Transactional
    public int insertAddress(MallAddress address)
    {
        // 设为默认地址时，先清除该会员其他默认
        if ("1".equals(address.getIsDefault()))
        {
            addressMapper.clearDefaultByMemberId(address.getMemberId());
        }
        address.setCreateTime(new Date());
        return addressMapper.insertAddress(address);
    }

    @Override
    @Transactional
    public int updateAddress(MallAddress address)
    {
        if ("1".equals(address.getIsDefault()))
        {
            addressMapper.clearDefaultByMemberId(address.getMemberId());
        }
        address.setUpdateTime(new Date());
        return addressMapper.updateAddress(address);
    }

    @Override
    public int deleteAddressById(Long addressId)
    {
        return addressMapper.deleteAddressById(addressId);
    }
}
