package com.leyou.item.service;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父id查询子节点
     * @param pid
     * @return
     */
    public List<Category> queryCategoryListByPid(Long pid) {

        Category category = new Category();
        category.setParentId(pid);
        return this.categoryMapper.select(category);
    }

    /**
     * 根据多个id查询分类名称
     * @param ids
     * @return
     */
    public List<String> queryNamesByIds(List<Long> ids){
        return ids.stream().map(id -> {
            Category category = this.categoryMapper.selectByPrimaryKey(id);
            return category.getName();
        }).collect(Collectors.toList());
    }
}
