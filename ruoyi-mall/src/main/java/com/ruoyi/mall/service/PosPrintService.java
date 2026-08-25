package com.ruoyi.mall.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallOrder;
import com.ruoyi.mall.domain.MallOrderItem;

/**
 * POS 小票打印：按 ESC/POS 生成小票字节流 → Socket 写到网口热敏打印机(ip:9100)。
 * 打印机到货前可把 cfg.enabled 置 false，仅把小票内容打到日志做打桩。
 *
 * 注：ruoyi-mall 不依赖 ruoyi-system，无法读取 sys_config；
 * 打印参数由 admin 层(ApiCashierController)从 sys_config 读出后通过 {@link ReceiptConfig} 传入。
 */
@Service
public class PosPrintService
{
    private static final Logger log = LoggerFactory.getLogger(PosPrintService.class);

    // 80mm 热敏纸常见 42 字符宽（Font A）。金额用全角 "￥" 前缀（GBK 可编码，半角 ¥ 在 GBK 会变 ?）。
    private static final int LINE_WIDTH = 42;

    /** 打印参数（由 admin 层从 sys_config 组装传入） */
    public static class ReceiptConfig
    {
        public boolean enabled;
        public String  ip;
        public int     port = 9100;
        public String  storeName = "DODOMINIMART";
        public String  address = "";
        public String  phone = "";
        public String  footer = "Thank you for shopping!";
    }

