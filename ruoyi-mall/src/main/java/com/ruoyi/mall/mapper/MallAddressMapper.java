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

    /** 删除某会员的全部地址（账号注销时清理个人数据） */
    int deleteAddressByMemberId(Long memberId);

    int clearDefaultByMemberId(Long memberId);

    MallAddress selectDefaultAddressByMemberId(Long memberId);
}
