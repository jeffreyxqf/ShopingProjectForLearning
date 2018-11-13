package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecClient;
import com.leyou.item.pojo.*;
import org.apache.catalina.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    public Map<String, Object> loadGoods(Long spuId){

        Map<String, Object> map = new HashMap<>();

        // 查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);

        // 查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);

        // 查询spu下的所有sku
        List<Sku> skus = this.goodsClient.querykSkuListBySpuId(spuId);

        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询分类名称
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("id", cids.get(i));
            map1.put("name", names.get(i));
            categories.add(map1);
        }

        // 查询规格参数组，及组下的参数
        List<SpecGroup> groups = this.specClient.queryGroupWithParamByCid(spu.getCid3());

        // 查询特殊的规格参数
        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), false, null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });

        map.put("spu", spu);
        map.put("spuDetail", spuDetail);
        map.put("skus", skus);
        map.put("brand", brand);
        // List<Map<String, Object>>
        map.put("categories", categories);
        map.put("groups", groups);
        // 特殊的规格参数：Map<paramId, paramName>
        map.put("paramMap", paramMap);

        return map;
    }

}
