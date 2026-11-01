package com.kun.oj.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kun.oj.common.PageResult;
import com.kun.oj.model.dto.request.UserPageRequest;
import com.kun.oj.model.dto.request.UserUpdateRequest;
import com.kun.oj.model.entity.User;
import com.kun.oj.model.vo.UserPageVO;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface AdminUserService extends IService<User> {


    PageResult<UserPageVO> queryUserPage(UserPageRequest userQueryRequest);

    Boolean updateUser(UserUpdateRequest userUpdateRequest, Long userId);
}
