package com.ruoyi.mall.mapper;

import java.util.List;
import com.ruoyi.mall.domain.MallPosHeldCart;

public interface MallPosHeldCartMapper
{
    /** 插入暂存挂单，回写自增 id */
    int insertHeldCart(MallPosHeldCart cart);

    /** 按 id 查暂存 */
    MallPosHeldCart selectHeldCartById(Long id);

    /** 暂存列表（status=0 暂存中），不限本人——换班/换设备都能调出 */
    List<MallPosHeldCart> selectOpenHeldCarts();

    /** 统计当前暂存中数量（自动序号"暂存#N"用） */
    int countAll();

    /** 更新状态：0暂存中 1已恢复结算 2作废 */
    int updateStatus(MallPosHeldCart cart);
}
