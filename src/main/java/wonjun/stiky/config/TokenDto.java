package wonjun.stiky.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenDto {

    private String grantType;
    private String accessToken;
    private String refreshToken;

    private TokenDto(String accessToken, String refreshToken) {
        this.grantType = "Bearer";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static TokenDto from(String accessToken, String refreshToken) {
        return new TokenDto(accessToken, refreshToken);
    }

}
