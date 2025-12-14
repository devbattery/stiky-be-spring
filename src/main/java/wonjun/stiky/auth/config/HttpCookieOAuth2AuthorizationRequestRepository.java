package wonjun.stiky.auth.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteAuthorizationRequestCookies(request, response);
            return;
        }

        addCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            addCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    public void deleteAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return Optional.ofNullable(cookie.getValue());
            }
        }

        return Optional.empty();
    }

    private void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
                           int maxAge) {
        if (response == null) {
            return;
        }

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge);

        applyCookieSecurity(builder, request);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        if (response == null) {
            return;
        }

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0);

        applyCookieSecurity(builder, request);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    private OAuth2AuthorizationRequest deserialize(String serializedRequest) {
        byte[] bytes = Base64.getUrlDecoder().decode(serializedRequest);
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
    }

    private void applyCookieSecurity(ResponseCookie.ResponseCookieBuilder builder, HttpServletRequest request) {
        if (supportsCookieDomain(request)) {
            builder.domain(cookieDomain)
                    .secure(true)
                    .sameSite("None");
            return;
        }

        if (isHttpsRequest(request)) {
            builder.secure(true)
                    .sameSite("None");
        } else {
            builder.secure(false)
                    .sameSite("Lax");
        }
    }

    private boolean isHttpsRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

    private boolean supportsCookieDomain(HttpServletRequest request) {
        if (cookieDomain == null || cookieDomain.isBlank() || request == null) {
            return false;
        }

        String normalizedDomain = cookieDomain.startsWith(".") ? cookieDomain.substring(1) : cookieDomain;
        String serverName = request.getServerName();

        return serverName.equals(normalizedDomain) || serverName.endsWith("." + normalizedDomain);
    }
}
