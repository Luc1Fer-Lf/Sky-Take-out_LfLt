package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    //使用阿里云的OSS进行文件上传
    @RequestMapping("/upload")
    public Result upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("文件上传：{}", originalFilename);
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String url;
        try {
            String fileName = UUID.randomUUID().toString() + suffix;
            url = aliOssUtil.upload(file.getBytes(), fileName);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
            return Result.error("上传失败");
        }
        return Result.success(url);
    }
}
