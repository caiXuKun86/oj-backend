package com.kun.oj.model.dto.question;

import cn.hutool.crypto.digest.DigestUtil;
import com.kun.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryRequest extends PageRequest implements Serializable {

    /**
     * 编号
     */
    private String number;
    /**
     * 标题
     */
    private String title;
    /**
     * 难度
     */
    private String difficulty;
    /**
     * 标签列表（json 数组）
     */
    private List<Long> tagIds;


    public Integer getTagIdsSize() {
        return tagIds == null ? 0 : tagIds.size();
    }

    public String generateCacheKeyHash() {
        // 1. 对 tagIds 进行自然排序与去重，防止 [1, 2] 和 [2, 1] 产生不同的 Hash
        String sortedTagsStr = "";
        if (tagIds != null && !tagIds.isEmpty()) {
            sortedTagsStr = new TreeSet<>(tagIds).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        // 2. 拼接规范化的参数串（包含父类 PageRequest 中的分页与排序字段）
        StringBuilder sb = new StringBuilder();
        sb.append("num=").append(number != null ? number.trim() : "")
                .append("&title=").append(title != null ? title.trim() : "")
                .append("&diff=").append(difficulty != null ? difficulty.trim() : "")
                .append("&tags=").append(sortedTagsStr)
                .append("&current=").append(getCurrent())
                .append("&pageSize=").append(getPageSize())
                .append("&sortField=").append(getSortField() != null ? getSortField() : "")
                .append("&sortOrder=").append(getSortOrder() != null ? getSortOrder() : "");

        // 3. 计算 MD5 (方案 A: Hutool 方式)
        return DigestUtil.md5Hex(sb.toString());

    }
    private static final long serialVersionUID = 1L;
}
