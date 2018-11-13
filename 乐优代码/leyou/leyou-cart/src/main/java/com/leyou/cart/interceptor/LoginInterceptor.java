package com.leyou.cart.interceptor;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {

    // 声明一个线程变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 前置方法：在handler方法执行之前执行
     * false-被拦截
     * true-放行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取token信息
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        // 判断token是否为空
        if (StringUtils.isBlank(token)){
            // 没有登录，跳转到登录页
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=http://www.leyou.com/cart.html");
            return false;
        }

        // 解析jwt
        UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
        if (userInfo == null) {
            response.sendRedirect("http://www.leyou.com/login.html?returnUrl=http://www.leyou.com/cart.html");
            return false;
        }

        THREAD_LOCAL.set(userInfo);

        return true;
    }

    /**
     * 获取线程变量中的参数
     * @return
     */
    public static UserInfo get(){
        return THREAD_LOCAL.get();
    }

    /**
     * 完成方法：在视图渲染完成之后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须要释放资源，使用的是tomcat线程池，业务逻辑处理完成之后，线程并没有结束，还回到线程池中了
        THREAD_LOCAL.remove();
    }
}
