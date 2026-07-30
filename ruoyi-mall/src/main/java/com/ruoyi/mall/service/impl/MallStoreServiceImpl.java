package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.mall.domain.MallStore;
import com.ruoyi.mall.domain.MallProductStock;
import com.ruoyi.mall.mapper.MallStoreMapper;
import com.ruoyi.mall.mapper.MallProductStockMapper;
import com.ruoyi.mall.service.IMallStoreService;

@Service
public class MallStoreServiceImpl implements IMallStoreService
{
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Autowired
    private MallStoreMapper storeMapper;

    @Autowired
    private MallProductStockMapper productStockMapper;

    @Override
    public List<MallStore> selectStoreList(MallStore store)
    {
        return storeMapper.selectStoreList(store);
    }

    @Override
    public MallStore selectStoreById(Long storeId)
    {
        return storeMapper.selectStoreById(storeId);
    }

    @Override
    public int insertStore(MallStore store)
    {
        store.setCreateTime(DateUtils.getNowDate());
        return storeMapper.insertStore(store);
    }

    @Override
    public int updateStore(MallStore store)
    {
        store.setUpdateTime(DateUtils.getNowDate());
        return storeMapper.updateStore(store);
    }

    @Override
    public int deleteStoreById(Long storeId)
    {
        return storeMapper.deleteStoreById(storeId);
    }

    @Override
    public List<MallStore> listActiveStores()
    {
        return storeMapper.selectActiveStores();
    }

    @Override
    public MallStore selectNearestStore(BigDecimal lat, BigDecimal lng)
    {
        List<MallStore> stores = storeMapper.selectActiveStores();
        if (stores == null || stores.isEmpty())
        {
            return null;
        }
        // 无坐标：返回排序最前一家兜底
        if (lat == null || lng == null)
        {
            return stores.get(0);
        }
        double userLat = lat.doubleValue();
        double userLng = lng.doubleValue();

        MallStore nearest = null;
        double best = Double.MAX_VALUE;
        for (MallStore s : stores)
        {
            if (s.getLat() == null || s.getLng() == null)
            {
                continue;
            }
            double d = haversineKm(userLat, userLng, s.getLat().doubleValue(), s.getLng().doubleValue());
            if (d < best)
            {
                best = d;
                nearest = s;
            }
        }
        // 全部门店都没配坐标：兜底第一家
        if (nearest == null)
        {
            return stores.get(0);
        }
        nearest.setDistanceKm(BigDecimal.valueOf(Math.round(best * 100.0) / 100.0));
        return nearest;
    }

    @Override
    public List<MallProductStock> listStoreStock(Long storeId)
    {
        return productStockMapper.selectByStore(storeId);
    }

    @Override
    public void saveStoreStock(Long productId, Long storeId, Integer stock)
    {
        if (stock == null)
        {
            productStockMapper.deleteStock(productId, storeId);
        }
        else
        {
            MallProductStock ps = new MallProductStock();
            ps.setProductId(productId);
            ps.setStoreId(storeId);
            ps.setStock(stock);
            productStockMapper.upsertStock(ps);
        }
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2)
    {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
