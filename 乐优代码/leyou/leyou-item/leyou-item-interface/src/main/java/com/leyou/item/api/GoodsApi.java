package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GoodsApi {

    @GetMapping("spu/page")
    public PageResult<SpuBo> querySpuBoListByPage(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "saleable", defaultValue = "true") Boolean saleable,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows
    );

    /**
     * 根据spuId查询spuDetail
     *
     * @param id
     * @return
     */
    @GetMapping("spu/detail/{id}")
    public SpuDetail querySpuDetailBySpuId(@PathVariable("id") Long id);

    /**
     * 根据spuid查询sku集合
     *
     * @param id
     * @return
     */
    @GetMapping("sku/list")
    public List<Sku> querykSkuListBySpuId(@RequestParam("id") Long id);

    @GetMapping("spu/{id}")
    public Spu querySpuById(@PathVariable("id")Long id);

    @GetMapping("sku/{id}")
    public Sku querySkuById(@PathVariable("id")Long id);
}
