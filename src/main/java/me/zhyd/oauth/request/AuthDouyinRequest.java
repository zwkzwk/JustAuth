package me.zhyd.oauth.request;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.*;
import me.zhyd.oauth.utils.UrlBuilder;


/**
 * 抖音登录
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0
 * @since 1.8
 */
public class AuthDouyinRequest extends AuthDefaultRequest {

    public AuthDouyinRequest(AuthConfig config) {
        super(config, AuthSource.DOUYIN);
    }

    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        return this.getToken(accessTokenUrl(authCallback.getCode()));
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        HttpResponse response = doGetUserInfo(authToken);
        JSONObject userInfoObject = JSONObject.parseObject(response.body());
        this.checkResponse(userInfoObject);
        return AuthUser.builder()
            .uuid(userInfoObject.getString("union_id"))
            .username(userInfoObject.getString("nickname"))
            .nickname(userInfoObject.getString("nickname"))
            .avatar(userInfoObject.getString("avatar"))
            .remark(userInfoObject.getString("description"))
            .gender(AuthUserGender.UNKNOWN)
            .token(authToken)
            .source(source)
            .build();
    }

    @Override
    public AuthResponse refresh(AuthToken oldToken) {
        return AuthResponse.builder()
            .code(AuthResponseStatus.SUCCESS.getCode())
            .data(getToken(refreshTokenUrl(oldToken.getRefreshToken())))
            .build();
    }

    /**
     * 检查响应内容是否正确
     *
     * @param object 请求响应内容
     */
    private void checkResponse(JSONObject object) {
        String message = object.getString("message");
        JSONObject data = object.getJSONObject("data");
        int errorCode = data.getIntValue("error_code");
        if ("error".equals(message) || errorCode != 0) {
            throw new AuthException(errorCode, data.getString("description"));
        }
    }

    /**
     * 获取token，适用于获取access_token和刷新token
     *
     * @param accessTokenUrl 实际请求token的地址
     * @return token对象
     */
    private AuthToken getToken(String accessTokenUrl) {
        HttpResponse response = HttpRequest.post(accessTokenUrl).execute();
        String accessTokenStr = response.body();
        JSONObject object = JSONObject.parseObject(accessTokenStr);
        this.checkResponse(object);
        return AuthToken.builder()
            .accessToken(object.getString("access_token"))
            .openId(object.getString("open_id"))
            .expireIn(object.getIntValue("expires_in"))
            .refreshToken(object.getString("refresh_token"))
            .scope(object.getString("scope"))
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
            .queryParam("client_key", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri())
            .queryParam("state", getRealState(config.getState()))
            .queryParam("scope", "user_info")
            .build();
    }

    /**
     * 返回获取accessToken的url
     *
     * @param code oauth的授权码
     * @return 返回获取accessToken的url
     */
    @Override
    protected String accessTokenUrl(String code) {
        return UrlBuilder.fromBaseUrl(source.accessToken())
            .queryParam("code", code)
            .queryParam("client_key", config.getClientId())
            .queryParam("client_secret", config.getClientSecret())
            .queryParam("grant_type", "authorization_code")
            .build();
    }

    /**
     * 返回获取userInfo的url
     *
     * @param authToken oauth返回的token
     * @return 返回获取userInfo的url
     */
    @Override
    protected String userInfoUrl(AuthToken authToken) {
        return UrlBuilder.fromBaseUrl(source.userInfo())
            .queryParam("access_token", authToken.getAccessToken())
            .queryParam("open_id", authToken.getOpenId())
            .build();
    }

    /**
     * 返回获取accessToken的url
     *
     * @param refreshToken oauth返回的refreshtoken
     * @return 返回获取accessToken的url
     */
    @Override
    protected String refreshTokenUrl(String refreshToken) {
        return UrlBuilder.fromBaseUrl(source.refresh())
            .queryParam("client_key", config.getClientId())
            .queryParam("refresh_token", refreshToken)
            .queryParam("grant_type", "refresh_token")
            .build();
    }
}
