package com.ruoyi.mall.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.mall.domain.MallStore;

public interface IMallStoreService
{
    List<MallStore> selectStoreList(MallStore store);

    MallStore selectStoreById(Long storeId);

    int insertStore(MallStore store);

    int updateStore(MallStore store);

    int deleteStoreById(Long storeId);

    /** 对外：所有营业中的门店（按 sort） */
    List<MallStore> listActiveStores();

    /**
     * 就近门店：按经纬度取距离最近的一家营业门店，distanceKm 已填充。
     * lat/lng 为空时返回排序最前的一家（兜底）。无营业门店返回 null。
     */
    MallStore selectNearestStore(BigDecimal lat, BigDecimal lng);

    /** 门店级商品库存覆盖：该店已配的独立库存列表（含商品名）。 */
    List<com.ruoyi.mall.domain.MallProductStock> listStoreStock(Long storeId);

    /** 门店级商品库存覆盖：设置(stock!=null)或清除(stock==null，回退用商户总库存)。 */
    void saveStoreStock(Long productId, Long storeId, Integer stock);
}
