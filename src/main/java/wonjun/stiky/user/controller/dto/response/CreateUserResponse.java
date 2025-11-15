package wonjun.stiky.user.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateUserResponse {

    @Schema(description = "생성된 사용자의 PK", example = "1")
    private Long id;

    public static CreateUserResponse of(Long id) {
        return new CreateUserResponse(id);
    }

}
