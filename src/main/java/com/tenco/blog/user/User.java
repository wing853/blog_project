package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "user_tb")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // 사용자명 중복 방지를 위한 유니크 제약 조건 설정
    @Column(unique = true)
    private String username;

    private String password;
    private String email;
    // 엔티티가 영속화 될 때 자동으로 현재 시간을 주입해라 pc -> db
    @CreationTimestamp
    private Timestamp createdAt;

    // User 테이블에는 이미지 파일명만 저장할 예정(실제 데이터는 내 서버 컴퓨터 로컬에 저장)
    private String profileImage; // 프로필 이미지는 선택사항


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserRole> roles = new ArrayList<>();

    @Column(nullable = false)
    @ColumnDefault("'LOCAL'") // 어노테이션으로 디폴트값 선언방법 문자열일경우 '' 반드시 사용
    @Enumerated(EnumType.STRING)
    private OAuthProvider oAuthProvider;

    @Builder
    public User(Integer id, String username, String password,
                String email, Timestamp createdAt, String profileImage) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
    }

    // 편의 기능 추가 - 회원 정보 수정
    public void update(UserRequest.UpdateDTO updateDTO, String newProfileImageFilename) {
        this.password = updateDTO.getPassword();
        this.profileImage = newProfileImageFilename;
        // Dirty Checking 처리
    }

    // User 엔티티에 권한 관련 편의 기능 만들어보기
    // Role 추가 메서드
    public void addRole(Role role) {
        this.roles.add(UserRole.builder()
                .role(role)
                .build());
    }

    // 해당 Role을 가지고있는지 여부 확인
    public boolean hasRole(Role role) {
        // 1. 방어적 코드 작성
        if (this.roles == null || this.roles.isEmpty()) {
            // Role 자체가 설정되지 않은 상태
            return false;
        }

        for (UserRole userRole : this.roles) {
            if (userRole.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    // 관리자 여부 확인 편의 메서드
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    // Mustache 화면에서 사용할 편의 메서드 1
    public String getRoleDisplay() {
        return isAdmin() ? "ADMIN" : "USER";
    }

    // Mustache 화면에서 사용할 편의 메서드 2
    // OAuthProvider 값에 따라서 경로 변수를 다르게 리턴
    public String getProfilePath() {
        if(this.profileImage == null) {
            return null;
        }

        // 이미지 경로가 http로 시작(소셜가입)
        if(this.profileImage.startsWith("http")) {
            return this.getProfileImage();
        }

        // 로컬 이미지(서버 기준 경로)
        return "/images/" + this.profileImage;
    }
}
