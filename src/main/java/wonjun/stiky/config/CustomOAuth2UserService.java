package wonjun.stiky.config;

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
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // google "sub"
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());

        update(attributes);
        return oAuth2User;
    }

    private void update(OAuthAttributes attributes) {
        try {
            Member foundMember = memberQueryService.fetchByEmail(attributes.getEmail());
            foundMember.updateSocialInfo(attributes.getProvider(), attributes.getNameAttributeKey());
            memberQueryService.save(foundMember);
        } catch (RuntimeException e) { // TODO: fetchByEmail의 customException으로 변경
            Member member = Member.builder()
                    .email(attributes.getEmail())
                    .nickname(attributes.getName()) // TODO: 사용자가 온보딩 때 바꿀 수 있도록
                    .role("ROLE_USER")
                    .provider(attributes.getProvider())
                    .providerId(attributes.getNameAttributeKey())
                    .build();
            memberQueryService.save(member);
        }
    }

}
