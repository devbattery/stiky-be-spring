package wonjun.stiky.auth.config;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.service.MemberQueryService;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberQueryService memberQueryService;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // google "sub"
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());

        process(attributes);
        return oAuth2User;
    }

    private void process(OAuthAttributes attributes) {
        memberQueryService.fetchByEmailOpt(attributes.getEmail())
                .ifPresentOrElse(
                        member -> {
                            member.updateSocialInfo(attributes.getProvider(), attributes.getNameAttributeKey());
                            memberQueryService.save(member);
                        },
                        () -> {
                            Member member = makeMember(attributes);
                            memberQueryService.save(member);
                        }
                );
    }

    private Member makeMember(OAuthAttributes attributes) {
        return Member.builder()
                .email(attributes.getEmail())
                .nickname(attributes.getName())
                .password(UUID.randomUUID().toString()) // 더미 패스워드
                .role("ROLE_USER")
                .provider(attributes.getProvider())
                .providerId(attributes.getNameAttributeKey())
                .build();
    }

}
