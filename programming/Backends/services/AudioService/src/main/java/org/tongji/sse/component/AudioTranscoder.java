package org.tongji.sse.component;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Component
public class AudioTranscoder {

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    public AudioTranscoder() throws IOException {
        // 确保 ffmpeg.exe 和 ffprobe.exe 在系统 PATH 下
        this.ffmpeg = new FFmpeg("ffmpeg");
        this.ffprobe = new FFprobe("ffprobe");
    }

    /**
     * 将任意音频文件转码为 WAV，返回转码后的 File
     */
    public File transcodeToWav(File inputFile) throws IOException {
        // 1️⃣ 创建临时输出 WAV 文件
        File tempOutput = File.createTempFile("output-", ".wav");

        // 2️⃣ 使用 ffmpeg 转码
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile.getAbsolutePath())
                .addOutput(tempOutput.getAbsolutePath())
                .setFormat("wav")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        // 3️⃣ 返回输出文件，调用方负责上传和删除
        return tempOutput;
    }
}
