package com.kun.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.ErrorCode;
import com.kun.oj.constant.CommonConstant;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.exception.ThrowUtils;
import com.kun.oj.mapper.QuestionTagMapper;
import com.kun.oj.model.dto.questionTag.QuestionTagQueryRequest;
import com.kun.oj.model.entity.QuestionTag;
import com.kun.oj.model.vo.TagVO;
import com.kun.oj.service.QuestionTagService;
import com.kun.oj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author root
 * @description 针对表【question_tag(题目标签)】的数据库操作Service实现
 * @createDate 2026-06-20 19:10:04
 */
@Service
public class QuestionTagServiceImpl extends ServiceImpl<QuestionTagMapper, QuestionTag>
        implements QuestionTagService {
    @Override
    public void validQuestionTag(QuestionTag questionTag, boolean add) {
        if (questionTag == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = questionTag.getName();
        Long parentId = questionTag.getParentId();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasNull(name, parentId), ErrorCode.PARAMS_ERROR);
        }


    }

    /**
     * 获取查询包装类
     *
     * @param questionTagQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionTag> getQueryWrapper(QuestionTagQueryRequest questionTagQueryRequest) {

        QueryWrapper<QuestionTag> queryWrapper = new QueryWrapper<>();
        if (questionTagQueryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.select("id", "questionId", "userId", "language", "reviewStatus", "reviewMessage", "reviewerId");
        Long id = questionTagQueryRequest.getId();
        String name = questionTagQueryRequest.getName();
        Long parentId = questionTagQueryRequest.getParentId();
        String sortField = questionTagQueryRequest.getSortField();
        String sortOrder = questionTagQueryRequest.getSortOrder();
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(parentId), "userId", parentId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }



    @Override
    public List<TagVO> getAllTags() {
        List<QuestionTag> list = this.list();
        Map<Long, TagVO> map = new HashMap<>();
        List<TagVO> tagVOList = new ArrayList<>();

        for (QuestionTag questionTag : list) {
            Long parentId = questionTag.getParentId();
            TagVO tagVO = new TagVO();
            BeanUtil.copyProperties(questionTag, tagVO);
            if (parentId == 0) {
                tagVO.setChildren(new ArrayList<>());
                tagVOList.add(tagVO);
                map.put(tagVO.getId(), tagVO);
            } else {
                tagVO.setChildren(null);
                TagVO parentTagVO = map.get(parentId);
                parentTagVO.getChildren().add(tagVO);
            }
        }
        return tagVOList;
    }

}




