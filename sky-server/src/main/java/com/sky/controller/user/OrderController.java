package com.sky.controller.user;


import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端-订单接口")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO 下单参数
     * @return Result<OrderSubmitVO>
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单:{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 支付参数
     * @return Result<OrderPaymentVO>
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付:{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单:{}", orderPaymentVO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }


    /**
     * 查询历史订单
     * @param ordersPageQueryDTO 分页查询参数
     * @return Result<PageResult<OrderVO>>
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult<OrderVO>> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO){
        // 设置当前用户ID
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        log.info("查询历史订单,分页查询参数:{}", ordersPageQueryDTO);
        PageResult<OrderVO> pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 查询订单详情
     * @param id 订单ID
     * @return Result<OrderVO>
     */
    @GetMapping("orderDetail/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        log.info("查询订单详情,订单ID:{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 用户取消订单
     * @param id 订单ID
     *
     */
    @PutMapping("cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancelOrder(@PathVariable Long id) {
        log.info("用户取消订单,订单ID:{}", id);
        orderService.cancelByUser(id);
        return Result.success();
    }

    /**
     * 再来一单
     * 将原订单中的商品重新加入到购物车中
     * @param id 订单ID
     */
    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        log.info("再来一单,订单ID:{}", id);
        orderService.repetition(id);
        return Result.success();
    }
}
