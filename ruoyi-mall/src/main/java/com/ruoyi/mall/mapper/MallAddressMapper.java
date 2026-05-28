package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallAddress;

public interface MallAddressMapper
{
    List<MallAddress> selectAddressByMemberId(Long memberId);

    MallAddress selectAddressById(Long addressId);

    int insertAddress(MallAddress address);

    int updateAddress(MallAddress address);

    int deleteAddressById(Long addressId);

    int clearDefaultByMemberId(Long memberId);

    MallAddress selectDefaultAddressByMemberId(Long memberId);
}
