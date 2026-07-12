package com.susumonitor.server.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.susumonitor.server.module.auth.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 将当前接口注册为 MyBatis Mapper，使 Spring 能够注入并调用数据库访问方法。
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    // 按用户名查询用户，供注册查重和登录认证使用。
    UserEntity selectByUsername(@Param("username") String username);

    // 查询所有待审核用户，供管理员审核列表使用。
    List<UserEntity> selectPendingUsers();
}
