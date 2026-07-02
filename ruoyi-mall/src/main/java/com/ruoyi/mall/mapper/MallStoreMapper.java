package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallStore;

public interface MallStoreMapper
{
    List<MallStore> selectStoreList(MallStore store);

    MallStore selectStoreById(Long storeId);

    /** 对外接口用：仅营业中、未删除的门店 */
    List<MallStore> selectActiveStores();

    int insertStore(MallStore store);

    int updateStore(MallStore store);

    /** 逻辑删除 */
    int deleteStoreById(Long storeId);
}
