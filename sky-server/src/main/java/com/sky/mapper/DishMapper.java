package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);


    /**
     * 插入菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT)
    void insert(Dish dish);

    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    List<Integer> getStatusByIds(List<Long> ids);


    void deleteBatch(List<Long> ids);

    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);


    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);


    List<Dish> list(Dish dish);


    @Select("select * from dish where category_id = #{categoryId}")
    List<Dish> listByCategoryId(Long categoryId);

    @Update("update dish set status = #{status} where category_id = #{CategoryId}")
    void updateStatusByCategoryId(Integer status, Long CategoryId);
}
