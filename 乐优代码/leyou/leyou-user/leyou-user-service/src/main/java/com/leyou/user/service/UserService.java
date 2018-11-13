package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String VERIFY_PREFIX = "user:verify:";

    private static final Long VERIFY_EXPIRE = 5l;

    /**
     * 校验用户名和手机号是否可用
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUser(String data, Integer type) {

        User record = new User();
        switch (type) {
            case 1: // 校验用户名
                record.setUsername(data);
                break;
            case 2: // 校验手机号
                record.setPhone(data);
                break;
            default: // 参数不合法
                return null;
        }

        return this.userMapper.selectCount(record) == 0;
    }

    /**
     * 发送短信
     * @param phone
     */
    public void sendVerifyCode(String phone) {
        // 判断手机号是否为null
        if (StringUtils.isBlank(phone)) {
            return;
        }

        // 生成短信验证码
        String code = NumberUtils.generateCode(6);

        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        // 发送短信：发送消息到队列
        this.amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE", "sms.verifyCode", msg);

        // 保存短信验证码到redis
        this.redisTemplate.opsForValue().set(VERIFY_PREFIX + phone, code, VERIFY_EXPIRE, TimeUnit.MINUTES);
    }

    public Boolean register(User user, String code) {
        // 获取redis中的验证码
        String cacheCode = this.redisTemplate.opsForValue().get(VERIFY_PREFIX + user.getPhone());
        // 校验验证码
        if (!StringUtils.equals(cacheCode, code)){
            return null;
        }

        // 生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        // 对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        // 新增用户
        user.setId(null);
        user.setCreated(new Date());
        Boolean flag = this.userMapper.insertSelective(user) == 1;
        // 删除redis中的校验码
        if (flag) {
            this.redisTemplate.delete(VERIFY_PREFIX + user.getPhone());

            return true;
        }
        return false;
    }

    public User queryUser(String username, String password) {
        // 根据用户名查询用户信息
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);

        if (user == null){
            return null;
        }

        // 获取盐对用户输入的密码进行加密
        String passwd = CodecUtils.md5Hex(password, user.getSalt());

        // 判断密码是否正确
        if (StringUtils.equals(passwd, user.getPassword())){
            return user;
        }
        return null;
    }
}
