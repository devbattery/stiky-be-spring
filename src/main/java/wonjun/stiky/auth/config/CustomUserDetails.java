package wonjun.stiky.auth.config;

import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import wonjun.stiky.member.domain.Member;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(member.getRole()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료되지 않음
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠기지 않음
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료되지 않음
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화됨
    }

}
