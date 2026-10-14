package cn.swust.bokesys.common.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 评论敏感词过滤，对应《概要设计》第 2 章「安全性」：
 * 系统对评论内容执行基础的敏感词过滤，命中敏感词的评论将被拒绝提交。
 *
 * <p>词表内置一份最小集合，够演示与验收即可。
 */
public final class SensitiveWordUtil {

    private static final Set<String> WORDS = new LinkedHashSet<>(Arrays.asList(
            "赌博", "博彩", "色情", "暴力", "诈骗",
            "代开发票", "办证", "枪支", "毒品",
            "加微信", "加QQ", "刷单", "兼职日结"
    ));

    private SensitiveWordUtil() {
    }

    /**
     * 返回文本中命中的第一个敏感词；未命中返回 {@code null}。
     *
     * @param text 待检查文本
     */
    public static String firstHit(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        for (String word : WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return word;
            }
        }
        return null;
    }

    /** 是否命中敏感词 */
    public static boolean contains(String text) {
        return firstHit(text) != null;
    }
}
