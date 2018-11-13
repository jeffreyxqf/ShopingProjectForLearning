package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    private static final String KEY_PREFIX = "user:cart:";

    public void saveCart(Cart cart) {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();

        // 组装redis中外层的key
        String key = KEY_PREFIX + userInfo.getId();
        // 查询该用户购物车信息
        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(key);

        // 保存页面传递过来的数量
        Integer num = cart.getNum();

        // 判断该记录在不在购物车中
        if (hashOperations.hasKey(cart.getSkuId().toString())) {
            // 在，更新数量
            String jsonData = hashOperations.get(cart.getSkuId().toString()).toString();
            cart = JsonUtils.parse(jsonData, Cart.class);
            cart.setNum(cart.getNum() + num);
        } else {
            // 不在，新增记录
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setUserId(userInfo.getId());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            cart.setPrice(sku.getPrice());
        }
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));
    }

    public List<Cart> queryCarts() {
        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        // 组装redis中外层的key
        String key = KEY_PREFIX + userInfo.getId();
        // 判断该用户有没有购物车
        if (!this.redisTemplate.hasKey(key)){
            return null;
        }
        // 查询该用户购物车信息
        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(key);
        List<Object> jsonDatas = hashOperations.values();
        // 判断集合是否为空
        if (CollectionUtils.isEmpty(jsonDatas)) {
            return null;
        }
        // 把购物车的字符串集合转化成List<Cart>
        return jsonDatas.stream().map(jsonData -> {
            return JsonUtils.parse(jsonData.toString(), Cart.class);
        }).collect(Collectors.toList());
    }
}
