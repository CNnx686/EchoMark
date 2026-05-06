package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.NotificationDTO;
import org.tongji.sse.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 批量根据 ID 查询
    @PostMapping("/batch")
    public ApiResponse<List<NotificationDTO>> getByIds(
            @RequestBody List<Long> notificationIds,
            HttpServletRequest request) {
        return ApiResponse.success(
                notificationService.getNotificationsByIds(notificationIds, request)
        );
    }

    // 获取当前用户的所有通知
    @GetMapping("/me")
    public ApiResponse<List<NotificationDTO>> getMyNotifications(
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                notificationService.getUserNotifications(request)
        );
    }

    @PostMapping("/read")
    public ApiResponse<Boolean> readByIds(
            @RequestBody List<Long> notificationIds,
            HttpServletRequest request){
        return ApiResponse.success(
                notificationService.readNotificationsByIds(notificationIds, request)
        );
    }
}

