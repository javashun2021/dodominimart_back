package com.ruoyi.web.controller.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.common.config.Global;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * App 文件上传接口（需 JWT）
 * POST /api/v1/upload/image
 */
@RestController
@RequestMapping("/api/v1/upload")
public class ApiUploadController extends BaseApiController
{
    private static final long   MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = new HashSet<>(
            Arrays.asList(".jpg", ".jpeg", ".png", ".webp"));

    @Autowired
    private ServerConfig serverConfig;

    /**
     * 图片上传
     * Content-Type: multipart/form-data
     * 参数名: file
     * 限制: jpg/jpeg/png/webp，最大 5 MB
     */
    @PostMapping("/image")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file,
                                  HttpServletRequest request)
    {
        getCurrentMemberId(request);

        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("请选择要上传的文件");
        }

        if (file.getSize() > MAX_SIZE_BYTES)
        {
            return AjaxResult.error("图片大小不能超过 5MB");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                : "";
        if (!ALLOWED_EXT.contains(ext))
        {
            return AjaxResult.error("仅支持 jpg / jpeg / png / webp 格式");
        }

        try
        {
            String uploadDir = Global.getUploadPath();
            String fileName = FileUploadUtils.upload(uploadDir, file, ext);
            String path = "/profile/upload/" + fileName;
            String url  = serverConfig.getUrl() + path;
            return AjaxResult.success("上传成功")
                    .put("url",  url)
                    .put("path", path);
        }
        catch (Exception e)
        {
            return AjaxResult.error("上传失败，请重试");
        }
    }
}
