package org.tongji.sse.component;

import org.apache.tika.Tika;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.exception.BadRequestException;

import java.io.File;
import java.util.List;

@Component
public class AudioValidator {

    private final Tika tika = new Tika();

    /**
     * 校验：
     * 1. 是否为音频文件
     * 2. 是否为允许的 MIME
     * 3. 是否超出最大时长
     */
    public void validate(File file, int maxDurationSeconds, List<String> allowedMimeTypes) {
        try {
            // MIME 检测
            String mimeType = tika.detect(file);

            boolean valid = allowedMimeTypes.stream().anyMatch(allowed -> {
                if (allowed.endsWith("/*")) {
                    // 支持通配符匹配 audio/*
                    String prefix = allowed.split("/")[0];
                    return mimeType.startsWith(prefix + "/");
                } else {
                    return mimeType.equals(allowed);
                }
            });

            if (!valid) {
                throw new BadRequestException("不支持的音频类型: " + mimeType);
            }
            // 检查时长
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            int duration = header.getTrackLength(); // 秒
            if (duration > maxDurationSeconds) {
                throw new BadRequestException("音频时长不能超过 " + maxDurationSeconds + " 秒");
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("无法解析音频文件，文件可能已损坏");
        }
    }
}
