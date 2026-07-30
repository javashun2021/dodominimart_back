package com.ruoyi.mall.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.mall.domain.MallProductStock;

/**
 * 门店级商品库存覆盖 Mapper。
 * 语义：无行=用商户总库存；有行=门店优先用此库存。
 */
public interface MallProductStockMapper
{
    /** 取门店对某商品的独立库存；无覆盖返回 null（调用方回退商户总库存）。 */
    Integer selectStock(@Param("productId") Long productId, @Param("storeId") Long storeId);

    /** 是否存在覆盖行（0/1），用于决定扣哪个池。 */
    int countStock(@Param("productId") Long productId, @Param("storeId") Long storeId);

    /** 原子扣减门店独立库存（stock>=quantity 才成功），返回受影响行数。 */
    int deductStock(@Param("productId") Long productId, @Param("storeId") Long storeId,
                    @Param("quantity") int quantity);

    /** 回补门店独立库存（取消/退款）。 */
    int restoreStock(@Param("productId") Long productId, @Param("storeId") Long storeId,
                     @Param("quantity") int quantity);

    /** 后台设置门店独立库存（存在则更新，否则插入）。 */
    int upsertStock(MallProductStock ps);

    /** 后台清除门店独立库存（该商品该店回退用总库存）。 */
    int deleteStock(@Param("productId") Long productId, @Param("storeId") Long storeId);

    /** 某门店已配的所有独立库存（后台按店配库存页）。 */
    List<MallProductStock> selectByStore(@Param("storeId") Long storeId);
}
