package wonjun.stiky.auth.controller.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class SignupResponse {

    private Long id;

    private SignupResponse(Long id) {
        this.id = id;
    }

    public static SignupResponse of(Long id) {
        return new SignupResponse(id);
    }

}
