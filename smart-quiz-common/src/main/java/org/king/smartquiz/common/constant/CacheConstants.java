package org.king.smartquiz.common.constant;

/**
 * 缓存（Redis）Key 常量
 */
public class CacheConstants {

    /** 登录令牌前缀：login:token:{uuid} */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    /** 图形验证码前缀：captcha:{uuid} */
    public static final String CAPTCHA_KEY = "captcha:";

    /** 防重复提交前缀 */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /** 题目详情缓存前缀 */
    public static final String QUESTION_DETAIL_KEY = "question:detail:";

    /** 公告列表缓存 */
    public static final String NOTICE_LIST_KEY = "notice:list";

    /** AI 调用统计前缀（按日期） */
    public static final String AI_STAT_KEY = "ai:stat:";

    /** 接口限流前缀 */
    public static final String RATE_LIMIT_KEY = "rate_limit:";
}
