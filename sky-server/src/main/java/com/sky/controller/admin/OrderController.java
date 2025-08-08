package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端-订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO 分页查询参数
     * @return Result<PageResult<OrderVO>>
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult<OrderVO>> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单搜索:{}", ordersPageQueryDTO);
        PageResult<OrderVO> pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     * @return OrderStatisticsVO 订单统计视图对象
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        log.info("各个状态的订单数量统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 查询订单详情
     * @param id 订单ID
     * @return Result<OrderVO> 订单视图对象
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> getDetail(@PathVariable Long id) {
        log.info("查询订单详情,订单ID:{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param ordersConfirmDTO 接单DTO
     * @return Result
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        Long id = ordersConfirmDTO.getId();
        log.info("接单,订单ID:{}", id);
        orderService.confirm(id);
        return Result.success();
    }

    /**
     * 拒单
     * @param ordersRejectionDTO 订单拒绝DTO
     * @return Result
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒单:{}", ordersRejectionDTO);
        orderService.rejecttion(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO 订单取消DTO
     * @return Result
     */
    @PutMapping("/cancel")
    @ApiOperation("商家取消订单")
    public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("商家取消订单:{}", ordersCancelDTO);
        orderService.cancelByAdminer(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送")
    public Result delivery(@PathVariable Long id) {
        log.info("派送订单,订单ID:{}", id);
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id 订单ID
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {
        log.info("完成订单,订单ID:{}", id);
        orderService.complete(id);
        return Result.success();
    }
}
