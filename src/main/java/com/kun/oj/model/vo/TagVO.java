package com.kun.oj.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagVO {
    /**
     * id
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 父标签Id
     */
    private Long parentId;

    private List<TagVO> children;

    private static final long serialVersionUID = 1L;


}

