package com.shixi.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixi.business.entity.EmployeeUserEntity;
import com.shixi.business.mapper.EmployeeUserMapper;
import com.shixi.security.CurrentUser;
import com.shixi.security.CurrentUserContext;
import com.shixi.security.JwtTokenService;
import com.shixi.security.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final EmployeeUserMapper employeeUserMapper;
    private final JwtTokenService jwtTokenService;

    public AuthService(EmployeeUserMapper employeeUserMapper, JwtTokenService jwtTokenService) {
        this.employeeUserMapper = employeeUserMapper;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new UnauthorizedException("用户名或密码不能为空");
        }

        EmployeeUserEntity user = employeeUserMapper.selectOne(
                new LambdaQueryWrapper<EmployeeUserEntity>()
                        .eq(EmployeeUserEntity::getUsername, request.username().trim())
                        .last("LIMIT 1")
        );
        if (user == null || !user.isEnabled() || !sha256Hex(request.password()).equalsIgnoreCase(user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        CurrentUser currentUser = toCurrentUser(user);
        JwtTokenService.TokenIssue issue = jwtTokenService.createToken(currentUser);
        return new LoginResponse(issue.token(), issue.expiresAt(), toUserProfile(currentUser));
    }

    public UserProfile currentUser() {
        return toUserProfile(CurrentUserContext.require());
    }

    private CurrentUser toCurrentUser(EmployeeUserEntity user) {
        return new CurrentUser(
                user.getUserId(),
                user.getUsername(),
                user.getEmployeeId(),
                user.getDisplayName(),
                user.getRole()
        );
    }

    private UserProfile toUserProfile(CurrentUser user) {
        return new UserProfile(
                user.userId(),
                user.username(),
                user.employeeId(),
                user.displayName(),
                user.role()
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("密码摘要计算失败", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, long expiresAt, UserProfile user) {
    }

    public record UserProfile(String userId, String username, String employeeId, String displayName, String role) {
    }
}
