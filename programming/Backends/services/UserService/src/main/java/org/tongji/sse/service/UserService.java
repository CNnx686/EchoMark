package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.UserProfileDTO;
import org.tongji.sse.dto.UserProfileUpdateRequestDTO;

/**
 * 用户服务接口
 * 定义了用户个人资料相关的业务逻辑方法
 */
public interface UserService {
    /**
     * 获取用户个人资料
     * 
     * @param userId 用户ID
     * @return 包含用户个人资料DTO的API响应
     */
    ApiResponse<UserProfileDTO> getProfile(Long userId);

    /**
     * 获取当前登录用户的个人资料
     *
     * @param request HTTP请求
     * @return 包含用户个人资料DTO的API响应
     */
    ApiResponse<UserProfileDTO> getMyProfile(HttpServletRequest request);

    /**
     * 更新当前登录用户的个人资料
     *
     * @param request HTTP请求
     * @param dto 包含更新信息的DTO
     * @param avatarFile 用户头像文件
     * @return 包含更新后的用户个人资料DTO的API响应
     */
    ApiResponse<UserProfileDTO> updateMyProfile(HttpServletRequest request, UserProfileUpdateRequestDTO dto, MultipartFile avatarFile);

    /**
     * 获取当前用户的收藏列表
     * @param request HTTP请求
     * @return 收藏的音频ID列表
     */
    ApiResponse<java.util.List<Long>> getMyFavorites(HttpServletRequest request);

    /**
     * 添加收藏
     * @param request HTTP请求
     * @param recordId 音频记录ID
     * @return 操作结果
     */
    ApiResponse<Void> addFavorite(HttpServletRequest request, Long recordId);

    /**
     * 删除收藏
     * @param request HTTP请求
     * @param recordId 音频记录ID
     * @return 操作结果
     */
    ApiResponse<Void> removeFavorite(HttpServletRequest request, Long recordId);

    /**
     * 获取当前用户的设置
     * @param request HTTP请求
     * @return 用户设置DTO
     */
    ApiResponse<org.tongji.sse.dto.UserSettingDTO> getMySettings(HttpServletRequest request);

    /**
     * 更新当前用户的设置
     * @param request HTTP请求
     * @param dto 用户设置DTO
     * @return 更新后的用户设置DTO
     */
    ApiResponse<org.tongji.sse.dto.UserSettingDTO> updateMySettings(HttpServletRequest request, org.tongji.sse.dto.UserSettingDTO dto);
}

