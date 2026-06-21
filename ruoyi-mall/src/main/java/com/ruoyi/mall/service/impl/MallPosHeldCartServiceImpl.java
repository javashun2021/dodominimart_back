package com.ruoyi.mall.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallPosHeldCart;
import com.ruoyi.mall.mapper.MallPosHeldCartMapper;
import com.ruoyi.mall.service.IMallPosHeldCartService;

@Service
public class MallPosHeldCartServiceImpl implements IMallPosHeldCartService
{
    @Autowired
    private MallPosHeldCartMapper heldCartMapper;

    @Override
    public MallPosHeldCart hold(MallPosHeldCart cart)
    {
        if (cart.getLabel() == null || cart.getLabel().trim().isEmpty())
        {
            // 自动序号"暂存#N"：N = 历史暂存总数 + 1（仅展示用，不要求严格连续）
            cart.setLabel("Hold #" + (heldCartMapper.countAll() + 1));
        }
        if (cart.getItemCount() == null) cart.setItemCount(0);
        cart.setStatus("0");
        cart.setCreateTime(new Date());
        heldCartMapper.insertHeldCart(cart);
        return cart;
    }

    @Override
    public List<MallPosHeldCart> listOpen()
    {
        return heldCartMapper.selectOpenHeldCarts();
    }

    @Override
    public MallPosHeldCart get(Long id)
    {
        return heldCartMapper.selectHeldCartById(id);
    }

    @Override
    public int markResolved(Long id)
    {
        return updateStatus(id, "1");
    }

    @Override
    public int voidCart(Long id)
    {
        return updateStatus(id, "2");
    }

    private int updateStatus(Long id, String status)
    {
        MallPosHeldCart c = new MallPosHeldCart();
        c.setId(id);
        c.setStatus(status);
        c.setUpdateTime(new Date());
        return heldCartMapper.updateStatus(c);
    }
}
