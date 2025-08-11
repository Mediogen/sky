package com.sky.mapper;


import com.sky.entity.User;
import com.sky.vo.CountUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {


    @Select("select * from user where openid = #{openid}")
    public User findByOpenid(String openid);


    public void add(User user);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    List<CountUser> countUserByCreateTime(LocalDate begin, LocalDate endPlusOne);

    @Select("select count(0) from user where  create_time < #{time}")
    Long countUserBeforeDate(LocalDateTime time);
}
