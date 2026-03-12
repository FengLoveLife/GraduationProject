package com.saul.controller;

import com.saul.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 通用上传控制器
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    // 物理存储路径（对应本地磁盘）
    private static final String UPLOAD_PATH = "D:/GraduationProject/Img/";
    
    // 网络访问前缀（对应 WebMvcConfig 中的虚拟路径映射）
    private static final String ACCESS_URL_PREFIX = "http://localhost:8999/images/";

    /**
     * 通用文件上传接口
     * 逻辑：接收文件 -> 校验 -> UUID重命名 -> 保存本地 -> 返回可访问URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 1. 空文件校验
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 2. 提取后缀名
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 3. UUID 重命名：彻底杜绝重名覆盖，解决浏览器缓存问题
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        try {
            // 4. 确保物理目录存在
            File dir = new File(UPLOAD_PATH);
            if (!dir.exists()) {
                dir.mkdirs(); // 自动创建 D:/GraduationProject/Img/ 目录
            }

            // 5. 保存文件到本地磁盘
            File dest = new File(dir, newFileName);
            file.transferTo(dest);

            // 6. 拼接并返回完整网络访问 URL
            // 映射关系：本地 D:/GraduationProject/Img/xxx.jpg -> 网络 http://localhost:8999/images/xxx.jpg
            String finalUrl = ACCESS_URL_PREFIX + newFileName;
            
            return Result.success("文件上传成功", finalUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
