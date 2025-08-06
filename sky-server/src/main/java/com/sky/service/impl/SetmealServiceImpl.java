package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult<SetmealVO> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.page(setmealPageQueryDTO);
        return new PageResult<>(page.getTotal(),page.getResult());
    }

    /**
     * 根据ID查询套餐
     * @param id
     * @return
     */
    @Override
    @Transactional
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal == null) {
            return null;
        }
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        List<SetmealDish> dishs = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(dishs);
        return setmealVO;
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void add(SetmealDTO setmealDTO) {
        // 保存套餐信息

        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.add(setmeal);
        // 保存套餐菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            for (SetmealDish setmealdish : setmealDishes) {
                setmealdish.setSetmealId(setmeal.getId());
                setmealDishMapper.add(setmealdish);
            }
        }

    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //判断套餐是否有起售状态
        List<Integer> status =setmealMapper.getStatusByIds(ids);
        if (status.contains(StatusConstant.ENABLE)) {
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }
        // 删除套餐菜品关系
        setmealDishMapper.deleteBySetmealIds(ids);
        // 删除套餐
        setmealMapper.deleteByIds(ids);

    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        //如果套餐中有菜品处于停售状态，则不能起售套餐
        if(status.equals(StatusConstant.ENABLE)){
            List<SetmealDish> dishStatus = setmealDishMapper.getBySetmealId(id);
            for (SetmealDish setmealDish : dishStatus) {
                if (dishMapper.getById(setmealDish.getDishId()).getStatus().equals(StatusConstant.DISABLE) ){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }


        }
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        // 更新套餐状态
        setmealMapper.update(setmeal);

    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //更新setmeal
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //更新套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes == null || setmealDishes.isEmpty()) {
            // 如果没有菜品信息，则直接返回
            return;
        }
        //删除原有套餐菜品关系
        List<Long> ids = new ArrayList<>();
        ids.add(setmealDTO.getId());
        setmealDishMapper.deleteBySetmealIds(ids);
        //重新添加套餐菜品关系
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
            setmealDishMapper.add(setmealDish);
        }
    }

    /**
     * 条件查询套餐列表
     * @param setmeal
     * @return List<Setmeal>
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据套餐ID查询套餐下的菜品列表
     * @param setmealId 套餐ID
     * @return
     */
    @Override
    public List<DishItemVO> getDishesBySetmealId(Long setmealId) {
        List<DishItemVO> setmealDishes = setmealDishMapper.getDishesBySetmealId(setmealId);
        return setmealDishes;
    }
}
