package wonjun.stiky.member.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // jOOQ 매핑 편의성을 위해 추가 (Record -> POJO)
@NoArgsConstructor
public class Member {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String role;
    private String provider;
    private String providerId;

    @Builder
    public Member(Long id, String email, String password, String nickname, String role, String provider, String providerId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    public void updateSocialInfo(String provider, String providerId) {
        if (this.provider == null || !this.provider.equals("local")) {
            this.provider = provider;
        }
        this.providerId = providerId;
    }
}