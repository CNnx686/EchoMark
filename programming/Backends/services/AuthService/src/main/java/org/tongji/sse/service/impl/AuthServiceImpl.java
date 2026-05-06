package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.tongji.sse.dto.*;
import org.tongji.sse.entity.User;
import org.tongji.sse.entity.UserProfile;
import org.tongji.sse.repository.UserProfileRepository;
import org.tongji.sse.repository.UserRepository;
import org.tongji.sse.security.JwtUtil;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.AuthService;
import org.tongji.sse.service.EmailService;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * AuthServiceImpl 类是 AuthService 接口的实现类，
 * 负责处理用户认证相关的业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 用户数据访问层，用于操作用户数据
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    // 密码加密器，用于对用户密码进行加密
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 发送注册验证码
     * @param email 用户邮箱
     * @return ApiResponse<Void>
     */
    @Override
    public ApiResponse<Void> sendRegisterCode(String email) {
        if (email == null || email.isEmpty()) {
            return ApiResponse.error(400, "邮箱不能为空");
        }
        if (userRepository.existsByEmail(email)) {
            return ApiResponse.error(409, "邮箱已被注册");
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存储验证码到 Redis，设置5分钟过期
        redisTemplate.opsForValue().set("REGISTER_CODE:" + email, code, 5, TimeUnit.MINUTES);

        // 发送验证码邮件
        emailService.sendSimpleMessage(email, "注册验证码", "您的声印注册验证码是：" + code + "，有效期5分钟。");

        return ApiResponse.success(null);
    }

    /**
     * 用户注册方法
     * @param dto 包含用户名、密码、邮箱等注册信息
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @SuppressWarnings("null")
    @Override
    public ApiResponse<Void> register(UserRegisterRequestDTO dto) {
        // 检查用户名是否已存在
        if(dto.getUsername() != null && userRepository.existsByUsername(dto.getUsername())) {
            return ApiResponse.error(409, "用户名已存在");
        }
        // 检查邮箱是否已被注册
        if(dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            return ApiResponse.error(409, "邮箱已被注册");
        }
        // 检查手机号是否已被注册
        if(dto.getPhoneNumber() != null && userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            return ApiResponse.error(409, "手机号已被注册");
        }

        // 验证验证码
        String cachedCode = redisTemplate.opsForValue().get("REGISTER_CODE:" + dto.getEmail());
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        // 创建用户实体并保存到数据库
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword())) // 加密密码
                .registrationTime(java.time.LocalDateTime.now())
                .build();
        userRepository.save(user);

        // 创建默认个人资料
        UserProfile userProfile = UserProfile.builder()
                .userId(user.getId())
                .nickname(user.getUsername())
                .bio("这个人很懒，什么都没有写")
                .selfDescription("这个人很懒，什么都没有写")
                .build();
        userProfileRepository.save(userProfile);

        // 删除验证码
        redisTemplate.delete("REGISTER_CODE:" + dto.getEmail());

        // 不返回具体数据，data = null
        return ApiResponse.success(null);
    }

    /**
     * 用户登录方法
     * @param dto 包含用户名和密码的登录信息
     * @return ApiResponse<LoginResponseDTO>，data 包含 token 和用户名
     */
    @Override
    public ApiResponse<LoginResponseDTO> login(UserLoginRequestDTO dto) {
        String identifier = dto.getUsername();
        Optional<User> optionalUser = userRepository.findByUsername(identifier);
        if (optionalUser.isEmpty()) {
            optionalUser = userRepository.findByEmail(identifier);
        }
        if (optionalUser.isEmpty()) {
            optionalUser = userRepository.findByPhoneNumber(identifier);
        }

        if(optionalUser.isEmpty() || !passwordEncoder.matches(dto.getPassword(), optionalUser.get().getPassword())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        User user = optionalUser.get();
        String token = JwtUtil.generateToken(user.getId());

        LoginResponseDTO responseDTO = LoginResponseDTO.builder()
                .username(user.getUsername())
                .token(token)
                .build();

        return ApiResponse.success(responseDTO);
    }

    /**
     * 获取当前用户信息
     * @param request HTTP 请求，包含用户的 JWT Token
     * @return ApiResponse<UserResponseDTO>，data 包含当前用户信息
     */
    @Override
    public ApiResponse<UserResponseDTO> getCurrentUser(HttpServletRequest request) {
        Long uid = SecurityUtil.getUserIdOrNull(request);
        if (uid == null) return ApiResponse.error(401, "未认证");

        User user = userRepository.findById(uid).orElse(null);
        if (user == null) return ApiResponse.error(404, "用户不存在");

        UserResponseDTO dto = UserResponseDTO.builder()
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .registrationTime(user.getRegistrationTime())
                .build();

        return ApiResponse.success(dto);
    }

    /**
     * 用户登出方法
     * @param request HTTP 请求，包含用户的 JWT Token
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @Override
    public ApiResponse<Void> logout(HttpServletRequest request) {
        // In a stateless JWT system, logout is handled by the client.
        // Optionally, we can blacklist the token here using Redis.
        return ApiResponse.success(null);
    }

    /**
     * 请求重置密码
     * @param identifier 用户的邮箱
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @SuppressWarnings("null")
    @Override
    public ApiResponse<Void> requestPasswordReset(String identifier) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isEmpty()) {
            user = userRepository.findByPhoneNumber(identifier);
        }
        if (user.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        String email = user.get().getEmail();
        if (email == null || email.isEmpty()) {
            return ApiResponse.error(400, "该用户未绑定邮箱，无法通过邮箱重置密码");
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存储验证码到 Redis，设置5分钟过期
        redisTemplate.opsForValue().set("RESET_CODE:" + email, code, 5, TimeUnit.MINUTES);

        // 发送验证码邮件
        emailService.sendSimpleMessage(email, "重置密码验证码", "您的声印重置密码验证码是：" + code + "，有效期5分钟。");

        return ApiResponse.success(null);
    }

    /**
     * 重置密码
     * @param dto 包含验证码和新密码
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @Override
    public ApiResponse<Void> resetPassword(PasswordResetRequestDTO dto) {
        Optional<User> optionalUser = userRepository.findByEmail(dto.getIdentifier());
        if (optionalUser.isEmpty()) {
            optionalUser = userRepository.findByPhoneNumber(dto.getIdentifier());
        }
        if (optionalUser.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        User user = optionalUser.get();
        String email = user.getEmail();

        if (email == null || email.isEmpty()) {
            return ApiResponse.error(400, "用户未绑定邮箱");
        }

        String cachedCode = redisTemplate.opsForValue().get("RESET_CODE:" + email);

        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // Delete code
        redisTemplate.delete("RESET_CODE:" + email);

        return ApiResponse.success(null);
    }
}
