package wonjun.stiky.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                        requestFields(
                                fieldWithPath("name").description("사용자 이름"),
                                fieldWithPath("email").description("사용자 이메일"),
                                fieldWithPath("age").description("사용자 나이")
                        ),
                        responseFields(
                                fieldWithPath("id").description("사용자 ID")
                        )
                ));
    }
//
//    @Test
//    @DisplayName("사용자 조회 API")
//    void getUser() throws Exception {
//        mockMvc.perform(get("/api/users/{id}", 1)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(document("user-get",
//                        pathParameters(
//                                parameterWithName("id").description("사용자 ID")
//                        ),
//                        responseFields(
//                                fieldWithPath("id").description("사용자 ID"),
//                                fieldWithPath("name").description("사용자 이름"),
//                                fieldWithPath("email").description("사용자 이메일"),
//                                fieldWithPath("age").description("사용자 나이")
//                        )
//                ));
//    }
//
//    @Test
//    @DisplayName("사용자 목록 조회 API")
//    void getUsers() throws Exception {
//        mockMvc.perform(get("/api/users")
//                        .param("page", "0")
//                        .param("size", "10")
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(document("user-list",
//                        queryParameters(
//                                parameterWithName("page").description("페이지 번호 (0부터 시작)"),
//                                parameterWithName("size").description("페이지 크기")
//                        ),
//                        responseFields(
//                                fieldWithPath("[].id").description("사용자 ID"),
//                                fieldWithPath("[].name").description("사용자 이름"),
//                                fieldWithPath("[].email").description("사용자 이메일"),
//                                fieldWithPath("[].age").description("사용자 나이")
//                        )
//                ));
//    }
//
//    @Test
//    @DisplayName("사용자 수정 API")
//    void updateUser() throws Exception {
//        String requestBody = """
//                {
//                    "name": "김철수",
//                    "email": "kim@example.com",
//                    "age": 30
//                }
//                """;
//
//        mockMvc.perform(put("/api/users/{id}", 1)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andDo(document("user-update",
//                        pathParameters(
//                                parameterWithName("id").description("사용자 ID")
//                        ),
//                        requestFields(
//                                fieldWithPath("name").description("수정할 사용자 이름"),
//                                fieldWithPath("email").description("수정할 사용자 이메일"),
//                                fieldWithPath("age").description("수정할 사용자 나이")
//                        ),
//                        responseFields(
//                                fieldWithPath("id").description("사용자 ID"),
//                                fieldWithPath("name").description("사용자 이름"),
//                                fieldWithPath("email").description("사용자 이메일"),
//                                fieldWithPath("age").description("사용자 나이")
//                        )
//                ));
//    }
//
//    @Test
//    @DisplayName("사용자 삭제 API")
//    void deleteUser() throws Exception {
//        mockMvc.perform(delete("/api/users/{id}", 1))
//                .andExpect(status().isNoContent())
//                .andDo(document("user-delete",
//                        pathParameters(
//                                parameterWithName("id").description("삭제할 사용자 ID")
//                        )
//                ));
//    }

}