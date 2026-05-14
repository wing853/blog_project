package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    // User : UserRole 연관 관계를 단방향 1:N
    // JPA가 1:N 구조일 경우 (User, UserRole), JoinColumn(name="user_id")의미는
    // 여기 테이블에 컬럼 user_id 생성하라는 의미
    // 그런데 1:N 구조에서 FK 컬럼이 1 쪽테이블에 생성되는 경우는 없다.
    // 무조건 N 쪽에 FK 컬럼이 만들어져야 하기 때문에 자동으로 User테이블에
    // @JoinColumn("user_id") 하더라도 알아서 UserRole 컬럼을 자기가 생성한다.

    /**
     * 사용자 권한 목록
     * User(1) : UserRole(N) 연관관계를 정의 함
     * <p>
     * 1. @OneToMany + JoinColumn("user_id")
     * - User가 UserRole 리스트를 관리함(단방향)
     * - 실제 DB user_role_tb 테이블에 FK 컬럼은 user_id 명이 user_role_tb에 생성된다
     * <p>
     * 2. CasecadeType.ALL 운명 공동체
     * Java 기준에서 User 저장하면 Role도 자동 저장되고, User 삭제하면 가지고 있던
     * Role들도 다 삭제. DB에서 실제 delete 쿼리가 발생
     * <p>
     * 3. orPhanRemoval: 리스트와 DB를 동기화
     * DB에서 실제 delete 쿼리가 발생 -> true 처리
     * <p>
     * 4. fetch = FetchType.EAGER (특별취큽)
     * 데이터양이 얼마 되지 않아 한번에 데이터를 채워서 가지고 오는것이 편리
     *
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserRole> roles = new ArrayList<>();

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
        if(this.roles == null || this.roles.isEmpty()) {
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

    // Mustache 화면에서 사용할 편의 메서드
    public String getRoleDisplay() {
        return isAdmin() ? "ADMIN" : "USER";
    }
}
