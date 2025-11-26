package wonjun.stiky.acceptance;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import wonjun.stiky.auth.config.JwtTokenProvider;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.MemberRepository;

class AuthAcceptanceTest extends AcceptanceTestBase {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("OAuth2 인증 코드로 액세스 토큰 교환 API")
    void exchangeToken() throws Exception {
        // Given
        // 1. 가상의 인증 코드 생성
        String code = UUID.randomUUID().toString();
        String expectedAccessToken = "mock-access-token-12345";

        // 2. Redis에 인증 코드와 토큰 매핑 저장 (OAuth2SuccessHandler가 수행하는 작업을 시뮬레이션)
        redisTemplate.opsForValue().set("LOGIN_CODE:" + code, expectedAccessToken, 60, TimeUnit.SECONDS);

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("code", code);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedAccessToken))
                .andDo(document("auth-exchange-token",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("토큰 교환")
                                .description("OAuth2 로그인 후 발급받은 임시 코드로 액세스 토큰을 교환합니다.")
                                .requestSchema(Schema.schema("TokenExchangeRequest"))
                                .responseSchema(Schema.schema("TokenExchangeResponse"))
                                .requestFields(
                                        fieldWithPath("code").description("OAuth2 리다이렉트 시 받은 임시 코드")
                                )
                                .responseFields(
                                        fieldWithPath("accessToken").description("JWT 액세스 토큰")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("일반 회원가입 API")
    void signup() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "wonjun@example.com",
                    "password": "password1234",
                    "nickname": "원준"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andDo(document("auth-signup",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("일반 회원가입")
                                .description("이메일과 비밀번호로 회원가입을 진행합니다.")
                                .requestSchema(Schema.schema("SignupRequest"))
                                .requestFields(
                                        fieldWithPath("email").description("이메일"),
                                        fieldWithPath("password").description("비밀번호"),
                                        fieldWithPath("nickname").description("이름")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("일반 로그인 API")
    void login() throws Exception {
        // Given
        String email = "login@example.com";
        String password = "password1234";

        memberRepository.save(Member.builder()
                .email(email)
                .password(new BCryptPasswordEncoder().encode(password))
                .nickname("로그인유저")
                .role("ROLE_USER")
                .provider("local")
                .build());

        String requestBody = """
                {
                    "email": "login@example.com",
                    "password": "password1234"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andDo(document("auth-login",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("일반 로그인")
                                .description("이메일과 비밀번호로 로그인하여 토큰을 발급받습니다.")
                                .requestSchema(Schema.schema("LoginRequest"))
                                .responseSchema(Schema.schema("LoginResponse"))
                                .requestFields(
                                        fieldWithPath("email").description("이메일"),
                                        fieldWithPath("password").description("비밀번호")
                                )
                                .responseFields(
                                        fieldWithPath("grantType").description("인증 타입 (Bearer)"),
                                        fieldWithPath("accessToken").description("액세스 토큰"),
                                        fieldWithPath("refreshToken").description("리프레시 토큰")
                                )
                                .build())
                ));
    }

}