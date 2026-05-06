package org.tongji.sse.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.service.StorageService;


import java.io.File;
import java.nio.file.*;

// @Service
public class LocalStorageServiceImpl  {

    private final Path root = Paths.get("storage");

    public LocalStorageServiceImpl() throws Exception {
        Files.createDirectories(root);
    }

    // @Override
    public String uploadFile(File file, String objectName) throws Exception {
        Path target = root.resolve(objectName);

        // 确保父目录存在
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        // 使用 Files.copy 直接从 File 的 Path 复制
        Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/files/" + objectName;
    }

    // @Override
    public void deleteFile(String url) throws Exception {
        // 可根据实际 URL 解析文件路径，这里留空即可
    }
}
