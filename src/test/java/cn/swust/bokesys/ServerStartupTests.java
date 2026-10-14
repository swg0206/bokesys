package cn.swust.bokesys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实端口冒烟测试：用内嵌 Tomcat 起一个真实 HTTP 服务，再从进程外发请求。
 *
 * <p>与 {@link WebLayerTests} 的区别：MockMvc 走的是模拟的 Servlet 环境，
 * 绕过了内嵌容器本身。这里补上那一层，确认「打好的包真的能起服务、真的能响应」，
 * 覆盖 Tomcat、过滤器链与真正的网络返回。
 *
 * <p>用 JDK 自带的 java.net.http.HttpClient 而不是 TestRestTemplate：
 * 后者在 Spring Boot 4 中迁到了独立构件，且会额外拖入 spring-boot-restclient 模块，
 * 为了一个冒烟测试不值得再引一个依赖。JDK 客户端也更接近「真实外部客户端」。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("真实端口启动冒烟测试")
class ServerStartupTests {

    /** 不自动跟随重定向，这样可以精确断言拦截器返回的 302 与 Location */
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("内嵌 Tomcat 真实提供首页、静态资源与后台拦截")
    void servesRealHttpRequests() throws Exception {
        HttpResponse<String> home = get("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("笔迹博客"), "首页应渲染出站点名称");
        assertTrue(home.body().contains("Spring Boot 约定优于配置的实践总结"),
                "首页应包含已发布文章");

        // 静态资源由 Spring Boot 默认映射直接提供
        HttpResponse<String> css = get("/css/style.css");
        assertEquals(200, css.statusCode());
        assertTrue(css.body().contains("--primary: #2563eb"), "前台样式表应能被访问到");

        // 未登录访问后台应被 LoginInterceptor 重定向到登录页。
        // Tomcat 会把 sendRedirect 的相对路径规范化成绝对 URL，因此这里只断言路径部分。
        HttpResponse<String> admin = get("/admin/dashboard");
        assertEquals(302, admin.statusCode(), "未登录访问后台应返回 302");
        String location = admin.headers().firstValue("Location").orElse("");
        assertTrue(location.endsWith("/admin/login"),
                "应重定向到后台登录页，实际 Location = " + location);

        // 验证码接口可访问且为 PNG
        HttpResponse<byte[]> captcha = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/admin/captcha"))
                        .timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, captcha.statusCode());
        assertTrue(captcha.body().length > 100, "验证码应返回一张真实图片");
    }
}
