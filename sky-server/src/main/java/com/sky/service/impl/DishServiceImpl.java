package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     *插入菜品及其口味信息
     * @param dishDto 菜品数据传输对象
     *
     */
    @Transactional
    @Override
    public void save(DishDTO dishDto) {

        Dish dish = new Dish();
        // 属性拷贝
        BeanUtils.copyProperties(dishDto, dish);
        dishMapper.insert(dish);

        List<DishFlavor> flavors = dishDto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                // 设置菜品ID
                flavor.setDishId(dish.getId());

            }
            // 批量插入菜品口味
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 设置分页参数
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 查询菜品数据
        Page<DishVO> dishs = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult<>(dishs.getTotal(), dishs.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {

        //判断菜品是否有起售状态
        List<Integer> status =dishMapper.getStatusByIds(ids);
        if (status.contains(StatusConstant.ENABLE)) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        //判断菜品是否和套餐有关联
        List<SetmealDish> setmealDishes = setmealDishMapper.getByDishIds(ids);
        if (!setmealDishes.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 批量删除菜品
        dishMapper.deleteBatch(ids);
        // 批量删除菜品口味
        dishFlavorMapper.deleteBatchByDishIds(ids);


    }

    /**
     * 菜品起售或停售
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        // 更新菜品状态
        dishMapper.update(dish);

    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        //更新dish
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);


        //更新菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors == null || flavors.isEmpty()) {
            // 如果没有口味信息，则直接返回
            return;
        }
        List<Long> list = new ArrayList<>();
        list.add(dishDTO.getId());

        dishFlavorMapper.deleteBatchByDishIds(list);
        // 设置菜品ID
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }
        dishFlavorMapper.insertBatch(flavors);

    }

    /**
     * @param id
     * @return
     */
    @Override
    public DishVO getById(Long id) {
        //查询dish
        Dish dish = dishMapper.getById(id);
        //查询dishFlavor
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        DishVO dishVO = DishVO.builder().flavors(flavors).build();
        BeanUtils.copyProperties(dish, dishVO);
        //查询分类名称
        String categoryName = categoryMapper.getNameById(dish.getCategoryId());
        dishVO.setCategoryName(categoryName);
        return dishVO;
    }

    /**
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> dishes = dishMapper.getByCategoryId(categoryId);
        return dishes;
    }
}
