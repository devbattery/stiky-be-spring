package wonjun.stiky.user.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateUserResponse {

    private Long id;

    public static CreateUserResponse of(Long id) {
        return new CreateUserResponse(id);
    }

}
