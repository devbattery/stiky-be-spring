package wonjun.stiky.acceptance;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserAcceptanceTest extends AcceptanceTestBase {

    @Test
    @DisplayName("사용자 생성 API")
    void createUser() throws Exception {
        String requestBody = """
                {
                    "name": "홍길동",
                    "email": "hong@example.com",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andDo(document("user-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("사용자 생성")
                                .description("새로운 사용자를 생성합니다.")
                                .requestSchema(Schema.schema("UserCreateRequest"))
                                .responseSchema(Schema.schema("UserCreateResponse"))
                                .requestFields(
                                        fieldWithPath("name").description("사용자 이름"),
                                        fieldWithPath("email").description("사용자 이메일"),
                                        fieldWithPath("age").description("사용자 나이")
                                )
                                .responseFields(
                                        fieldWithPath("id").description("생성된 사용자의 PK")
                                )
                                .build())
                ));
    }

}