package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.dto.AudioResponseDto;
import org.tongji.sse.dto.PublishAudioRequest;
import org.tongji.sse.dto.UpdateAudioRequest;
import org.tongji.sse.dto.UploadAudioRequest;

import java.util.List;

public interface AudioService {
    AudioResponseDto upload(HttpServletRequest request, MultipartFile file, UploadAudioRequest req);

    AudioResponseDto publish(HttpServletRequest request, Long audioId, PublishAudioRequest req);

    AudioResponseDto hide(HttpServletRequest request, Long audioId, boolean hidden);

    void delete(HttpServletRequest request, Long audioId);

    AudioResponseDto get(HttpServletRequest request, Long audioId);

    List<AudioResponseDto> getMulti(HttpServletRequest request, List<Long> ids);

    List<AudioResponseDto> getNearbyAudio(Double latitude, Double longitude, Double distance);

    List<AudioResponseDto> search(HttpServletRequest request, String query);

    List<AudioResponseDto> getRecommendation(int limit);

    String uploadPhoto(HttpServletRequest request, MultipartFile file);

    List<AudioResponseDto> getAllUserAudios(HttpServletRequest request, Long userId);

    AudioResponseDto updateAudio(HttpServletRequest request, Long id, UpdateAudioRequest req);
}
