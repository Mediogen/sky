package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传接口
     * @param file 上传的文件
     * @return Result封装文件的访问地址
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result upload(MultipartFile file) {
        // 这里可以添加文件上传的逻辑
        try {
            //截取文件名的后缀
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return Result.error("文件名不能为空");
            }
            String fileExtension = fileName.substring(fileName.lastIndexOf("."));
            // 生成新的文件名，使用UUID避免重复
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            String url = aliOssUtil.upload(file.getBytes(), newFileName);
            log.info("文件上传成功，文件名：{}，访问地址：{}", newFileName, url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}
