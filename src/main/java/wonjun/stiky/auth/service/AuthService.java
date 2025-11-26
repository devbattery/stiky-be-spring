package wonjun.stiky.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.auth.controller.dto.request.LoginRequest;
import wonjun.stiky.auth.controller.dto.request.SignupRequest;
import wonjun.stiky.auth.controller.dto.response.SignupResponse;
import wonjun.stiky.auth.config.JwtTokenProvider;
import wonjun.stiky.auth.controller.dto.TokenDto;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.service.MemberQueryService;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberQueryService memberQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public SignupResponse signup(SignupRequest request) {
        if (memberQueryService.fetchByEmailOpt(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 계정"); // TODO: 커스텀 예외처리
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("ROLE_USER")
                .provider("local")
                .build();

        Member savedMember = memberQueryService.save(member);
        return SignupResponse.of(savedMember.getId());
    }

    public TokenDto login(LoginRequest request) {
        Member member = memberQueryService.fetchByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호."); // TODO: 커스텀 예외처리
        }

        TokenDto tokenDto = jwtTokenProvider.generateToken(member.getEmail(), member.getRole());

        redisTemplate.opsForValue()
                .set("RT:" + member.getEmail(), tokenDto.getRefreshToken(), 7, TimeUnit.DAYS);

        return tokenDto;
    }

}
