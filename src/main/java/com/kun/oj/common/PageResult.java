package com.kun.oj.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kun.oj.constant.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 全局统一分页列表响应实体
 *
 * @param <T> 数据记录泛型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {


    private List<T> list;

    private Integer pageNum;

    private Integer pageSize;

    private Long total;

    private Integer totalPages;

    private Boolean hasNext;

    /**
     * 构建分页结果
     */
    public static <T> PageResult<T> of(List<T> list, Integer page, Integer pageSize, Long total) {
        List<T> safeList = list != null ? list : Collections.emptyList();
        long safeTotal = total != null ? total : 0L;
        int safePage = (page != null && page > 0) ? page : CommonConstants.DEFAULT_PAGE_NUM;
        int safePageSize = (pageSize != null && pageSize > 0) ? pageSize : CommonConstants.DEFAULT_PAGE_SIZE;
        int calculatedPages = safePageSize > 0 ? (int) Math.ceil((double) safeTotal / safePageSize) : 0;
        boolean next = safePage < calculatedPages;

        return PageResult.<T>builder()
                .list(safeList)
                .pageNum(safePage)
                .pageSize(safePageSize)
                .total(safeTotal)
                .totalPages(calculatedPages)
                .hasNext(next)
                .build();
    }

    /**
     * 构建空分页结果
     */
    public static <T> PageResult<T> empty(Integer page, Integer pageSize) {
        return of(Collections.emptyList(), page, pageSize, 0L);
    }

    /**
     * 构建默认空分页结果
     */
    public static <T> PageResult<T> empty() {
        return empty(CommonConstants.DEFAULT_PAGE_NUM, CommonConstants.DEFAULT_PAGE_SIZE);
    }

    /* ========== 原 PageUtils 方法集成 ========== */

    /**
     * 将 MyBatis-Plus IPage 直接转换为系统的标准 PageResult
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        if (page == null) {
            return empty();
        }
        return of(page.getRecords(), (int) page.getCurrent(), (int) page.getSize(), page.getTotal());
    }

    /**
     * 将 MyBatis-Plus IPage 中的实体集合通过指定映射函数转换为 VO 后的 PageResult
     */
    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> converter) {
        if (page == null || page.getRecords() == null) {
            return empty();
        }
        List<T> voList = page.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());
        return of(voList, (int) page.getCurrent(), (int) page.getSize(), page.getTotal());
    }

    /**
     * 使用已转换好的记录集合与 IPage 分页元数据封装为 PageResult
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> targetList) {
        if (page == null) {
            return empty();
        }
        return of(targetList, (int) page.getCurrent(), (int) page.getSize(), page.getTotal());
    }


}
