package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.*;
import com.sky.webSocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
    private WebSocketServer webSocketServer;
//    @Autowired
//    private UserMapper userMapper;


    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     * @param ordersSubmitDTO 用户下单数据传输对象
     * @return OrderSubmitVO 订单提交视图对象
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
        //3.检查地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

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
        return OrderSubmitVO.builder()
            .id(order.getId())
            .orderAmount(order.getAmount())
            .orderNumber(order.getNumber())
            .orderTime(order.getOrderTime())
            .build();
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO 订单支付数据传输对象
     * @return OrderPaymentVO 订单支付视图对象
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO){
        // 当前登录用户id
        //Long userId = BaseContext.getCurrentId();
        //User user = userMapper.getById(userId);

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
     * @param outTradeNo 商户订单号
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
        // 发送消息通知商家订单支付成功
        webSocketServer.sendToAdmin(1,ordersDB.getId(),outTradeNo);


    }

    /**
     * 分页查询历史订单
     * @param ordersPageQueryDTO 分页查询参数
     * @return PageResult<OrderVO>
     */
    @Override
    public PageResult<OrderVO> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //查询订单列表
        Page<OrderVO> Page = orderMapper.ordersPageQuery(ordersPageQueryDTO);
        //为每个订单查询订单菜品详情
        if (Page != null && Page.getResult() != null && !Page.getResult().isEmpty()) {
            for (OrderVO orderVO : Page.getResult()) {
                //查询订单详情
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
                orderVO.setOrderDetailList(orderDetails);
                //将订单详情列表转换为JSON字符串
                String jsonString = convertListToJsonString(orderDetails);
                orderVO.setOrderDishes(jsonString);

            }
            return new PageResult<>(Page.getTotal(), Page.getResult());
        }
        //如果没有查询到订单，返回空的PageResult

        return new PageResult<>(0L, new ArrayList<>());
    }

    /**
     * 查询订单详情
     * @param id 订单ID
     * @return OrderVO
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        OrderVO orderVO= orderMapper.getOrdersById(id);
        if (orderVO != null ) {
            //查询订单详情
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetails);
            return orderVO;
        }else {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }

    /**
     * 用户取消订单
     * @param id 订单ID
     */
    @Override
    public void cancelByUser(Long id) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(id);
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能直接取消待付款和待接单的订单
        if(orderDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消订单")
                .cancelTime(LocalDateTime.now())
                .build();
        //如果订单状态是待接单，需进行退款
        if (orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //TODO 进行退款操作
            //调用微信支付接口，进行退款

            //修改支付状态为退款
            orders.setPayStatus(Orders.REFUND);
        }


        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param orderId 订单ID
     * 将原订单中的商品重新加入到购物车中
     */
    @Override
    public void repetition(Long orderId) {
        //查询订单
        OrderVO orders = orderMapper.getOrdersById(orderId);
        //校验订单是否存在
        if (orders == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //将订单详情对象列表转换成购物车对象
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        orderDetailMapper.getByOrderId(orderId).forEach(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartList.add(shoppingCart);
        });
        //将购物车对象列表批量插入到购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);


    }

    /**
     * 各个状态的订单数量统计
     * @return OrderStatisticsVO
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 修正点 1: 将VO对象的创建移到if判断之外，确保任何情况下都有一个对象可以返回。
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();

        // 修正点 2: 为所有统计字段设置默认值0。
        // 这能保证即使数据库中没有某个状态的订单，返回的JSON中该字段也是0而不是null。
//        orderStatisticsVO.setToBeConfirmed(0);
//        orderStatisticsVO.setConfirmed(0);
//        orderStatisticsVO.setDeliveryInProgress(0);

        // 查询各个状态的订单数量
        List<SqlOrderStatisticsVO> list = orderMapper.statistics();

        // 仅在列表有效时才进行遍历和赋值
        if (list != null && !list.isEmpty()) {
            for (SqlOrderStatisticsVO sqlOrderStatisticsVO : list) {
                Integer status = sqlOrderStatisticsVO.getStatus();
                Integer count = sqlOrderStatisticsVO.getCount();
                // 使用 if-else if 结构替代 switch
                if (status.equals(Orders.TO_BE_CONFIRMED)) {
                    orderStatisticsVO.setToBeConfirmed(count);
                } else if (status.equals(Orders.CONFIRMED)) {
                    orderStatisticsVO.setConfirmed(count);
                } else if (status.equals(Orders.DELIVERY_IN_PROGRESS)) {
                    orderStatisticsVO.setDeliveryInProgress(count);
                }
            }
        }

        // 修正点 3: 返回填充好数据（或带有默认值0）的orderStatisticsVO对象，而不是null。
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param orderId 订单ID
     */
    @Override
    public void confirm(Long orderId) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(orderId);
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能接单待接单的订单
        if(!orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(orderId)
                .status(Orders.CONFIRMED) // 设置订单状态为已接单
                .build();
        orderMapper.update(orders);

    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejecttion(OrdersRejectionDTO ordersRejectionDTO) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(ordersRejectionDTO.getId());
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能拒单待接单的订单
        if(!orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED) // 设置订单状态为已取消
                .cancelReason(ordersRejectionDTO.getRejectionReason()) // 设置拒单原因
                .cancelTime(LocalDateTime.now())// 设置取消时间
                .build();
        //如果支付状态是已付款，需进行退款?都待接单了，怎么可能是未付款的支付状态呢？
        if (orderDB.getPayStatus().equals(Orders.PAID)) {
            //TODO 进行退款操作
            //调用微信支付接口，进行退款
            //修改支付状态为退款
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO  订单取消数据传输对象
     */
    @Override
    public void cancelByAdminer(OrdersCancelDTO ordersCancelDTO) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(ordersCancelDTO.getId());
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,商家可以取消的订单状态为：待付款、待接单、已接单、派送中
        //如果订单状态是已完成或已取消，则不能取消订单
        if(orderDB.getStatus() > Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED) // 设置订单状态为已取消
                .cancelReason(ordersCancelDTO.getCancelReason()) // 设置取消原因
                .cancelTime(LocalDateTime.now())// 设置取消时间
                .build();
        //如果支付状态是已付款，需进行退款
        if (orderDB.getPayStatus().equals(Orders.PAID)) {
            //TODO 进行退款操作
            //调用微信支付接口，进行退款

            //修改支付状态为退款
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);

    }

    /**
     * 派送订单
     * @param id 订单ID
     */
    @Override
    public void delivery(Long id) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(id);
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能派送已接单的订单
        if(!orderDB.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS) // 设置订单状态为派送中
                .build();
        orderMapper.update(orders);

    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(id);
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能完成派送中的订单
        if(!orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED) // 设置订单状态为已完成
                .deliveryTime(LocalDateTime.now()) // 设置送达时间
                .build();
        orderMapper.update(orders);

    }

    /**
     * 用户催单
     * @param id 订单ID
     */
    @Override
    public void reminder(Long id) {
        //查询订单
        Orders orderDB = orderMapper.getOrdersById(id);
        //校验订单是否存在
        if (orderDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态：1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消
        //校验订单状态,只能催单待接单的订单
        if(!orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //发送消息通知商家催单
        webSocketServer.sendToAdmin(2,orderDB.getId(),orderDB.getNumber());

    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    // 将订单详情列表转换为JSON字符串
    public String convertListToJsonString(List<OrderDetail> orderDetailList) {
        // 校验集合是否为空，避免空指针异常
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            return "";
        }

        // 使用 Stream API 进行处理
        return orderDetailList.stream()
                // 1. 将每个 OrderDetail 对象映射为 "名称*数量" 格式的字符串
                .map(detail -> detail.getName() + "*" + detail.getNumber())
                // 2. 使用 ", " 作为分隔符将所有字符串连接起来
                .collect(Collectors.joining(", "));
    }


}
