package org.king.smartquiz.common.constant;

/**
 * 令牌相关常量
 */
public class TokenConstants {

    /** 请求头名称 */
    public static final String AUTHENTICATION = "Authorization";

    /** 令牌前缀 */
    public static final String PREFIX = "Bearer ";

    /** JWT 中用户ID的 claim 名 */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT 中用户名的 claim 名 */
    public static final String CLAIM_USERNAME = "username";

    /** JWT 中角色的 claim 名 */
    public static final String CLAIM_ROLE = "role";

    /** JWT 中关联 Redis 登录态的 uuid claim 名 */
    public static final String CLAIM_UUID = "uuid";
}
