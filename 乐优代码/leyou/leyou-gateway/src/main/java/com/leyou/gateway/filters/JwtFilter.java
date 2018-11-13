package com.leyou.gateway.filters;

import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
@Component
public class JwtFilter extends ZuulFilter {

    @Autowired
    private JwtProperties properties;

    @Autowired
    private FilterProperties filterProperties;

    /**
     * 过滤器的类型：pre route post error
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        // 获取zuul网关的上下文对象
        RequestContext context = RequestContext.getCurrentContext();
        // 获取request对象
        HttpServletRequest request = context.getRequest();
        // 获取当前的请求路径
        StringBuffer url = request.getRequestURL();

        // 如果当前的请求路径包含白名单中的任一路径
        for (String path : this.filterProperties.getAllowPaths()) {
            if (StringUtils.contains(url, path)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Object run() throws ZuulException {

        // 获取zuul网关的上下文对象
        RequestContext context = RequestContext.getCurrentContext();
        // 获取request对象
        HttpServletRequest request = context.getRequest();
        // 获取jwt
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        // 解析token
        try {
            JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            // 不转发请求
            context.setSendZuulResponse(false);
            // 设置响应状态码
            context.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        }
        // 什么都不干，也就是放行
        return null;
    }
}
