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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import at.favre.lib.crypto.bcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AliyunSmsVerifyService aliyunSmsVerifyService;
    private final RestTemplate restTemplate;

    @Value("${app.content-service-url:http://localhost:8084}")
    private String contentServiceUrl;

    public LoginResponse login(LoginRequest req) {
        boolean smsLogin = StringUtils.hasText(req.getPhone()) && StringUtils.hasText(req.getSmsCode());
        boolean pwdLogin = StringUtils.hasText(req.getUsername()) && StringUtils.hasText(req.getPassword());
        if (smsLogin && pwdLogin) {
            throw BusinessException.badRequest("请仅使用密码登录或验证码登录其中一种方式");
        }
        if (!smsLogin && !pwdLogin) {
            throw BusinessException.badRequest("请填写用户名和密码，或手机号和验证码");
        }

        User user;
        if (smsLogin) {
            String phone = normalizePhone(req.getPhone());
            aliyunSmsVerifyService.checkVerifyCode(phone, req.getSmsCode());
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (user == null) {
                throw BusinessException.unauthorized("该手机号未注册");
            }
        } else {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, req.getUsername()));
            if (user == null) {
                throw BusinessException.unauthorized("用户名或密码错误");
            }
            BCrypt.Result result = BCrypt.verifyer().verify(req.getPassword().toCharArray(), user.getPassword());
            if (!result.verified) {
                throw BusinessException.unauthorized("用户名或密码错误");
            }
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, new LoginResponse.UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername()));
    }

    public UserVO register(RegisterRequest req) {
        String phone = normalizePhone(req.getPhone());
        aliyunSmsVerifyService.checkVerifyCode(phone, req.getSmsCode());

        long uCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (uCount > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
        long pCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (pCount > 0) {
            throw BusinessException.conflict("该手机号已注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.withDefaults().hashToString(12, req.getPassword().toCharArray()));
        user.setNickname(req.getUsername());
        user.setPhone(phone);
        user.setRole("USER");
        userMapper.insert(user);
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    /**
     * 通过手机短信验证码重置登录密码（需先 {@code POST /api/auth/sms/send}，scene=RESET_PASSWORD）。
     */
    public void resetPasswordByPhone(String phone, String smsCode, String newPassword) {
        String p = normalizePhone(phone);
        aliyunSmsVerifyService.checkVerifyCode(p, smsCode);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, p));
        if (user == null) {
            throw BusinessException.badRequest("该手机号未注册");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw BusinessException.badRequest("密码至少 6 位");
        }
        user.setPassword(BCrypt.withDefaults().hashToString(12, newPassword.toCharArray()));
        userMapper.updateById(user);
    }

    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) return null;
        return toVO(user);
    }

    /** 按昵称或用户名模糊搜索（公开），用于搜索页「用户」；限制 20 条，不返回敏感字段 */
    public List<UserVO> searchByKeyword(String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String keyword = q.trim();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword))
                .last("LIMIT 20");
        List<User> list = userMapper.selectList(qw);
        return list.stream().map(UserService::toVO).collect(Collectors.toList());
    }

    /** 批量获取用户（仅返回非 null），用于关注列表等 */
    public List<UserVO> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<UserVO> result = new ArrayList<>();
        for (Long id : ids) {
            UserVO vo = getById(id);
            if (vo != null) result.add(vo);
        }
        return result;
    }

    private static UserVO toVO(User user) {
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
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        applyProfileVisibilityByModeration(user, vo);
        return vo;
    }

    /**
     * 资料审核未通过时，统一隐藏资料字段，避免任何页面误展示。
     */
    private static void applyProfileVisibilityByModeration(User user, UserVO vo) {
        if (user == null || vo == null) return;
        String st = user.getProfileModerationStatus();
        if (st == null || !"REJECTED".equalsIgnoreCase(st)) return;
        vo.setNickname(null);
        vo.setAvatar(null);
        vo.setCover(null);
        vo.setGender(null);
        vo.setResidence(null);
        vo.setIndustry(null);
        vo.setBio(null);
        vo.setIntro("该用户个人信息未通过审核，资料暂不可见");
    }

    /** 13812345678 -> 138****5678 */
    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone.charAt(0) + "****" + phone.substring(phone.length() - 2);
    }

    static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.trim().replaceAll("\\s+", "");
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
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setProfileModerationStatus("PENDING");
        } else {
            user.setProfileModerationStatus("APPROVED");
        }
        userMapper.updateById(user);
        submitProfileModeration(user);
        return getById(userId);
    }

    private void submitProfileModeration(User user) {
        if (user == null || user.getId() == null) return;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return;
        try {
            String url = contentServiceUrl.replaceFirst("/$", "") + "/api/admin/moderation/tasks/submit";
            var req = new java.util.HashMap<String, Object>();
            req.put("resourceType", "USER_PROFILE");
            req.put("resourceId", user.getId());
            req.put("ownerUserId", user.getId());
            req.put("payloadSnapshot", "nickname=" + nullable(user.getNickname())
                    + "\nintro=" + nullable(user.getIntro())
                    + "\nresidence=" + nullable(user.getResidence())
                    + "\nindustry=" + nullable(user.getIndustry())
                    + "\nbio=" + nullable(user.getBio()));
            @SuppressWarnings("unchecked")
            var resp = restTemplate.postForObject(url, req, java.util.Map.class);
            if (resp != null && resp.get("status") != null) {
                String st = String.valueOf(resp.get("status")).toUpperCase();
                user.setProfileModerationStatus(st);
                userMapper.updateById(user);
            }
        } catch (Exception ignored) {
        }
    }

    private static String nullable(String s) {
        return s == null ? "" : s;
    }
}
