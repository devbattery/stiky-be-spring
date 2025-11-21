package wonjun.stiky.auth.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    
    private String email;
    private String password;
    private String repeatPassword;
    private String nickname;

}
