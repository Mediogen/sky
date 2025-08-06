package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "菜品相关接口")
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    /**
     * 新增菜品接口
     * @Param dishDto
     * @return Result
     */
    @PostMapping
    @ApiOperation("新增菜品")
    @CacheEvict(value = "dishesByCategory", key = "#dishDto.categoryId") // 清除缓存
    public Result save(@RequestBody DishDTO dishDto) {
        log.info("新增菜品接口被调用");
        dishService.save(dishDto);
        // 这里可以添加具体的业务逻辑
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @Param dishPageQueryDTO
     * @return Result<PageResult<DishVO>>
     *
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询接口被调用，参数：{}", dishPageQueryDTO);
        PageResult<DishVO> pageResult = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids 菜品ID
     * @return Result
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    @CacheEvict(value = "dishesByCategory", allEntries = true) // 清除所有缓存
    public Result deleteBatch(@RequestParam List<Long> ids) {
        log.info("批量删除菜品接口被调用，菜品ID：{}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 菜品起售或停售
     * @param status 菜品销售状态
     * @param id 菜品ID
     * @return Result
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售、停售")
    @Caching(evict = {
            @CacheEvict(cacheNames = "dishesByCategory", allEntries = true),
            @CacheEvict(cacheNames = "setmealsByCategory", allEntries = true)
    })// 清除所有相关缓存
    public Result updateStatus(@PathVariable Integer status,@RequestParam Long id) {
        log.info("菜品起售或停售接口被调用，状态：{}，菜品ID：{}", status, id);
        dishService.updateStatus(status, id);
        return Result.success();
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return Result
     */

    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(value = "dishesByCategory", allEntries = true) // 清除所有缓存
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品接口被调用，参数：{}",dishDTO);
        dishService.update(dishDTO);
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @Param id 菜品ID
     * @return Result<DishVO>
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result getById(@PathVariable Long id){
        log.info("根据id查询菜品接口被调用，菜品ID：{}", id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类ID
     * @return Result<Integer>
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(@RequestParam Long categoryId) {
        log.info("根据分类id查询菜品接口被调用，分类ID：{}", categoryId);
        List<Dish> dishes= dishService.getByCategoryId(categoryId);
        return Result.success(dishes);
    }

}
