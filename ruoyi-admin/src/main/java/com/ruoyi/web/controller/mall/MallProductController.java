package com.ruoyi.web.controller.mall;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.page.TableDataInfo;
import com.ruoyi.framework.util.ShiroUtils;
import com.ruoyi.framework.web.base.BaseController;
import com.ruoyi.mall.domain.MallCategory;
import com.ruoyi.mall.domain.MallProduct;
import com.ruoyi.mall.service.IMallCategoryService;
import com.ruoyi.mall.service.IMallProductService;
import com.ruoyi.system.service.ISysConfigService;

@Controller
@RequestMapping("/mall/product")
public class MallProductController extends BaseController
{
    private final String prefix = "mall/product";

    @Autowired
    private IMallProductService productService;

    @Autowired
    private IMallCategoryService categoryService;

    @Autowired
    private ISysConfigService configService;

    @RequiresPermissions("mall:product:view")
    @GetMapping
    public String product()
    {
        return prefix + "/product";
    }

    @RequiresPermissions("mall:product:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MallProduct product)
    {
        startPage();
        List<MallProduct> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    @RequiresPermissions("mall:product:add")
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("categories", categoryService.selectCategoryList(new MallCategory()));
        return prefix + "/add";
    }

    @RequiresPermissions("mall:product:add")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MallProduct product)
    {
        product.setCreateBy(ShiroUtils.getLoginName());
        return toAjax(productService.insertProduct(product));
    }

    @RequiresPermissions("mall:product:edit")
    @GetMapping("/edit/{productId}")
    public String edit(@PathVariable Long productId, ModelMap mmap)
    {
        mmap.put("product", productService.selectProductById(productId));
        mmap.put("categories", categoryService.selectCategoryList(new MallCategory()));
        return prefix + "/edit";
    }

    @RequiresPermissions("mall:product:edit")
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MallProduct product)
    {
        product.setUpdateBy(ShiroUtils.getLoginName());
        return toAjax(productService.updateProduct(product));
    }

    @RequiresPermissions("mall:product:remove")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(productService.deleteProductByIds(ids));
    }

    /**
     * 商品标签打印页（方式A：浏览器/PDF 打印不干胶标签）。
     * 每张标签：店名(DODOMINIMART) + 商品名 + ₱价格 + Code128 条码图 + 条码数字。
     * @param ids    商品ID逗号分隔；为空时返回空页
     * @param copies 每个商品份数（默认1）
     */
    @RequiresPermissions("mall:product:view")
    @GetMapping("/label")
    public String label(@RequestParam(required = false) String ids,
                        @RequestParam(defaultValue = "1") int copies,
                        ModelMap mmap)
    {
        if (copies < 1)   copies = 1;
        if (copies > 100) copies = 100;

        String storeName = cfg("app.store.name", "DODOMINIMART");
        List<Map<String, Object>> labels = new ArrayList<>();
        if (ids != null && !ids.trim().isEmpty())
        {
            for (String idStr : ids.split(","))
            {
                Long productId;
                try { productId = Long.valueOf(idStr.trim()); }
                catch (NumberFormatException e) { continue; }
                MallProduct p = productService.selectProductById(productId);
                if (p == null) continue;

                // 没填条码的商品用 P+零填充 兜底，保证标签总有条码
                String code = (p.getBarcode() != null && !p.getBarcode().isEmpty())
                        ? p.getBarcode() : String.format("P%07d", productId);
                String img = barcodeDataUri(code, 360, 90);

                for (int i = 0; i < copies; i++)
                {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name",       p.getName());
                    row.put("price",      p.getPrice());
                    row.put("code",       code);
                    row.put("barcodeImg", img);
                    labels.add(row);
                }
            }
        }
        mmap.put("storeName", storeName);
        mmap.put("labels", labels);
        return prefix + "/label";
    }

    /** 生成 Code128 条码并返回 base64 data URI（内嵌打印页，零驱动） */
    private String barcodeDataUri(String text, int width, int height)
    {
        try
        {
            BitMatrix matrix = new Code128Writer().encode(text, BarcodeFormat.CODE_128, width, height);
            BufferedImage img = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++)
            {
                for (int y = 0; y < matrix.getHeight(); y++)
                {
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        }
        catch (Exception e)
        {
            return "";
        }
    }

    /** 读取系统参数，取不到返回默认值 */
    private String cfg(String key, String defaultValue)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            return (v != null && !v.isEmpty()) ? v : defaultValue;
        }
        catch (Exception e) { return defaultValue; }
    }
}
