package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.service.UserService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties properties;

    @PostMapping("accredit")
    public ResponseEntity<Void> accredit(
            @RequestParam("username")String username,
            @RequestParam("password")String password,
            HttpServletRequest request, HttpServletResponse response
    ){
        try {
            String token = this.userService.accredit(username, password);
            // 放入cookie中
            if (StringUtils.isBlank(token)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire()*60);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
            @CookieValue(value = "TT_TOKEN", required = false)String token,
            HttpServletRequest request, HttpServletResponse response
    ){

        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());

            // 判断userInfo是否为null
            if (userInfo == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 刷新登陆状态
            // 刷新jwt的有效时间
            token = JwtUtils.generateToken(userInfo, this.properties.getPrivateKey(), this.properties.getExpire());
            // 刷新cookie有效时间
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
