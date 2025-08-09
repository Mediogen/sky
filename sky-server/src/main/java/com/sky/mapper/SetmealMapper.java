package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param categoryId 分类ID
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO 分页查询参数
     * @return Page<SetmealVO> 分页结果
     */
    Page<SetmealVO> page(SetmealPageQueryDTO setmealPageQueryDTO);


    /**
     * 根据id查询套餐信息
     * @param id 套餐ID
     * @return Setmeal 套餐信息
     */
    Setmeal getById(Long id);

    /**
     * 新增套餐
     * @param setmeal 套餐信息
     */
    @AutoFill(OperationType.INSERT)
    void add(Setmeal setmeal);

    /**
     * 批量删除套餐
     * @param ids 套餐ID列表
     */
    void deleteByIds(List<Long> ids);


    /**
     * 查询套餐状态
     * @param ids 套餐ID列表
     * @return List<Integer> 套餐状态列表
     */
    List<Integer> getStatusByIds(List<Long> ids);

    /**
     * 更新套餐状态
     * @param setmeal 套餐信息
     *
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    /**
     * 批量起售或停售套餐
     * @param status 套餐状态参数
     * @param ids 套餐ID列表
     */
    void updateStatusByIds(Integer status, List<Long> ids);

    /**
     * 动态条件查询套餐
     * @param setmeal 查询参数
     * @return List<Setmeal>
     */
    List<Setmeal> list(Setmeal setmeal);

    @Update("update setmeal set status = #{status} where category_id = #{CategoryId}")
    void updateStatusByCategoryId(Integer status, Long CategoryId);

    @Select("select count(id) from setmeal where status = #{i}")
    Integer setmealCountByStatus(int i);
}
