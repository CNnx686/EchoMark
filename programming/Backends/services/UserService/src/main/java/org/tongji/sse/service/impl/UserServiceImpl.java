package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.UserProfileDTO;
import org.tongji.sse.dto.UserProfileUpdateRequestDTO;
import org.tongji.sse.dto.UserSettingDTO;
import org.tongji.sse.entity.Favorite;
import org.tongji.sse.entity.UserProfile;
import org.tongji.sse.entity.UserSetting;
import org.tongji.sse.repository.FavoriteRepository;
import org.tongji.sse.repository.UserProfileRepository;
import org.tongji.sse.repository.UserSettingRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 实现了 UserService 接口，提供用户个人资料的具体业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserSettingRepository userSettingRepository;

    @Value("${resource-server.url}")
    private String resourceServerBaseUrl;

    /**
     * 获取当前登录用户的个人资料
     *
     * @param request HTTP请求
     * @return 包含用户个人资料DTO的API响应
     */
    @SuppressWarnings("null")
    @Override
    public ApiResponse<UserProfileDTO> getMyProfile(HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) return ApiResponse.error(401, "未认证");
        return userProfileRepository.findById(userId)
        .map(profile -> ApiResponse.success(mapToDTO(profile)))
        .orElseGet(() -> {
            // 用户不存在，在数据库中创建默认资料并返回
            UserProfile defaultProfile = UserProfile.builder()
                    .userId(userId)
                    .nickname("User_" + userId)
                    .build();
            userProfileRepository.save(defaultProfile);
            return ApiResponse.success(mapToDTO(defaultProfile));
        });
    }

    /**
     * 更新当前登录用户的个人资料
     *
     * @param request HTTP请求
     * @param dto 包含更新信息的DTO
     * @param avatarFile 用户头像文件
     * @return 包含更新后的用户个人资料DTO的API响应
     */
    @SuppressWarnings("null")
    @Override
    public ApiResponse<UserProfileDTO> updateMyProfile(HttpServletRequest request, UserProfileUpdateRequestDTO dto, MultipartFile avatarFile) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) return ApiResponse.error(401, "未认证");

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .nickname("User_" + userId)
                        .build());

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = uploadAvatarToResourceServer(userId, avatarFile);
            profile.setAvatarUrl(avatarUrl);
        } else if (dto.getAvatarUrl() != null) {
            profile.setAvatarUrl(dto.getAvatarUrl());
        }

        if (dto.getNickname() != null) profile.setNickname(dto.getNickname());
        if (dto.getBio() != null) profile.setBio(dto.getBio());
        if (dto.getSelfDescription() != null) profile.setSelfDescription(dto.getSelfDescription());

        UserProfile saved = userProfileRepository.save(profile);
        return ApiResponse.success(mapToDTO(saved));
    }

    /**
     * 获取其他用户个人资料
     * 
     * @param userId 用户ID
     * @return 包含用户个人资料DTO的API响应
     */
    @SuppressWarnings("null")
    @Override
    public ApiResponse<UserProfileDTO> getProfile(Long userId) {
        return userProfileRepository.findById(userId)
                .map(profile -> ApiResponse.success(mapToDTO(profile)))
                .orElseGet(() -> {
                    // 用户不存在，返回默认资料但不保存，并提示用户不存在
                    UserProfile defaultProfile = UserProfile.builder()
                            .userId(userId)
                            .nickname("User_" + userId)
                            .build();
                    return ApiResponse.success("用户不存在", mapToDTO(defaultProfile));
                });
    }



    /**
     * 上传用户头像到资源服务器
     * 
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像的URL地址
     */
    private String uploadAvatarToResourceServer(Long userId, MultipartFile file) {
        try {
            String filename = "users/" + userId + "/avatar_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String resourceServerUrl = resourceServerBaseUrl + "/upload/" + filename;
            
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(resourceServerUrl, file.getBytes());
            
            return resourceServerBaseUrl + "/files/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }
    }

    /**
     * 将 UserProfile 实体转换为 UserProfileDTO
     * 
     * @param entity UserProfile 实体
     * @return UserProfileDTO 对象
     */
    private UserProfileDTO mapToDTO(UserProfile entity) {
        return UserProfileDTO.builder()
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .avatarUrl(entity.getAvatarUrl())
                .bio(entity.getBio())
                .selfDescription(entity.getSelfDescription())
                .build();
    }

    @Override
    public ApiResponse<List<Long>> getMyFavorites(HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) return ApiResponse.error(401, "未认证");
        
        List<Long> recordIds = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(Favorite::getRecordId)
                .collect(Collectors.toList());
                
        return ApiResponse.success(recordIds);
    }

    @SuppressWarnings("null")
    @Override
    @Transactional
    public ApiResponse<Void> addFavorite(HttpServletRequest request, Long recordId) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) return ApiResponse.error(401, "未认证");
        
        if (favoriteRepository.existsByUserIdAndRecordId(userId, recordId)) {
            return ApiResponse.error(400, "已收藏该记录");
        }
        
        Favorite favorite = Favorite.builder()
                .userId(userId)
                .recordId(recordId)
                .build();
        favoriteRepository.save(favorite);
        
        return ApiResponse.success(null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeFavorite(HttpServletRequest request, Long recordId) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) return ApiResponse.error(401, "未认证");
        
        favoriteRepository.deleteByUserIdAndRecordId(userId, recordId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<UserSettingDTO> getMySettings(HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) {
            return ApiResponse.error(401, "未认证");
        }

        @SuppressWarnings("null")
        UserSetting setting = userSettingRepository.findById(userId)
                .orElseGet(() -> {
                    // 如果设置不存在，创建默认设置
                    UserSetting newSetting = UserSetting.builder()
                            .userId(userId)
                            .notificationMute(false) // 默认不静默
                            .build();
                    return userSettingRepository.save(newSetting);
                });

        return ApiResponse.success(new UserSettingDTO(setting.getUserId(), setting.getNotificationMute()));
    }

    @Override
    public ApiResponse<UserSettingDTO> updateMySettings(HttpServletRequest request, UserSettingDTO dto) {
        Long userId = SecurityUtil.getUserIdOrNull(request);
        if (userId == null) {
            return ApiResponse.error(401, "未认证");
        }

        UserSetting setting = userSettingRepository.findById(userId)
                .orElseGet(() -> UserSetting.builder()
                        .userId(userId)
                        .notificationMute(false)
                        .build());

        if (dto.getNotificationMute() != null) {
            setting.setNotificationMute(dto.getNotificationMute());
        }

        @SuppressWarnings("null")
        UserSetting saved = userSettingRepository.save(setting);
        return ApiResponse.success(new UserSettingDTO(saved.getUserId(), saved.getNotificationMute()));
    }
}

