package com.kun.oj.service.admin.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kun.oj.common.PageResult;
import com.kun.oj.common.ResultCode;
import com.kun.oj.enums.UserRoleEnum;
import com.kun.oj.exception.BusinessException;
import com.kun.oj.mapper.UserMapper;
import com.kun.oj.model.dto.request.UserPageRequest;
import com.kun.oj.model.dto.request.UserUpdateRequest;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.vo.UserPageVO;
import com.kun.oj.service.admin.AdminUserService;
import org.springframework.stereotype.Service;

/**
 * @author Lenovo
 * @description 针对表【question_answer(题目答案)】的数据库操作Service实现
 * @createDate 2026-10-31 10:44:23
 */
@Service
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User>
        implements AdminUserService {


    @Override
    public PageResult<UserPageVO> queryUserPage(UserPageRequest pageRequest) {

        Long id = pageRequest.getId();
        String username = pageRequest.getUsername();
        String nickname = pageRequest.getNickname();
        String role = pageRequest.getRole();


        if (StrUtil.isNotBlank(role) && UserRoleEnum.getByCode(role) == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "角色不存在");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .like(StrUtil.isNotBlank(username), User::getUsername, username)
                .like(StrUtil.isNotBlank(nickname), User::getNickname, nickname)
                .eq(StrUtil.isNotBlank(role), User::getRole, role)
                .eq(id != null, User::getId, id);

        if (StrUtil.isBlank(pageRequest.getSortField())) {
            queryWrapper.orderBy(true, pageRequest.getIsAsc(), User::getCreateTime);
        }

        Page<User> questionPage = this.page(pageRequest.toPage(), queryWrapper);
        return PageResult.of(questionPage, user -> BeanUtil.copyProperties(user, UserPageVO.class));

    }

    @Override
    public Boolean updateUser(UserUpdateRequest userUpdateRequest, Long userId) {

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR, "用户不存在");
        }

        String username = userUpdateRequest.getUsername();
        String nickname = userUpdateRequest.getNickname();
        String role = userUpdateRequest.getRole();

        user.setUsername(username);
        user.setNickname(nickname);
        user.setRole(role);
        boolean b = this.updateById(user);
        if (!b) {
            throw new BusinessException(ResultCode.OPERATION_ERROR);
        }

        return true;
    }
}




