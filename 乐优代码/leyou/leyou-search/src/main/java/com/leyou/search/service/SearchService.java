package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GoodsRepository goodsRepository;

    public SearchResult search(SearchRequest request) {

        // 自定义搜索构建对象
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的查询条件
        BoolQueryBuilder basicQuery = buildBasicQueryWithFilter(request);
        queryBuilder.withQuery(basicQuery);
        // 添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 添加分页
        Integer page = request.getPage() - 1;
        Integer size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 添加聚合
        String categoryAggName = "categoryAgg";
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 执行查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // 获取聚合结果集并解析
        List<Map<String, Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));

        // 只有分类聚合的结果为1的时候，才进行规格参数的聚合
        List<Map<String, Object>> specs = null;
        if (categories.size() == 1) {
            specs = addParamAggregation((Long) categories.get(0).get("id"), basicQuery);
        }

        return new SearchResult(goodsPage.getContent(), goodsPage.getTotalElements(), goodsPage.getTotalPages(), categories, brands, specs);
    }

    private BoolQueryBuilder buildBasicQueryWithFilter(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));
        // 获取过滤条件，添加过滤
        Map<String, String> filter = request.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.equals("品牌", key)){
                key = "brandId";
            } else if(StringUtils.equals("分类", key)) {
                key = "cid3";
            } else {
                // specs.name.keyword
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 规格参数的聚合
     *
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> addParamAggregation(Long cid, QueryBuilder basicQuery) {
        // 查询要聚合的规格参数
        List<SpecParam> params = this.specClient.queryParams(null, cid, null, true);
        // 初始化自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加基本的查询条件
        queryBuilder.withQuery(basicQuery);
        // 添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));
        // 进行规格参数的聚合
        params.forEach(param -> {
            // 添加聚合，聚合名称：规格参数名；聚合字段：specs.paramName.keyword
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        // 执行查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        // 解析结果集Map<param.getName, aggregation>
        Map<String, Aggregation> paramAggregationMap = goodsPage.getAggregations().asMap();

        // 初始化最终结果集合
        List<Map<String, Object>> specs = new ArrayList<>();
        // 解析聚合的map
        for (Map.Entry<String, Aggregation> aggregationEntry : paramAggregationMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            // 聚合结果的名称就是参数名
            map.put("k", aggregationEntry.getKey());
            // 规格参数都是字符串
            StringTerms paramAggregation = (StringTerms)aggregationEntry.getValue();

            // 初始化options集合
            List<String> options = new ArrayList<>();
            // 解析每一个桶把桶中的key放入options
            paramAggregation.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            // 放入map集合中
            map.put("options", options);

            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析品牌聚合结果
     *
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        // 该集合收集所有的品牌id
        List<Long> brandIds = new ArrayList<>();
        terms.getBuckets().forEach(bucket -> {
            // 获取桶中的key（brandId）添加到品牌id集合中
            brandIds.add(bucket.getKeyAsNumber().longValue());
        });
        // 根据id集合查询品牌对象集合
        return this.brandClient.queryBrandsByIds(brandIds);
    }

    /**
     * 解析分类聚合结果
     *
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        // 该集合收集所有的品牌id
        List<Long> cids = new ArrayList<>();
        terms.getBuckets().forEach(bucket -> {
            // 获取桶中的key（brandId）添加到品牌id集合中
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        // 根据cids查询names
        List<String> names = this.categoryClient.queryNamesByIds(cids);

        // 初始化分类集合
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        return categories;
    }


    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // 查询分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询所有的sku
        List<Sku> skus = this.goodsClient.querykSkuListBySpuId(spu.getId());

        // 查询所有的搜索规格参数
        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), null, true);

        // 查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        // 处理通过的规格参数，反序列化为Map对象{"1", "其他"}
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // 处理特殊的规格参数，反序列化为Map对象<paramId, List<可选值>>
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });

        // 初始化规格参数的map<规格参数名，规格参数值>
        Map<String, Object> specs = new HashMap<>();
        params.forEach(param -> {
            if (param.getGeneric()) {
                // 如果搜索的规格参数是通用的
                String value = genericSpecMap.get(param.getId().toString()).toString();
                // 判断是否是数字
                if (param.getNumeric()) {
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            } else {
                // 如果是特殊的规格参数
                List<Object> value = specialSpecMap.get(param.getId().toString());
                specs.put(param.getName(), value);
            }

        });

        // 封装价格集合
        List<Long> price = new ArrayList<>();
        // 封装sku的集合
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            // 把价格添加到价格集合
            price.add(sku.getPrice());

            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            map.put("price", sku.getPrice());
            skuMapList.add(map);
        });

        // 设置spuId
        goods.setId(spu.getId());
        // 设置分类的id
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        // 设置品牌的id
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        // spu的title + 分类名称 + 品牌的名称
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        // 收集spu下的所有sku的价格
        goods.setPrice(price);
        // 收集spu下的所有sku
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        // 收集spu所属分类下的所有搜索的规格参数
        goods.setSpecs(specs);

        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public void save(Long spuId) throws IOException {
        // 查询spu信息
        Spu spu = this.goodsClient.querySpuById(spuId);
        // 从spu构建goods对象
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void delete(Long spuId) {
        this.goodsRepository.deleteById(spuId);
    }
}
