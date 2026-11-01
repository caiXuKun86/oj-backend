package com.kun.oj.common;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kun.oj.constant.CommonConstants;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 通用分页请求基础类
 */
@Data
public class PageRequest  {


    /**
     * 默认分页参数与安全上限（引用系统常量）
     */
    public static final int DEFAULT_PAGE_NUM = CommonConstants.DEFAULT_PAGE_NUM;
    public static final int DEFAULT_PAGE_SIZE = CommonConstants.DEFAULT_PAGE_SIZE;
    public static final int MAX_PAGE_SIZE = CommonConstants.MAX_PAGE_SIZE;

    private Integer pageNum = DEFAULT_PAGE_NUM;

    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private String sortField;

    private Boolean isAsc = false;

    /* ========== 容错与防御式 Getter（防止前端传 null 导致 NPE） ========== */

    public Integer getPageNum() {
        if (this.pageNum == null || this.pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return this.pageNum;
    }

    public void setPage(Integer page) {
        if (page != null && page > 0) {
            this.pageNum = page;
        }
    }

    public Integer getPage() {
        return getPageNum();
    }

    public Integer getPageSize() {
        if (this.pageSize == null || this.pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        // 强制上限保护，防止通过非校验路径传大数值导致 OOM
        if (this.pageSize > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return this.pageSize;
    }

    /**
     * 计算原生 SQL 分页的 offset（偏移量）
     * 对应 SQL: LIMIT #{offset}, #{pageSize}
     */
    public long getOffset() {
        return (long) (getPageNum() - 1) * getPageSize();
    }


    /**
     * 将当前请求对象直接转换为 MyBatis-Plus 的 Page 分页对象（支持自动附加排序规则）
     */
    public <T> Page<T> toPage() {
        Page<T> page = new Page<>(getPageNum(), getPageSize());
        if (StringUtils.hasText(this.sortField)) {
            // 防 SQL 注入校验（只允许大小写字母、数字和下划线）
            if (this.sortField.matches("^[a-zA-Z0-9_]+$")) {
                page.addOrder(Boolean.TRUE.equals(this.isAsc)
                        ? OrderItem.asc(this.sortField)
                        : OrderItem.desc(this.sortField));
            }
        }
        return page;
    }


    /**
     * 静态快捷方法：构建 MyBatis-Plus 分页对象 (自动处理默认页码与最大条数保护)
     * (原 PageUtils.buildPage 方法)
     */
    public static <T> Page<T> buildPage(Integer pageNum, Integer pageSize) {
        long current = (pageNum != null && pageNum > 0) ? pageNum : DEFAULT_PAGE_NUM;
        long size = (pageSize != null && pageSize > 0) ? pageSize : DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        return new Page<>(current, size);
    }
}