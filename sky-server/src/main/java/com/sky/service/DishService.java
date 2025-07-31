package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void save(DishDTO dishDto);

    PageResult<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    void updateStatus(Integer status, Long id);

    void update(DishDTO dishDTO);

    DishVO getById(Long id);

    List<Dish> getByCategoryId(Long categoryId);
}
