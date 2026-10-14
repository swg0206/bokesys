package cn.swust.bokesys.config;

import cn.swust.bokesys.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring MVC 配置：注册登录拦截器、映射上传目录的静态访问路径。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    /**
     * 拦截 /admin/**，放行登录页与验证码接口。
     *
     * <p>若不放行 /admin/login，登录页自身也会被拦截，形成无限重定向。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/admin/captcha",
                        "/admin/logout",
                        "/admin/css/**",
                        "/admin/js/**",
                        "/admin/img/**");
    }

    /**
     * 把本地上传目录挂到 /uploads/** 上，使封面上传后能直接被浏览器访问。
     *
     * <p>用绝对路径，避免受进程工作目录影响。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        // 目录不存在时 toUri() 不会带结尾斜杠，Spring 会自行补上并打一条 WARN，
        // 这里主动补齐，保持启动日志干净
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
