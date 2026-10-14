package cn.swust.bokesys.common.util;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 图形验证码生成，配合《概要设计》6.2.2 后台登录页的「图形验证码 + 注册新验证码」。
 *
 * <p>用 JDK 自带的 java.awt 直接绘制，不引入 kaptcha 之类年久失修的第三方库。
 * Spring Boot 默认以 headless 模式运行，BufferedImage 在无显示设备的环境下同样可用。
 *
 * <p>题型采用「算术式」：题面形如 {@code 3 + 5 = ?}，答案是运算结果。
 * 相比随机字母，数字更易辨认，也避免了 0/O、1/l 的混淆。
 */
public final class CaptchaUtil {

    /** 验证码题面与答案 */
    public record Captcha(String code, BufferedImage image) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int WIDTH = 132;
    private static final int HEIGHT = 44;
    private static final String[] FONT_NAMES = {"Arial", "Verdana", "Tahoma", "SansSerif"};
    private static final Color[] COLORS = {
            new Color(0x2563EB), new Color(0x1F2937), new Color(0x0F766E),
            new Color(0xB45309), new Color(0x7C3AED)
    };

    private CaptchaUtil() {
    }

    /**
     * 生成一个算术验证码。
     *
     * <p>加法与减法各半，减法始终用大数减小数，保证答案非负。
     */
    public static Captcha generate() {
        int a = RANDOM.nextInt(9) + 1;
        int b = RANDOM.nextInt(9) + 1;
        boolean addition = RANDOM.nextBoolean();

        int answer;
        String text;
        if (addition) {
            answer = a + b;
            text = a + " + " + b + " = ?";
        } else {
            int big = Math.max(a, b);
            int small = Math.min(a, b);
            answer = big - small;
            text = big + " - " + small + " = ?";
        }
        return new Captcha(String.valueOf(answer), draw(text));
    }

    /** 把验证码图片写成 PNG 字节数组 */
    public static byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    private static BufferedImage draw(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景：浅灰底 + 少量噪点
            g.setColor(new Color(0xF5F7FA));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            for (int i = 0; i < 60; i++) {
                g.setColor(new Color(RANDOM.nextInt(0xCCCCCC)));
                g.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
            }

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(COLORS[RANDOM.nextInt(COLORS.length)]);
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }

            // 题面：逐字符轻微旋转与上下抖动
            g.setFont(new Font(FONT_NAMES[RANDOM.nextInt(FONT_NAMES.length)], Font.BOLD, 22));
            int charWidth = (WIDTH - 16) / Math.max(text.length(), 1);
            int x = 10;
            for (char c : text.toCharArray()) {
                AffineTransform saved = g.getTransform();
                double angle = (RANDOM.nextDouble() - 0.5) * 0.5;
                int dy = RANDOM.nextInt(7) - 3;
                g.rotate(angle, x, HEIGHT / 2.0);
                g.setColor(COLORS[RANDOM.nextInt(COLORS.length)]);
                g.drawString(String.valueOf(c), x, HEIGHT / 2 + 8 + dy);
                g.setTransform(saved);
                x += charWidth;
            }
            return image;
        } finally {
            g.dispose();
        }
    }
}