    /**
     * 打印订单小票。cfg.enabled=false 时仅写日志(打桩)，不连真机。
     * @throws RuntimeException 真机打印失败时抛出，供补打接口反馈；自动打印场景调用方应自行 catch。
     */
    public void printReceipt(MallOrder order, ReceiptConfig cfg)
    {
        if (cfg == null) cfg = new ReceiptConfig();
        if (!cfg.enabled)
        {
            log.info("[POS-PRINT stub] enabled=false, receipt for order {}:\n{}",
                    order.getOrderNo(), buildPlainText(order, cfg));
            return;
        }
        if (cfg.ip == null || cfg.ip.isEmpty())
        {
            log.warn("[POS-PRINT] printer ip not configured; skip printing order {}", order.getOrderNo());
            throw new RuntimeException("打印机 IP 未配置");
        }

        byte[] data = buildEscPos(order, cfg);
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(cfg.ip, cfg.port), 3000);
            try (OutputStream out = socket.getOutputStream())
            {
                out.write(data);
                out.flush();
            }
            log.info("[POS-PRINT] printed order {} to {}:{}", order.getOrderNo(), cfg.ip, cfg.port);
        }
        catch (IOException e)
        {
            log.error("[POS-PRINT] failed to print order {} to {}:{} — {}", order.getOrderNo(), cfg.ip, cfg.port, e.getMessage());
            throw new RuntimeException("打印失败:" + e.getMessage());
        }
    }

    // ── ESC/POS 字节流 ────────────────────────────────────────────────

    private byte[] buildEscPos(MallOrder order, ReceiptConfig cfg)
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try
        {
            b.write(new byte[]{0x1B, 0x40});          // ESC @ 初始化

            // 抬头（居中 + 加粗放大）
            b.write(new byte[]{0x1B, 0x61, 0x01});    // ESC a 1 居中
            b.write(new byte[]{0x1D, 0x21, 0x11});    // GS ! 倍宽倍高
            writeLine(b, cfg.storeName);
            b.write(new byte[]{0x1D, 0x21, 0x00});    // GS ! 复位字号
            if (cfg.address != null && !cfg.address.isEmpty()) writeLine(b, cfg.address);
            if (cfg.phone != null && !cfg.phone.isEmpty())     writeLine(b, "Tel: " + cfg.phone);

            // 正文（左对齐）
            b.write(new byte[]{0x1B, 0x61, 0x00});    // ESC a 0 左对齐
            writeLine(b, line('-'));
            writeLine(b, "Order: " + nz(order.getOrderNo()));
            writeLine(b, "Time : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(order.getCreateTime() != null ? order.getCreateTime() : new java.util.Date()));
            writeLine(b, line('-'));

            // 明细：名称行 + "qty x price        subtotal" 行
            if (order.getItems() != null)
            {
                for (MallOrderItem it : order.getItems())
                {
                    writeLine(b, nz(it.getProductName()));
                    String left  = "  " + it.getQuantity() + " x ￥" + money(it.getPrice());
                    String right = "￥" + money(it.getSubtotal());
                    writeLine(b, pad(left, right));
                }
            }
            writeLine(b, line('-'));

            // 汇总
            if (order.getPointsUsed() > 0)
            {
                writeLine(b, pad("Points Used (" + order.getPointsUsed() + ")",
                        "-￥" + money(BigDecimal.valueOf(order.getPointsUsed() / 10L))));
            }
            if (order.getCouponDiscount() != null && order.getCouponDiscount().signum() > 0)
            {
                writeLine(b, pad("Coupon", "-￥" + money(order.getCouponDiscount())));
            }
            b.write(new byte[]{0x1B, 0x45, 0x01});    // ESC E 1 加粗
            writeLine(b, pad("TOTAL", "￥" + money(order.getTotalAmount())));
            b.write(new byte[]{0x1B, 0x45, 0x00});    // 取消加粗
            if (order.getCashReceived() != null)
            {
                writeLine(b, pad("Cash", "￥" + money(order.getCashReceived())));
            }
            if (order.getChangeDue() != null)
            {
                writeLine(b, pad("Change", "￥" + money(order.getChangeDue())));
            }
            writeLine(b, line('-'));

            // 页脚（居中）
            b.write(new byte[]{0x1B, 0x61, 0x01});
            writeLine(b, nz(cfg.footer));

            // 走纸 + 切纸
            b.write(new byte[]{0x0A, 0x0A, 0x0A});
            b.write(new byte[]{0x1D, 0x56, 0x00});    // GS V 0 全切
        }
        catch (IOException ignored) { }
        return b.toByteArray();
    }

    private void writeLine(ByteArrayOutputStream b, String text) throws IOException
    {
        b.write(bytes(text));
        b.write(0x0A); // LF
    }

    /** 把文本按打印机字库编码；GBK 兼容中英文，取不到则退回平台默认 */
    private byte[] bytes(String s)
    {
        if (s == null) s = "";
        try { return s.getBytes(Charset.forName("GBK")); }
        catch (Exception e) { return s.getBytes(); }
    }

    // ── 纯文本（日志打桩用，与小票排版一致） ──────────────────────────

    private String buildPlainText(MallOrder order, ReceiptConfig cfg)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(nz(cfg.storeName)).append('\n');
        sb.append("Order: ").append(nz(order.getOrderNo())).append('\n');
        if (order.getItems() != null)
        {
            for (MallOrderItem it : order.getItems())
            {
                sb.append(nz(it.getProductName())).append('\n');
                sb.append(pad("  " + it.getQuantity() + " x ￥" + money(it.getPrice()),
                        "￥" + money(it.getSubtotal()))).append('\n');
            }
        }
        sb.append(pad("TOTAL", "￥" + money(order.getTotalAmount()))).append('\n');
        if (order.getCashReceived() != null) sb.append(pad("Cash", "￥" + money(order.getCashReceived()))).append('\n');
        if (order.getChangeDue() != null)    sb.append(pad("Change", "￥" + money(order.getChangeDue()))).append('\n');
        return sb.toString();
    }

    // ── 小工具 ───────────────────────────────────────────────────────

    private String pad(String left, String right)
    {
        int space = LINE_WIDTH - left.length() - right.length();
        if (space < 1) space = 1;
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < space; i++) sb.append(' ');
        sb.append(right);
        return sb.toString();
    }

    private String line(char c)
    {
        char[] arr = new char[LINE_WIDTH];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }

    private String money(BigDecimal v)
    {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String nz(String s) { return s != null ? s : ""; }
}
