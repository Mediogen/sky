package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常
        //1.查询用户地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //2.查询购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper
                .list(ShoppingCart.builder().userId(BaseContext.getCurrentId()).build());
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向order表插入一条数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setUserId(BaseContext.getCurrentId());
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        //设置订单号
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        orderMapper.insert(order);
        //向order_detail表插入多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        shoppingCartList.forEach(shoppingCart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        );
        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.cleanByUserId(BaseContext.getCurrentId());

        //封装VO对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderAmount(order.getAmount())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 分页查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult<OrderVO> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Orders orders = Orders.builder()
                .status(ordersPageQueryDTO.getStatus())
                .userId(BaseContext.getCurrentId())
                .build();
        Page<OrderVO> Page = (Page)orderMapper.getOrders(orders);
        //为每个订单查询订单菜品详情
        if (Page != null && Page.getResult() != null && !Page.getResult().isEmpty()) {
            for (OrderVO orderVO : Page.getResult()) {
                //查询订单详情
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
                orderVO.setOrderDetailList(orderDetails);

            }
            return new PageResult<>(Page.getTotal(), Page.getResult());
        }
        return null;
    }

    /**
     * 查询订单详情
     * @param id
     * @return OrderVO
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        Orders orders = Orders.builder().id(id).build();
        List<OrderVO> orderVOList = orderMapper.getOrders(orders);
        if (orderVOList != null && !orderVOList.isEmpty()) {
            OrderVO orderVO = orderVOList.get(0);
            //查询订单详情
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetails);
            return orderVO;
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrder(Long id) {
        //查询订单
        List<OrderVO> orderVOList = orderMapper.getOrders(Orders.builder().id(id).build());
        //校验订单是否存在
        if (orderVOList == null || orderVOList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders orderDB = orderVOList.get(0);
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能直接取消待付款和待接单的订单
        if(orderDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //如果订单状态是待接单，需进行退款
        if (orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //TODO 进行退款操作
            //调用微信支付接口，进行退款

            //修改支付状态为退款
            orderDB.setPayStatus(Orders.REFUND);
        }

        //更新订单状态
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消订单")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param orderId
     */
    @Override
    public void repetition(Long orderId) {
        //查询订单
        List<OrderVO> orderVOList = orderMapper.getOrders(Orders.builder().id(orderId).build());
        //校验订单是否存在
        if (orderVOList == null || orderVOList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders orders = orderVOList.get(0);
        //将订单详情对象列表转换成购物车对象
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        orderDetailList.forEach(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartList.add(shoppingCart);
        });
        //将购物车对象列表批量插入到购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);


    }

}
