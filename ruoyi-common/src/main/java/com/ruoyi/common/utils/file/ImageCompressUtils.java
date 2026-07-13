package com.ruoyi.common.utils.file;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图片压缩工具：所有图片落盘前统一调用，缩小尺寸 + 降低体积，减轻服务器存储与带宽压力。
 *
 * 只处理 JDK ImageIO 原生支持且能安全无损切换的格式（jpg/jpeg/png）；webp/gif 及其它文件一律原样返回，
 * 不改变扩展名。解码失败、压缩后反而更大等任何异常场景都回退到原始字节，保证「绝不损坏、绝不变大」。
 *
 * 纯 JDK 实现，无第三方依赖。
 */
public class ImageCompressUtils
{
    private static final Logger log = LoggerFactory.getLogger(ImageCompressUtils.class);

    /** 最长边上限（像素），超过则等比缩小 */
    private static final int MAX_DIMENSION = 1920;

    /** JPEG 重新编码质量（0~1） */
    private static final float JPEG_QUALITY = 0.82f;

    /** 可压缩的图片扩展名（其余格式原样返回） */
    private static final Set<String> COMPRESSIBLE = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));

    /** 该扩展名是否会被压缩（供调用方决定是否走压缩分支） */
    public static boolean isCompressible(String ext)
    {
        return COMPRESSIBLE.contains(normalize(ext));
    }

    /**
     * 压缩图片字节。非图片 / 无法解码 / 压缩后更大 → 返回原始字节（永不返回 null、永不变大）。
     *
     * @param data 原始文件字节
     * @param ext  扩展名（带不带点均可，如 "jpg" / ".JPG"）
     */
    public static byte[] compress(byte[] data, String ext)
    {
        if (data == null || data.length == 0) return data;
        String fmt = normalize(ext);
        if (!COMPRESSIBLE.contains(fmt)) return data;
        try
        {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            if (src == null) return data;   // 扩展名像图片但内容不是 → 原样

            BufferedImage scaled = scaleDown(src, MAX_DIMENSION);
            byte[] out = "png".equals(fmt) ? encodePng(scaled) : encodeJpeg(scaled, JPEG_QUALITY);
            // 压缩失败或没有变小就用原始（绝不变大）
            if (out == null || out.length == 0 || out.length >= data.length) return data;
            return out;
        }
        catch (Exception e)
        {
            log.warn("image compress failed, keep original ({} bytes): {}", data.length, e.getMessage());
            return data;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** 等比缩小到最长边不超过 max；本就不超过则原图返回 */
    private static BufferedImage scaleDown(BufferedImage src, int max)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        if (longest <= max) return src;

        double ratio = (double) max / longest;
        int nw = Math.max(1, (int) Math.round(w * ratio));
        int nh = Math.max(1, (int) Math.round(h * ratio));

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    /** JPEG 编码（含质量控制）。JPEG 不支持透明，先铺白底再压 */
    private static byte[] encodeJpeg(BufferedImage img, float quality) throws Exception
    {
        BufferedImage rgb = img;
        if (img.getColorModel().hasAlpha())
        {
            rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }

        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
        if (!it.hasNext()) return null;
        ImageWriter writer = it.next();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(bos))
        {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed())
            {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.flush();
            return bos.toByteArray();
        }
        finally
        {
            writer.dispose();
        }
    }

    /** PNG 无损重新编码（体积收益主要来自尺寸缩小） */
    private static byte[] encodePng(BufferedImage img) throws Exception
    {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        }
    }

    /** 扩展名归一化：去点、小写、jpeg→jpg 不做合并（jpg/jpeg 都在集合里） */
    private static String normalize(String ext)
    {
        if (ext == null) return "";
        String e = ext.trim().toLowerCase();
        if (e.startsWith(".")) e = e.substring(1);
        return e;
    }
}
