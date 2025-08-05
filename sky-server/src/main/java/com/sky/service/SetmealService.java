package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult<SetmealVO> page(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO getById(Long id);

    void add(SetmealDTO setmealDTO);

    void delete(List<Long> ids);

    void updateStatus(Integer status, Long id);

    void update(SetmealDTO setmealDTO);
}
