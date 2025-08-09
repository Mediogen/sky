package com.sky.controller.admin;


import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台相关接口")
@Slf4j
public class WorkspaceController {
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 查询今日运营数据
     * @return Result<BusinessDataVO>
     */
    @GetMapping("/businessData")
    @ApiOperation("查询今日运营数据")
    public Result<BusinessDataVO> getBusinessData() {
        log.info("工作台相关接口-查询今日运营数据");
        return Result.success(workspaceService.getBusinessData());
    }

    /**
     * 查询订单管理数据
     * @return Result<OrderOverViewVO>
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查询订单管理数据")
    public Result<OrderOverViewVO> overviewOrders() {
        log.info("工作台相关接口-查询订单管理数据");
        OrderOverViewVO orderOverViewVO = workspaceService.overviewOrders();
        return Result.success(orderOverViewVO);
    }

    /**
     * 查询菜品总览
     * @return Result<DishOverViewVO>
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("查询菜品总览")
    public Result<DishOverViewVO> overviewDishes() {
        log.info("工作台相关接口-查询菜品总览");
        DishOverViewVO dishOverViewVO = workspaceService.overviewDishes();
        return Result.success(dishOverViewVO);
    }

    /**
     * 查询套餐总览
     * @return Result<SetmealOverViewVO>
     */
    @GetMapping("/overviewSetmeals")
    @ApiOperation("查询套餐总览")
    public Result<SetmealOverViewVO> overviewSetmeals() {
        log.info("工作台相关接口-查询套餐总览");
        SetmealOverViewVO setmealOverViewVO = workspaceService.overviewSetmeals();
        return Result.success(setmealOverViewVO);
    }
}
