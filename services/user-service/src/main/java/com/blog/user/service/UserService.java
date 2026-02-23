package com.blog.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.user.dto.LoginRequest;
import com.blog.user.dto.LoginResponse;
import com.blog.user.dto.RegisterRequest;
import com.blog.user.dto.UpdateProfileRequest;
import com.blog.user.dto.UserVO;
import com.blog.user.entity.User;
import com.blog.user.mapper.UserMapper;
import com.blog.user.exception.BusinessException;
import com.blog.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import at.favre.lib.crypto.bcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        BCrypt.Result result = BCrypt.verifyer().verify(req.getPassword().toCharArray(), user.getPassword());
        if (!result.verified) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, new LoginResponse.UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername()));
    }

    public UserVO register(RegisterRequest req) {
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.withDefaults().hashToString(12, req.getPassword().toCharArray()));
        user.setNickname(req.getUsername());
        user.setRole("USER");
        userMapper.insert(user);
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCover(user.getCover());
        vo.setGender(user.getGender());
        vo.setIntro(user.getIntro());
        vo.setResidence(user.getResidence());
        vo.setIndustry(user.getIndustry());
        vo.setBio(user.getBio());
        vo.setWechatId(user.getWechatId());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public UserVO updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getCover() != null) user.setCover(req.getCover());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getIntro() != null) user.setIntro(req.getIntro());
        if (req.getResidence() != null) user.setResidence(req.getResidence());
        if (req.getIndustry() != null) user.setIndustry(req.getIndustry());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getWechatId() != null) user.setWechatId(req.getWechatId());
        userMapper.updateById(user);
        return getById(userId);
    }
}
