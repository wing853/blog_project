package com.tenco.blog.user;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.Properties;

public class UserResponse {

    @Data@NoArgsConstructor@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OAuthToken {
        private String accessToken;
        private String tokenType;
        private String refreshToken;
        private Integer expiresIn;
        private String scope;
        private Integer refreshTokenExpiresIn;}

    @Data@NoArgsConstructor@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class KakaoProfile {
        private Long id;
        private KakaoAccount kakaoAccount;


        @Data
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class KakaoAccount {
            private Profile profile;

            @Data
            @NoArgsConstructor
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
            public static class Profile {
                private String nickname;
                private String thumbnailImageUrl;
                private String profileImageUrl;
                private Boolean isDefaultImage;
                private Boolean isDefaultNickname;
            }
        }
    }
}