package com.leyou.common.pojo;

import java.util.List;

/**
 * 分页结果集
 * @param <T>
 */
public class PageResult<T> {

    private List<T> items; // 分页当前页的记录

    private Long total; // 数据的总条数

    private Integer totalPage; // 总页数

    public PageResult() {
    }

    public PageResult(List<T> items, Long total) {
        this.items = items;
        this.total = total;
    }

    public PageResult(List<T> items, Long total, Integer totalPage) {
        this.items = items;
        this.total = total;
        this.totalPage = totalPage;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }
}
