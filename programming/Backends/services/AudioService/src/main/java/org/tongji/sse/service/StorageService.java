package org.tongji.sse.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface StorageService {
    String uploadFile(File file, String objectName) throws Exception;
    void deleteFile(String url) throws Exception;
}