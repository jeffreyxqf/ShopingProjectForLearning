package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import com.netflix.discovery.converters.Auto;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AmqpTemplate amqpTemplate;
    /**
     * 根据条件查询spu的信息（分类名称，品牌的名称）
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<SpuBo> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows) {

        Example example = new Example(Spu.class);
        // 添加模糊查询
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)){
            criteria.andLike("title", "%" + key + "%");
        }

        // 添加上架下架的过滤条件
        criteria.andEqualTo("saleable", saleable);

        // 添加分页查询条件
        PageHelper.startPage(page, rows);

        // 查询spu
        List<Spu> spus = this.spuMapper.selectByExample(example);

        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        // 处理一个数组，返回一个新的数组。遍历每一个元素用箭头函数来进行处理，函数的返回值放入新的数组
        List<SpuBo> spuBoList = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu, spuBo);

            // 查询品牌名称
            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());

            // 查询分类的名称
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "/"));

            return spuBo;
        }).collect(Collectors.toList());

        return new PageResult<>(spuBoList, pageInfo.getTotal());
    }

    /**
     * 新增商品
     * @param spuBo
     */
    public void saveGoods(SpuBo spuBo) {

        // 新增spu
        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        this.spuMapper.insertSelective(spuBo);

        // 新增spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        this.spuDetailMapper.insertSelective(spuDetail);
        // 新增sku和库存
        saveSkuAndStock(spuBo);

        sendMsg("insert", spuBo.getId());
    }

    private void sendMsg(String type, Long spuId) {
        try {
            this.amqpTemplate.convertAndSend("item." + type, spuId);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        // 新增sku
        spuBo.getSkus().forEach(sku -> {
            sku.setId(null);
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);

            // 新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.insertSelective(stock);
        });
    }

    /**
     * 根据spuid查询spuDetail
     * @param id
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long id) {

        return this.spuDetailMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据spuid查询sku集合
     * @param id
     * @return
     */
    public List<Sku> querySkuListBySpuId(Long id) {
        Sku sku = new Sku();
        sku.setSpuId(id);
        List<Sku> skus = this.skuMapper.select(sku);
        skus.forEach(sku1 -> {
            Stock stock = this.stockMapper.selectByPrimaryKey(sku1.getId());
            sku1.setStock(stock.getStock());
        });
        return skus;
    }

    /**
     * 更新商品数据
     * @param spuBo
     * @return
     */
    public void updateGoods(SpuBo spuBo) {
        // 删除stock
        /// 先查询spu下的sku，收集skuid
        Sku record = new Sku();
        record.setSpuId(spuBo.getId());
        // 查询该spu下的所有sku
        List<Sku> skus = this.skuMapper.select(record);
        // 收集所有sku的id
        List<Long> skuIds = skus.stream().map(sku -> sku.getId()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(skuIds)){
            Example example = new Example(Stock.class);
            example.createCriteria().andIn("skuId", skuIds);
            this.stockMapper.deleteByExample(example);
        }

        // 删除sku，根据spu的id删除sku
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        this.skuMapper.delete(sku);

        // 更新spu和spuDetail
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        this.spuMapper.updateByPrimaryKeySelective(spuBo);

        this.spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());

        // 新增sku和stock
        saveSkuAndStock(spuBo);

        sendMsg("update", spuBo.getId());
    }

    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    public Sku querySkuById(Long id) {
        return this.skuMapper.selectByPrimaryKey(id);
    }
}
