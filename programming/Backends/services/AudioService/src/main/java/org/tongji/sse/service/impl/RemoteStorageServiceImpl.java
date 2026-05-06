package org.tongji.sse.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.tongji.sse.service.StorageService;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class RemoteStorageServiceImpl implements StorageService {

    // 资源服务器地址，可在配置中改成动态获取
    private static final String SERVER_URL = "http://101.37.31.227:5000";

    @Override
    public String uploadFile(File file, String objectName) throws Exception {
        String uploadUrl = SERVER_URL + "/upload/" + objectName;

        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);

        // 发送文件数据
        try (OutputStream os = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            StreamUtils.copy(fis, os);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 201) {
            String msg = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("上传失败: " + msg);
        }

        // 返回可访问的文件 URL
        return "/files/" + objectName;
    }

    @Override
    public void deleteFile(String url) throws Exception {
        // 从 URL 中提取 objectName
        if (!url.startsWith(SERVER_URL + "/files/")) {
            throw new IllegalArgumentException("无效文件 URL: " + url);
        }
        String objectName = url.substring((SERVER_URL + "/files/").length());
        String deleteUrl = SERVER_URL + "/delete/" + objectName;

        URL urlObj = new URL(deleteUrl);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("DELETE");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String msg = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("删除失败: " + msg);
        }
    }
}
