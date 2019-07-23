package me.zhyd.oauth.request;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.*;
import me.zhyd.oauth.utils.UrlBuilder;

import java.text.MessageFormat;

/**
 * 小米登录
 *
 * @author yangkai.shen (https://xkcoding.com)
 * @version 1.5
 * @since 1.5
 */
@Slf4j
public class AuthMiRequest extends AuthDefaultRequest {
    private static final String PREFIX = "&&&START&&&";

    public AuthMiRequest(AuthConfig config) {
        super(config, AuthSource.MI);
    }

    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        return getToken(accessTokenUrl(authCallback.getCode()));
    }

    private AuthToken getToken(String accessTokenUrl) {
        HttpResponse response = HttpRequest.get(accessTokenUrl).execute();
        String jsonStr = StrUtil.replace(response.body(), PREFIX, StrUtil.EMPTY);
        JSONObject accessTokenObject = JSONObject.parseObject(jsonStr);

        if (accessTokenObject.containsKey("error")) {
            throw new AuthException(accessTokenObject.getString("error_description"));
        }

        return AuthToken.builder()
            .accessToken(accessTokenObject.getString("access_token"))
            .expireIn(accessTokenObject.getIntValue("expires_in"))
            .scope(accessTokenObject.getString("scope"))
            .tokenType(accessTokenObject.getString("token_type"))
            .refreshToken(accessTokenObject.getString("refresh_token"))
            .openId(accessTokenObject.getString("openId"))
            .macAlgorithm(accessTokenObject.getString("mac_algorithm"))
            .macKey(accessTokenObject.getString("mac_key"))
            .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        // 获取用户信息
        HttpResponse userResponse = doGetUserInfo(authToken);

        JSONObject userProfile = JSONObject.parseObject(userResponse.body());
        if ("error".equalsIgnoreCase(userProfile.getString("result"))) {
            throw new AuthException(userProfile.getString("description"));
        }

        JSONObject user = userProfile.getJSONObject("data");

        AuthUser authUser = AuthUser.builder()
            .uuid(authToken.getOpenId())
            .username(user.getString("miliaoNick"))
            .nickname(user.getString("miliaoNick"))
            .avatar(user.getString("miliaoIcon"))
            .email(user.getString("mail"))
            .gender(AuthUserGender.UNKNOWN)
            .token(authToken)
            .source(source)
            .build();

        // 获取用户邮箱手机号等信息
        String emailPhoneUrl = MessageFormat.format("{0}?clientId={1}&token={2}", "https://open.account.xiaomi.com/user/phoneAndEmail", config
            .getClientId(), authToken.getAccessToken());

        HttpResponse emailResponse = HttpRequest.get(emailPhoneUrl).execute();
        JSONObject userEmailPhone = JSONObject.parseObject(emailResponse.body());
        if (!"error".equalsIgnoreCase(userEmailPhone.getString("result"))) {
            JSONObject emailPhone = userEmailPhone.getJSONObject("data");
            authUser.setEmail(emailPhone.getString("email"));
        } else {
            log.warn("小米开发平台暂时不对外开放用户手机及邮箱信息的获取");
        }

        return authUser;
    }

    /**
     * 刷新access token （续期）
     *
     * @param authToken 登录成功后返回的Token信息
     * @return AuthResponse
     */
    @Override
    public AuthResponse refresh(AuthToken authToken) {
        return AuthResponse.builder()
            .code(AuthResponseStatus.SUCCESS.getCode())
            .data(getToken(refreshTokenUrl(authToken.getRefreshToken())))
            .build();
    }

    /**
     * 返回认证url，可自行跳转页面
     *
     * @return 返回授权地址
     */
    @Override
    public String authorize() {
        return UrlBuilder.fromBaseUrl(source.authorize())
            .queryParam("response_type", "code")
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri())
            .queryParam("state", getRealState(config.getState()))
            .queryParam("scope", "user/profile%20user/openIdV2%20user/phoneAndEmail")
            .queryParam("skip_confirm", "false")
            .build();
    }

    /**
     * 返回获取userInfo的url
     *
     * @param authToken 用户授权后的token
     * @return 返回获取userInfo的url
     */
    @Override
    protected String userInfoUrl(AuthToken authToken) {
        return UrlBuilder.fromBaseUrl(source.userInfo())
            .queryParam("clientId", config.getClientId())
            .queryParam("token", authToken.getAccessToken())
            .build();
    }
}
