package com.sky.controller.user;


import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@Slf4j
@Api(tags = "C端-套餐相关接口")
@RequestMapping("/user/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类ID查询套餐
     * @param categoryId 分类ID
     * @return Result 统一返回Result对象
     */
    @GetMapping("/list")
    @ApiOperation("根据分类ID查询套餐")
    @Cacheable(value = "setmealsByCategory", key = "#categoryId")
    public Result<List<Setmeal>> list(@RequestParam Long categoryId) {
        log.info("根据分类ID:{} 查询套餐", categoryId);
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);
        List<Setmeal> list = setmealService.list(setmeal);
        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品
     * @param id 套餐ID
     * @return Result<List<DishItemVO>> 统一返回Result对象
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品")
    public Result<List<DishItemVO>> getDishesBySetmealId(@PathVariable Long id) {
        log.info("根据套餐id查询包含的菜品,套餐ID:{}", id);
        List<DishItemVO> list = setmealService.getDishesBySetmealId(id);
        return Result.success(list);
    }
}
