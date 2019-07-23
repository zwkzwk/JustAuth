package me.zhyd.oauth.config;

import lombok.*;

/**
 * JustAuth配置类
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0
 * @since 1.8
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthConfig {

    /**
     * 客户端id：对应个平台的appKey
     */
    private String clientId;

    /**
     * 客户端Secret：对应个平台的appSecret
     */
    private String clientSecret;

    /**
     * 登录成功后的回调地址
     */
    private String redirectUri;

    /**
     * 支付宝公钥：当选择支付宝登录时，该值可用
     */
    private String alipayPublicKey;

    /**
     * 是否需要申请unionid，目前只针对qq登录
     * 注：qq授权登录时，获取unionid需要单独发送邮件申请权限。如果个人开发者账号中申请了该权限，可以将该值置为true，在获取openId时就会同步获取unionId
     * 参考链接：http://wiki.connect.qq.com/unionid%E4%BB%8B%E7%BB%8D
     * <p>
     * 1.7.1版本新增参数
     */
    private boolean unionId;

    /**
     * 一个神奇的参数，最好使用随机的不可测的内容，可以用来防止CSRF攻击
     * <p>
     * 1.8.0版本新增参数
     */
    private String state;

    /**
     * Stack Overflow Key
     * <p>
     * 1.9.0版本新增参数
     */
    private String stackOverflowKey;
}
