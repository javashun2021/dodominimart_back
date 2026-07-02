package com.ruoyi.mall.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.base.BaseEntity;

/**
 * 门店/发货网点表 mall_store
 *
 * 模型 C：商品/库存/价格全局共享，不分店；门店只作为订单归属、骑手归属、自提点/发货网点。
 */
public class MallStore extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long storeId;

    @Excel(name = "门店名称")
    private String name;

    @Excel(name = "地址")
    private String address;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    @Excel(name = "电话")
    private String phone;

    /** 配送/覆盖半径(km) */
    private BigDecimal serviceRadiusKm;

    /** 营业时间，如 08:00-22:00 */
    private String businessHours;

    /** 状态（0营业 1停业） */
    @Excel(name = "状态", readConverterExp = "0=营业,1=停业")
    private String status;

    private Integer sort;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 非DB：距用户距离(km)，仅 nearest 接口填充 */
    private BigDecimal distanceKm;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }

    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getServiceRadiusKm() { return serviceRadiusKm; }
    public void setServiceRadiusKm(BigDecimal serviceRadiusKm) { this.serviceRadiusKm = serviceRadiusKm; }

    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
}
