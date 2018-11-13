package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 分页查询品牌数据
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    public PageResult<Brand> queryBrandByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {
        // 初始化example对象
        Example example = new Example(Brand.class);

        // 只有key不为null的时候，才需要拼接查询条件
        if (StringUtils.isNotBlank(key)){
            // 品牌名称或者是品牌的首字母
            example.createCriteria().andLike("name", "%" + key + "%").orEqualTo("letter", key);
        }

        // 添加排序功能
        example.setOrderByClause(sortBy + " " + (desc ? "desc" : "asc"));

        // 分页助手的静态方法，紧跟在该方法后的第一个方法会被分页
        PageHelper.startPage(page, rows);

        List<Brand> brands = this.brandMapper.selectByExample(example);

        // 封装pageInfo对象
        PageInfo<Brand> pageInfo = new PageInfo<>(brands);

        // 通过pageInfo对象获取分页参数，封装成分页结果集
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    public void saveBrand(Brand brand, List<Long> cids) {
        // 新增品牌，获取回写的品牌的id
        Boolean flag = this.brandMapper.insertSelective(brand) == 1;
        // 新增中间表记录
        if (flag) {
            cids.forEach(cid -> this.brandMapper.saveCategoryBrand(cid, brand.getId()));
        }
    }

    /**
     * 根据cid查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandsByCid(Long cid) {

        return this.brandMapper.selectBrandsByCid(cid);
    }

    /**
     * 根据id查询品牌
     * @param id
     * @return
     */
    public Brand queryBrandById(Long id) {
        return this.brandMapper.selectByPrimaryKey(id);
    }

    public List<Brand> queryBrandsByIds(List<Long> ids) {
        return this.brandMapper.selectByIdList(ids);
    }


}
