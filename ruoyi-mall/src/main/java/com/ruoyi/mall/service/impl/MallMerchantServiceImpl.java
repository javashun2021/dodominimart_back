package com.ruoyi.mall.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ImageUrlUtils;
import com.ruoyi.mall.domain.MallMerchant;
import com.ruoyi.mall.mapper.MallMerchantMapper;
import com.ruoyi.mall.service.IMallMerchantService;

/**
 * 商家/门店（入驻）服务。
 * 审核状态机照抄跑腿申请(0待审/1通过/2拒绝)，附近排序照搬门店 Haversine。
 */
@Service
public class MallMerchantServiceImpl implements IMallMerchantService
{
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Autowired
    private MallMerchantMapper merchantMapper;

    @Override
    public List<MallMerchant> selectMerchantList(MallMerchant merchant)
    {
        return merchantMapper.selectMerchantList(merchant);
    }

    @Override
    public MallMerchant selectMerchantById(Long merchantId)
    {
        return merchantMapper.selectMerchantById(merchantId);
    }

    @Override
    public int insertMerchant(MallMerchant merchant)
    {
        normalizeImages(merchant);
        merchant.setCreateTime(DateUtils.getNowDate());
        return merchantMapper.insertMerchant(merchant);
    }

    @Override
    public int updateMerchant(MallMerchant merchant)
    {
        normalizeImages(merchant);
        merchant.setUpdateTime(DateUtils.getNowDate());
        return merchantMapper.updateMerchant(merchant);
    }

    /** 入库前把 logoUrl/images 归一化成相对路径，避免存成内网绝对地址。 */
    private void normalizeImages(MallMerchant merchant)
    {
        if (merchant == null) return;
        merchant.setLogoUrl(ImageUrlUtils.toRelative(merchant.getLogoUrl()));
        merchant.setImages(ImageUrlUtils.toRelative(merchant.getImages()));
    }

    @Override
    public int deleteMerchantById(Long merchantId)
    {
        return merchantMapper.deleteMerchantById(merchantId);
    }

    @Override
    public void approveMerchant(Long merchantId, String reviewer)
    {
        MallMerchant m = new MallMerchant();
        m.setMerchantId(merchantId);
        m.setStatus("1");
        m.setRejectReason(null);
        m.setReviewTime(new Date());
        m.setReviewer(reviewer);
        merchantMapper.updateReview(m);
    }

    @Override
    public void rejectMerchant(Long merchantId, String reviewer, String rejectReason)
    {
        MallMerchant m = new MallMerchant();
        m.setMerchantId(merchantId);
        m.setStatus("2");
        m.setRejectReason(rejectReason);
        m.setReviewTime(new Date());
        m.setReviewer(reviewer);
        merchantMapper.updateReview(m);
    }

    @Override
    public MallMerchant getMyMerchant(Long ownerMemberId)
    {
        return merchantMapper.selectByOwner(ownerMemberId);
    }

    @Override
    public MallMerchant applyMerchant(MallMerchant draft, Long ownerMemberId)
    {
        if (draft == null || draft.getName() == null || draft.getName().trim().isEmpty())
        {
            throw new RuntimeException("Store name is required");
        }
        MallMerchant existing = merchantMapper.selectByOwner(ownerMemberId);
        if (existing != null && "1".equals(existing.getStatus()))
        {
            throw new RuntimeException("You already have an active store");
        }
        // 服务端强制敏感字段，忽略客户端传入的 status/owner/promoter
        draft.setOwnerMemberId(ownerMemberId);
        draft.setPromoterId(null);
        draft.setStatus("0");        // 待审核
        draft.setDelFlag("0");
        if (existing == null)
        {
            draft.setMerchantId(null);
            draft.setCreateBy("member:" + ownerMemberId);
            insertMerchant(draft);
        }
        else
        {
            // 编辑待审(0) / 被拒(2)或停业(3)后重新提交
            draft.setMerchantId(existing.getMerchantId());
            draft.setUpdateBy("member:" + ownerMemberId);
            draft.setRejectReason("");   // 清空上次拒绝原因
            updateMerchant(draft);
        }
        return merchantMapper.selectByOwner(ownerMemberId);
    }

    @Override
    public List<MallMerchant> selectByPromoter(Long promoterId)
    {
        return merchantMapper.selectByPromoter(promoterId);
    }

    @Override
    public int countByPromoter(Long promoterId)
    {
        return merchantMapper.countByPromoter(promoterId);
    }

    @Override
    public List<MallMerchant> selectNearby(BigDecimal lat, BigDecimal lng, String category, String keyword)
    {
        List<MallMerchant> all = merchantMapper.selectActiveMerchants();
        List<MallMerchant> filtered = new ArrayList<>();
        for (MallMerchant m : all)
        {
            if (category != null && !category.isEmpty() && !category.equals(m.getCategory())) continue;
            if (keyword != null && !keyword.isEmpty())
            {
                String name = m.getName() == null ? "" : m.getName().toLowerCase();
                if (!name.contains(keyword.toLowerCase())) continue;
            }
            filtered.add(m);
        }
        // 无坐标：不算距离，按原 sort 顺序返回
        if (lat == null || lng == null)
        {
            return filtered;
        }
        double userLat = lat.doubleValue();
        double userLng = lng.doubleValue();
        for (MallMerchant m : filtered)
        {
            if (m.getLat() != null && m.getLng() != null)
            {
                double d = haversineKm(userLat, userLng, m.getLat().doubleValue(), m.getLng().doubleValue());
                m.setDistanceKm(BigDecimal.valueOf(Math.round(d * 100.0) / 100.0));
            }
        }
        // 有坐标的按距离升序，无坐标的排最后
        filtered.sort((a, b) -> {
            BigDecimal da = a.getDistanceKm();
            BigDecimal db = b.getDistanceKm();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });
        return filtered;
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
