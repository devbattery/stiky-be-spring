package wonjun.stiky.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateUserRequest {

    @Schema(description = "사용자 이름", example = "정원준")
    private String name;

    @Schema(description = "사용자 이메일", example = "wonjun@stiky.com")
    private String email;

    @Schema(description = "사용자 나이", example = "25")
    private int age;

}
