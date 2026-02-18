package com.blog.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserVO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
    }
}
