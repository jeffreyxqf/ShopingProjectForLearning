package com.leyou.gateway.filters;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class LeyouCorsConfiguration {

    @Bean
    public CorsFilter corsFilter(){

        // cors跨域的配置对象
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许跨域的具体域名，因为将来可能要携带cookie，所以使用具体的域名，而不是用*
        configuration.addAllowedOrigin("http://manage.leyou.com");
        configuration.addAllowedOrigin("http://www.leyou.com");
        // 允许跨域的方法，*代表所有方法：get post put delete
        configuration.addAllowedMethod("*");
        // 允许携带的头信息
        configuration.addAllowedHeader("*");
        // 允许携带cookie信息
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(0l);

        // cors配置源
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", configuration);

        // 初始化corsFilter
        return new CorsFilter(configSource);
    }
}
