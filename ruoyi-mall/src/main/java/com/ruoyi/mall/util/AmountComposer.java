package com.ruoyi.mall.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 金额撮合：用商品单价(分)凑出目标金额(分)，最多 maxItems 件（含数量，同商品可重复）。
 *
 * 无界背包最少件数 DP：dp[a]=凑到 a 分的最少件数；命中 dp[target]≤maxItems 即成功，回溯出商品+数量。
 * 相同价格只保留一个代表商品（调用方应把库存高者排前，靠 putIfAbsent 保留）。
 */
public final class AmountComposer
{
    private AmountComposer() {}

    /** 撮合上限：目标金额超过 2000 元(200000 分)直接放弃（DP 规模护栏） */
    public static final int MAX_TARGET_CENTS = 200000;

    public static final class Pick
    {
        public final Long productId;
        public final int quantity;
        public Pick(Long productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    }

    /**
     * @param priceByProduct 候选商品 productId -> 单价(分)（已按 status/库存过滤）
     * @param targetCents    目标金额(分)
     * @param maxItems       最多件数（含数量）
     * @return 命中返回 [productId,数量] 列表；凑不出返回 null
     */
    public static List<Pick> compose(Map<Long, Integer> priceByProduct, long targetCents, int maxItems)
    {
        if (targetCents <= 0 || targetCents > MAX_TARGET_CENTS || maxItems <= 0) return null;
        if (priceByProduct == null || priceByProduct.isEmpty()) return null;

        int T = (int) targetCents;

        // 去重价格 -> 代表商品（保留先出现的，调用方按库存降序传入）
        Map<Integer, Long> priceToProduct = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> e : priceByProduct.entrySet())
        {
            Integer c = e.getValue();
            if (c != null && c > 0 && c <= T) priceToProduct.putIfAbsent(c, e.getKey());
        }
        if (priceToProduct.isEmpty()) return null;

        final int INF = Integer.MAX_VALUE;
        int[] dp = new int[T + 1];
        int[] from = new int[T + 1]; // 到达 a 时最后使用的价格(分)
        Arrays.fill(dp, INF);
        dp[0] = 0;
        for (int a = 1; a <= T; a++)
        {
            for (Integer c : priceToProduct.keySet())
            {
                if (c <= a && dp[a - c] != INF && dp[a - c] + 1 < dp[a])
                {
                    dp[a] = dp[a - c] + 1;
                    from[a] = c;
                }
            }
        }
        if (dp[T] == INF || dp[T] > maxItems) return null;

        Map<Integer, Integer> countByPrice = new HashMap<>();
        int a = T;
        while (a > 0)
        {
            int c = from[a];
            countByPrice.merge(c, 1, Integer::sum);
            a -= c;
        }
        List<Pick> picks = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : countByPrice.entrySet())
        {
            picks.add(new Pick(priceToProduct.get(e.getKey()), e.getValue()));
        }
        return picks;
    }
}
