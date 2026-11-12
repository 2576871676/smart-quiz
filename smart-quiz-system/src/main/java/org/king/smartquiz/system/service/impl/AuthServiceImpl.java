package org.king.smartquiz.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.king.smartquiz.common.constant.CacheConstants;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.common.utils.JwtUtil;
import org.king.smartquiz.common.utils.RedisUtil;
import org.king.smartquiz.framework.security.LoginUser;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.LoginDTO;
import org.king.smartquiz.system.dto.request.RegisterDTO;
import org.king.smartquiz.system.entity.User;
import org.king.smartquiz.system.mapper.UserMapper;
import org.king.smartquiz.system.service.AuthService;
import org.king.smartquiz.system.vo.LoginVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现：BCrypt 加密 + JWT 签发 + Redis 登录态管理。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /** BCrypt 编码器（线程安全，可复用单例） */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StrUtil.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        user.setRole(0);    // 默认学生
        user.setStatus(1);  // 默认正常
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        // 生成登录态并写入 Redis
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long expireSeconds = jwtUtil.getExpire();
        long now = System.currentTimeMillis();

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setRole(user.getRole());
        loginUser.setUuid(uuid);
        loginUser.setLoginTime(now);
        loginUser.setExpireTime(now + expireSeconds * 1000);
        redisUtil.setCacheObject(CacheConstants.LOGIN_TOKEN_KEY + uuid,
                loginUser, expireSeconds, TimeUnit.SECONDS);

        // 签发 JWT（携带 uuid 关联 Redis 登录态）
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole(), uuid);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public void logout() {
        LoginUser loginUser = UserContext.get();
        if (loginUser != null && StrUtil.isNotBlank(loginUser.getUuid())) {
            redisUtil.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + loginUser.getUuid());
        }
        UserContext.clear();
    }
}
