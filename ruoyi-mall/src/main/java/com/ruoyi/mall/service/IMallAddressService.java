package com.ruoyi.mall.service;

import java.util.List;
import com.ruoyi.mall.domain.MallAddress;

public interface IMallAddressService
{
    List<MallAddress> selectAddressByMemberId(Long memberId);

    MallAddress selectAddressById(Long addressId);

    int insertAddress(MallAddress address);

    int updateAddress(MallAddress address);

    int deleteAddressById(Long addressId);

    MallAddress selectDefaultAddressByMemberId(Long memberId);

    void setDefaultAddress(Long addressId, Long memberId);
}
