package cn.swust.bokesys.common.util;

import java.util.regex.Pattern;

/**
 * 文本处理小工具：摘要截取与 Markdown 源文的纯文本化。
 */
public final class TextUtil {

    /** 连续空白字符 */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextUtil() {
    }

    /**
     * 由正文生成列表页展示用的摘要：去掉 Markdown 标记、压缩空白后按字数截断。
     *
     * @param content 正文（Markdown 源文）
     * @param max     最大字符数
     */
    public static String summarize(String content, int max) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String plain = toPlainText(content);
        if (plain.length() <= max) {
            return plain;
        }
        return plain.substring(0, max) + "…";
    }

    /**
     * 把 Markdown 源文压成一行纯文本，用于摘要与 meta description。
     */
    public static String toPlainText(String markdown) {
        if (markdown == null) {
            return "";
        }
        String text = markdown
                // 代码块围栏
                .replaceAll("(?m)^```.*$", " ")
                // 图片与链接：保留可读文字
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                // 标题、引用、列表符号
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s{0,3}>\\s?", "")
                .replaceAll("(?m)^\\s{0,3}[-*+]\\s+", "")
                .replaceAll("(?m)^\\s{0,3}\\d+\\.\\s+", "")
                // 强调与行内代码
                .replaceAll("[*_`~]+", "");
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }
}
