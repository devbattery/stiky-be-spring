package wonjun.stiky.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.service.MemberQueryService;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberQueryService memberQueryService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // google "sub"

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = oAuth2User.getName(); // google "sub"

        try {
            Member foundMember = memberQueryService.fetchByEmail(email);
            foundMember.updateSocialInfo(provider, providerId);
            memberQueryService.save(foundMember);
        } catch (RuntimeException e) { // TODO: fetchByEmail의 customException으로 변경
            Member member = Member.builder()
                    .email(email)
                    .nickname(name) // TODO: 사용자가 온보딩 때 바꿀 수 있도록
                    .role("ROLE_USER")
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            memberQueryService.save(member);
        }

        return oAuth2User;
    }

}
