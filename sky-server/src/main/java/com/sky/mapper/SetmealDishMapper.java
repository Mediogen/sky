package com.sky.mapper;


import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    List<SetmealDish> getByDishIds(List<Long> dishIds);

    List<SetmealDish> getBySetmealId(Long id);

    /**
     * 添加套餐菜品关系
     * @param setmealdish
     */
    void add(SetmealDish setmealdish);

    /**
     * 批量删除套餐菜品关系
     * @param setmealIds
     */
    void deleteBySetmealIds(List<Long> setmealIds);


    @Select("select setmeal_id from setmeal_dish where dish_id = #{id}")
    List<Long> getSetmealIdsByDishId(Long id);
}
