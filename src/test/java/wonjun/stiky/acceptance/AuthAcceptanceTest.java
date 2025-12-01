package wonjun.stiky.acceptance;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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
        String code = UUID.randomUUID().toString();
        String expectedAccessToken = "mock-access-token-12345";
        String expectedRefreshToken = "mock-refresh-token-67890"; // 리프레시 토큰 추가

        // 2. Redis에 인증 코드와 토큰 매핑 저장
        redisTemplate.opsForValue().set("LOGIN_CODE:" + code, expectedAccessToken, 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("LOGIN_CODE:" + code + ":RT", expectedRefreshToken, 60, TimeUnit.SECONDS);

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("code", code);
        String requestBody = objectMapper.writeValueAsString(requestMap);

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedAccessToken))
                .andExpect(cookie().exists("refresh_token")) // 쿠키 존재 여부 확인
                .andExpect(cookie().value("refresh_token", expectedRefreshToken)) // 쿠키 값 확인
                .andDo(document("auth-exchange-token",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("토큰 교환")
                                .description("OAuth2 로그인 후 발급받은 임시 코드로 액세스 토큰을 교환하고, 리프레시 토큰을 쿠키로 설정합니다.")
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
    @DisplayName("일반 회원가입 API - 성공")
    void signup() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "wonjun@example.com",
                    "password": "password1234",
                    "repeatPassword": "password1234",
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
                                .responseSchema(Schema.schema("SignupResponse"))
                                .requestFields(
                                        fieldWithPath("email").description("이메일 (형식 준수)"),
                                        fieldWithPath("password").description("비밀번호 (8~20자)"),
                                        fieldWithPath("repeatPassword").description("비밀번호 확인 (비밀번호와 일치해야 함)"),
                                        fieldWithPath("nickname").description("닉네임 (2~10자)")
                                )
                                .responseFields(
                                        fieldWithPath("id").description("생성된 회원 ID")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("일반 회원가입 API - 실패 (비밀번호 불일치)")
    void signupFail_PasswordMismatch() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "fail@example.com",
                    "password": "password1234",
                    "repeatPassword": "password9999",
                    "nickname": "테스트"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andDo(document("auth-signup-fail-mismatch",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("일반 회원가입 실패 - 비밀번호 불일치")
                                .description("비밀번호와 비밀번호 확인 값이 다를 경우 회원가입에 실패합니다.")
                                .requestSchema(Schema.schema("SignupRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .build())
                ));
    }

    @Test
    @DisplayName("일반 회원가입 API - 실패 (입력값 유효성 검증)")
    void signupFail_InvalidInput() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "not-email-format",
                    "password": "123",
                    "repeatPassword": "123",
                    "nickname": ""
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors").isArray())
                .andDo(document("auth-signup-fail-validation",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("일반 회원가입 실패 - 유효성 검증")
                                .description("입력값이 제약조건(이메일 형식, 길이 등)을 위반할 경우 실패합니다.")
                                .requestSchema(Schema.schema("SignupRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(
                                        fieldWithPath("code").description("에러 코드"),
                                        fieldWithPath("message").description("에러 메시지"),
                                        fieldWithPath("errors[].field").description("문제가 발생한 필드명"),
                                        fieldWithPath("errors[].value").description("거부된 입력값"),
                                        fieldWithPath("errors[].reason").description("에러 상세 사유")
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
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // Body에 없어야 함
                .andExpect(cookie().exists("refresh_token")) // Cookie에 있어야 함
                .andExpect(cookie().httpOnly("refresh_token", true)) // HttpOnly 속성 확인
                .andDo(document("auth-login",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("일반 로그인")
                                .description("이메일과 비밀번호로 로그인하여 토큰을 발급받습니다. (Refresh Token은 HttpOnly 쿠키로 전달)")
                                .requestSchema(Schema.schema("LoginRequest"))
                                .responseSchema(Schema.schema("LoginResponse"))
                                .requestFields(
                                        fieldWithPath("email").description("이메일"),
                                        fieldWithPath("password").description("비밀번호")
                                )
                                .responseFields(
                                        // grantType과 refreshToken은 이제 Body에 없으므로 문서에서 제거
                                        fieldWithPath("accessToken").description("액세스 토큰")
                                )
                                .build())
                ));
    }

}