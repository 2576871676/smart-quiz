package org.king.smartquiz.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import org.king.smartquiz.common.constant.CacheConstants;
import org.king.smartquiz.common.core.ResultCode;
import org.king.smartquiz.common.exception.BusinessException;
import org.king.smartquiz.common.utils.RedisUtil;
import org.king.smartquiz.framework.security.LoginUser;
import org.king.smartquiz.framework.security.UserContext;
import org.king.smartquiz.system.dto.request.UpdatePasswordDTO;
import org.king.smartquiz.system.dto.request.UpdateProfileDTO;
import org.king.smartquiz.system.entity.User;
import org.king.smartquiz.system.mapper.UserMapper;
import org.king.smartquiz.system.service.UserService;
import org.king.smartquiz.system.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户信息服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public UserVO getCurrentUserInfo() {
        User user = currentUser();
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        User user = currentUser();

        // 仅更新非空字段，避免把未传的字段覆盖为 null
        if (StrUtil.isNotBlank(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        userMapper.updateById(user);

        // 同步刷新 Redis 中缓存的登录态昵称，保持一致
        refreshCachedLoginUser(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        User user = currentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
        }

        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    /** 取当前登录用户实体，不存在则抛异常 */
    private User currentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** 用最新资料刷新 Redis 登录态（保留剩余有效期） */
    private void refreshCachedLoginUser(User user) {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || StrUtil.isBlank(loginUser.getUuid())) {
            return;
        }
        String key = CacheConstants.LOGIN_TOKEN_KEY + loginUser.getUuid();
        long ttl = redisUtil.getExpire(key);
        if (ttl <= 0) {
            return;
        }
        loginUser.setNickname(user.getNickname());
        redisUtil.setCacheObject(key, loginUser, ttl, TimeUnit.SECONDS);
    }
}
