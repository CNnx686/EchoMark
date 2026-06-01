package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.component.AudioTranscoder;
import org.tongji.sse.config.AudioUploadProperties;
import org.tongji.sse.entity.Audio;
import org.tongji.sse.entity.User;
import org.tongji.sse.eventUtil.EventPublisher;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.repository.AudioRepository;
import org.tongji.sse.repository.UserRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.AudioService;
import org.tongji.sse.service.StorageService;
import org.tongji.sse.dto.*;
import org.tongji.sse.exception.*;
import org.tongji.sse.component.AudioValidator;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.huaban.analysis.jieba.WordDictionary;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.tongji.sse.type.BehaviorTargetType;
import org.tongji.sse.type.BehaviorType;

import java.util.*;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final AudioRepository repo;
    private final StorageService storage;
    private final UserRepository userRepo;
    private final AudioValidator audioValidator;
    private final AudioUploadProperties audioUploadProperties;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AudioTranscoder audioTranscoder;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JiebaSegmenter segmenter = new JiebaSegmenter();
    private final EventPublisher publisher;

    /**
     * 初始化jieba分词，加载用户自定义词典
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("user.dict");
            if (resource.exists()) {
                Path tempFile = Files.createTempFile("jieba_user_dict", ".txt");
                Files.copy(resource.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                WordDictionary.getInstance().loadUserDict(tempFile);
                tempFile.toFile().deleteOnExit();
            }
        } catch (Exception e) {
            System.err.println("Failed to load jieba user dictionary: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AudioResponseDto upload(HttpServletRequest request, MultipartFile file, UploadAudioRequest req) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("音频文件不能为空");
        }

        try {
            User user = userRepo.findById(uid)
                    .orElseThrow(() -> new NotFoundException("用户不存在"));

            // 写入临时文件
            File tempFile;

            tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // 音频合法性校验
            audioValidator.validate(
                    tempFile,
                    audioUploadProperties.getMaxDurationSeconds(),
                    audioUploadProperties.getAllowedMimeTypes()
            );


            // 转码为 WAV (若 FFmpeg 不可用则跳过)
            File wavFile;
            if (audioTranscoder != null) {
                wavFile = audioTranscoder.transcodeToWav(tempFile);
            } else {
                wavFile = tempFile;
            }

            // 上传
            String objectName = "audio/" + uid + "/" + System.currentTimeMillis() + "-" + wavFile.getName();
            String audioUrl = storage.uploadFile(wavFile, objectName);

            Audio audio = Audio.builder()
                    .userId(uid)
                    .audioUrl(audioUrl)
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .uploadTime(Instant.now())
                    .status("UPLOADED")
                    .deleted(false)
                    .user(user)
                    .build();

            // 删除临时文件
            wavFile.delete();
            tempFile.delete();

            return toDto(repo.save(audio));

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AudioResponseDto publish(HttpServletRequest request, Long audioId, PublishAudioRequest req) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);

        Audio audio = repo.findById(audioId).orElseThrow(() -> new NotFoundException("音频不存在"));

        if(!"UPLOADED".equals(audio.getStatus()))
            throw new BadRequestException("音频已发布或不可发布");
        if(!userRepo.existsById(uid))
            throw new NotFoundException("用户不存在");
        if (!audio.getUserId().equals(uid))
            throw new AccessDeniedException("不能发布他人的音频");

        audio.setTitle(req.getTitle());
        audio.setDescription(req.getDescription());
        audio.setTags(req.getTags());
        audio.setPhotoUrl(req.getPhotoUrl());
        audio.setPublishTime(Instant.now());
        audio.setStatus(req.getIsPublic() ? "PUBLISHED" : "HIDDEN");
        audio.setVisitCount(0L);

        UserBehaviorSignalEvent event = UserBehaviorSignalEvent.builder()
                .behaviorType(BehaviorType.PUBLISH)
                .userId(uid)
                .behaviorTargetType(BehaviorTargetType.AUDIO)
                .tags(req.getTags() == null ? new ArrayList<>() : new ArrayList<>(req.getTags()))
                .authorId(audio.getUserId())
                .textContent(req.getDescription())
                .build();

        publisher.register(event, EventChannelEnum.BEHAVIOR_PUSH);

        return toDto(repo.save(audio));
    }

    @Override
    @Transactional
    public AudioResponseDto hide(HttpServletRequest request, Long audioId, boolean hidden) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);

        Audio audio = repo.findById(audioId).orElseThrow(() -> new NotFoundException("音频不存在"));

        if (!audio.getUserId().equals(uid))
            throw new AccessDeniedException("不能操作他人的音频");

        audio.setStatus(hidden ? "HIDDEN" : "PUBLISHED");
        return toDto(repo.save(audio));
    }

    @Override
    @Transactional
    public void delete(HttpServletRequest request, Long audioId) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);

        Audio audio = repo.findById(audioId).orElseThrow(() -> new NotFoundException("音频不存在"));

        if (!audio.getUserId().equals(uid))
            throw new AccessDeniedException("不能删除他人的音频");

        audio.setDeleted(true);
        repo.save(audio);

        try {
            storage.deleteFile(audio.getAudioUrl());
            if (audio.getPhotoUrl() != null)
                storage.deleteFile(audio.getPhotoUrl());
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public AudioResponseDto get(HttpServletRequest request, Long audioId) {
        Audio audio = repo.findById(audioId).orElseThrow(() -> new NotFoundException("音频不存在"));

        Long uid = SecurityUtil.getUserIdOrNull(request);

        if (audio.isDeleted())
            throw new NotFoundException("音频不存在");

        if ("PUBLISHED".equals(audio.getStatus()) || (uid != null && uid.equals(audio.getUserId()))) {
            // 给visitCount自动自动+1
            audio.setVisitCount(audio.getVisitCount() + 1);
            repo.save(audio); // 更新数据库

            if(uid != null && !uid.equals(audio.getUserId())) {
                UserBehaviorSignalEvent event = UserBehaviorSignalEvent.builder()
                        .behaviorType(BehaviorType.PUBLISH)
                        .userId(uid)
                        .behaviorTargetType(BehaviorTargetType.AUDIO)
                        .targetId(audioId)
                        .authorId(audio.getUserId())
                        .tags(audio.getTags() == null ? new ArrayList<>() : new ArrayList<>(audio.getTags()))
                        .build();
                publisher.register(event, EventChannelEnum.BEHAVIOR_PUSH);
            }
            return toDto(audio);
        }

        throw new AccessDeniedException("音频不可见");
    }

    @Override
    public List<AudioResponseDto> getMulti(HttpServletRequest request, List<Long> ids) {
        Long uid = SecurityUtil.getUserIdOrNull(request);

        return repo.findAllByIdIn(ids).stream()
                .filter(a -> !a.isDeleted() &&
                        ("PUBLISHED".equals(a.getStatus()) || (uid != null && uid.equals(a.getUserId()))))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AudioResponseDto> getNearbyAudio(Double latitude, Double longitude, Double distance) {
        // 首先检查distance(单位: m)范围，不能过大或者非法
        if (distance <= 0 || distance > 20000) {
            throw new BadRequestException("距离参数不合法");
        }

        // 然后检查经纬度坐标是否合理
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("经纬度参数不合法");
        }

        // 查询附近符合要求的音频
        // 查询附近音频

        // 转换为 DTO
        return repo.findNearby(latitude, longitude, distance)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 在数据库中搜索包含关键词的音频，支持多关键词搜索，结果按匹配度和发布时间排序
     * 
     * @param query 搜索关键词，多个关键词可以以空格分隔
     * @return 匹配的音频列表
     */
    @Override
    public List<AudioResponseDto> search(HttpServletRequest request, String query) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String[] spaceSplit = query.trim().split("\\s+");
        Set<String> keywords = new HashSet<>();

        for (String s : spaceSplit) {
            List<SegToken> tokens = segmenter.process(s, JiebaSegmenter.SegMode.SEARCH);
            for (SegToken token : tokens) {
                keywords.add(token.word);
            }
            if (!s.isEmpty()) keywords.add(s);
        }

        Map<Long, Audio> audioMap = new HashMap<>();
        Map<Long, Integer> matchCount = new HashMap<>();

        for (String keyword : keywords) {
            List<Audio> results = repo.searchByKeyword(keyword);
            for (Audio audio : results) {
                audioMap.put(audio.getId(), audio);
                matchCount.put(audio.getId(), matchCount.getOrDefault(audio.getId(), 0) + 1);
            }
        }

        List<Audio> sortedAudios = new ArrayList<>(audioMap.values());
        sortedAudios.sort((a, b) -> {
            int countCompare = matchCount.get(b.getId()).compareTo(matchCount.get(a.getId()));
            if (countCompare != 0) return countCompare;
            if (a.getPublishTime() == null) return 1;
            if (b.getPublishTime() == null) return -1;
            return b.getPublishTime().compareTo(a.getPublishTime());
        });

        Long uid = SecurityUtil.getUserIdOrNull(request);
        if(uid != null) {
            UserBehaviorSignalEvent event = UserBehaviorSignalEvent.builder()
                    .behaviorType(BehaviorType.SEARCH)
                    .userId(uid)
                    .keyword(query)
                    .behaviorTargetType(BehaviorTargetType.TAG)
                    .build();
            publisher.register(event, EventChannelEnum.BEHAVIOR_PUSH);
        }

        return sortedAudios.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * 获取推荐音频列表，基于音频的点赞数和发布时间进行排序
     * 使用 Redis 缓存结果以提高性能，缓存有效期为 1 分钟
     * 
     * @param limit 返回的列表长度
     * @return 推荐的音频列表
     */
    @Override
    public List<AudioResponseDto> getRecommendation(int limit) {
        String cacheKey = "audio:recommendation";

        List<AudioResponseDto> fullList = null;
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    fullList = objectMapper.readValue(cached, new TypeReference<List<AudioResponseDto>>(){});
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        if (fullList == null) {
            List<Audio> allAudios = repo.findAll();
            List<Object[]> likeCounts = repo.countLikesPerAudio();
            Map<Long, Long> likeCountMap = new HashMap<>();
            for (Object[] row : likeCounts) {
                likeCountMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }

            long now = System.currentTimeMillis();
            List<Audio> publishedAudios = allAudios.stream()
                    .filter(a -> "PUBLISHED".equals(a.getStatus()) && !a.isDeleted())
                    .sorted((a, b) -> {
                        double scoreA = calculateScore(a, likeCountMap.getOrDefault(a.getId(), 0L), now);
                        double scoreB = calculateScore(b, likeCountMap.getOrDefault(b.getId(), 0L), now);
                        return Double.compare(scoreB, scoreA);
                    })
                    .collect(Collectors.toList());

            fullList = publishedAudios.stream().map(this::toDto).collect(Collectors.toList());

            try {
                if (redisTemplate != null) {
                    redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(fullList), 1, TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (limit <= 0) {
            return fullList;
        }
        return fullList.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public String uploadPhoto(HttpServletRequest request, MultipartFile file) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("图片文件不能为空");
        }

        try {
            User user = userRepo.findById(uid)
                    .orElseThrow(() -> new NotFoundException("用户不存在"));

            File tempFile;

            tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            String objectName = "photo/" + uid + "/" + System.currentTimeMillis() + "-" + tempFile.getName();
            String photoUrl = storage.uploadFile(tempFile, objectName);

            tempFile.delete();

            return photoUrl;
        }
        catch (BadRequestException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AudioResponseDto> getAllUserAudios(HttpServletRequest request, Long userId) {
        Long uid = SecurityUtil.getUserIdOrNull(request);
        List<Audio> audios;

        if (Objects.equals(uid, userId)) {
            // 获取该用户的所有音频（排除已删除的）
            audios = repo.findByUserIdAndDeletedFalse(userId);
        } else {
            // 获取该用户公开可见的音频
            audios = repo.findByUserIdAndStatusAndDeletedFalse(userId, "PUBLISHED");
        }

        // 转换为 DTO
        return audios.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AudioResponseDto updateAudio(HttpServletRequest request, Long id, UpdateAudioRequest req) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);
        Audio audio = repo.findByIdAndUserIdAndStatusNotAndDeletedFalse(id, uid, "UPLOADED")
                .orElseThrow(() -> new NotFoundException("未找到音频或者无权修改此音频"));
        audio.setTitle(req.getNewTitle());
        audio.setDescription(req.getNewDescription());
        repo.save(audio);

        UserBehaviorSignalEvent event = UserBehaviorSignalEvent.builder()
                .userId(uid)
                .behaviorType(BehaviorType.PUBLISH)
                .behaviorTargetType(BehaviorTargetType.AUDIO)
                .textContent(req.getNewDescription())
                .build();
        publisher.register(event, EventChannelEnum.BEHAVIOR_PUSH);

        return toDto(audio);
    }

    // 计算音频推荐分数，基于点赞数和发布时间
    private double calculateScore(Audio audio, long likes, long now) {
        long publishTime = audio.getPublishTime() != null ? audio.getPublishTime().toEpochMilli() : 0;
        double hoursSincePublish = (now - publishTime) / (1000.0 * 3600.0);
        if (hoursSincePublish < 0) hoursSincePublish = 0;
        return likes / Math.pow(hoursSincePublish + 2, 1.5);
    }

    private AudioResponseDto toDto(Audio a) {
        return AudioResponseDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userName(a.getUser().getUsername())
                .audioUrl(a.getAudioUrl())
                .photoUrl(a.getPhotoUrl())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .title(a.getTitle())
                .description(a.getDescription())
                .tags(a.getTags())
                .uploadTime(a.getUploadTime())
                .publishTime(a.getPublishTime())
                .status(a.getStatus())
                .visitCount(a.getVisitCount())
                .build();
    }
}
