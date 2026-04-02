package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */

    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //查询地址表中数据
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //校验用户地址是否超出配送范围
        String address = addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail();
        checkOutOfRange(address);

        //查询用户数据
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        //查询购物车列表
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.listByUserId(userId);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //1、构造订单数据，存入orders表中
        Orders orders = new Orders();
        //拷贝属性值将ordersDTO中的属性值拷贝到orders对象中
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        //补充缺失属性值
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayMethod(1);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail() + "");
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserName(user.getName());
        ordersMapper.insert(orders);//插入订单数据
        log.info("订单id：{}", orders.getId());
        //2、构造订单明细数据，存入order_detail表中
        //获取购物车数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            //关联订单id
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        //批量插入订单明细数据
        orderDetailMapper.insertBatch(orderDetailList);
        //3、清空购物车
        shoppingCartMapper.deleteByUserId(userId); //清空购物车数据
        //4、封装VO对象并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
//----------------------------------------------------------------------------------------------------------------------
//                               模拟支付成功
        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = ordersMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
        //通过websocket 推送消息给客户端
        Map map = new HashMap();
        map.put("type", 1);//1、表示来单提醒  2、表示用户催单
        map.put("orderId", ordersDB.getId());//订单id
        map.put("content", "订单号：" + ordersDB.getNumber());// 内容
        String json = JSON.toJSONString(map);//转为json
        webSocketServer.sendToAllClient(json);//推送消息给客户端

    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("历史订单查询：{}", ordersPageQueryDTO);
        //1、设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //补充属性值
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(ordersPageQueryDTO.getStatus());
        //2、执行分页查询
        Page<Orders> page = ordersMapper.pageQueryByUser(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            //查询订单明细
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Override
    public OrderVO getById(Long id) {
        OrderVO orderVO = new OrderVO();
        //1、查询订单表数据
        Orders orders = ordersMapper.getById(id);
        BeanUtils.copyProperties(orders, orderVO);
        //2、查询订单明细表数据
        orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(id));
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) throws Exception {
        Orders ordersDB = ordersMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);//订单不存在
        }
        if (ordersDB.getStatus() >2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);//订单状态错误
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //模拟退款，直接将待接单的订单置为已退款
            // 更新订单状态
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //查询当前订单的菜品信息
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orderDetailList == null || orderDetailList.size() == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //构造购物车
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        //1、设置分页参数
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //2、执行分页查询
        Page<Orders> page = ordersMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            //查询订单明细
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);

        }
        //3、根据订单ID查询菜品名称和数量并拼接成字符串返回
        orderVOList.forEach(orderVO -> orderVO.setOrderDishes(getOrderDishes(orderVO.getOrderDetailList())));
        //4、返回结果
        return new PageResult(page.getTotal(), orderVOList);
    }
    /**
     * 获取订单菜品信息
     * @param orderDetailList
     * @return
     */
    private String getOrderDishes(List<OrderDetail> orderDetailList) {
        StringBuilder orderDishes = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDishes.append(orderDetail.getDishFlavor() + " " + orderDetail.getName() + "*" + orderDetail.getNumber() + "份 , ");
        }
        return orderDishes.toString().substring(0, orderDishes.length() - 1);
    }

    /**
     * 统计订单数据
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = ordersMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = ordersMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = ordersMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单：{}", ordersConfirmDTO);
        ordersMapper.updateStatus(ordersConfirmDTO.getId(), Orders.CONFIRMED);
    }

    /**
     * 拒单
     * @param ordersRejectDTO
     */
    @Override
    public void reject(OrdersRejectionDTO ordersRejectDTO) {
        log.info("拒单：{}", ordersRejectDTO);
        //1、只有订单处于“待接单”状态时可以执行拒单操作
        Orders ordersDB = ordersMapper.getById(ordersRejectDTO.getId());
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //3、商家拒单时，如果用户已经完成了支付，需要为用户退款
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            // 调用微信支付接口退款
            //因为没有商家用户所以不能进行退款操作，直接将状态调为已退款，来模拟退款
            ordersMapper.update(Orders.builder()
                    .id(ordersDB.getId())
                    .status(Orders.REFUND)
                    .build());
        }
        //2、商家拒单时需要指定拒单原因
        if (ordersRejectDTO.getRejectionReason() == null || ordersRejectDTO.getRejectionReason().trim().length() == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_REJECT_REASON_NOT_NULL);
        }
        ordersMapper.update(Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectDTO.getRejectionReason())
                .cancelReason("商家拒单")
                .cancelTime(LocalDateTime.now())
                .build());
        log.info("拒单成功：{}", ordersDB);
    }

    /**
     * 管理员取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        //1、商家取消订单时需要指定取消原因
        if (ordersCancelDTO.getCancelReason() == null || ordersCancelDTO.getCancelReason().trim().length() == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_CANCEL_REASON_NOT_NULL);
        }
        //2、商家取消订单时，如果用户已经完成了支付，需要为用户退款
        Orders ordersDB = ordersMapper.getById(ordersCancelDTO.getId());
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            //调用微信支付接口退款
            //因为没有商家用户所以不能进行退款操作，直接将状态调为已退款，来模拟退款
            ordersMapper.update(Orders.builder()
                    .id(ordersCancelDTO.getId())
                    .status(Orders.REFUND) // 直接将状态调为已退款，来模拟退款
                    .build());
        }
        //3、更新订单状态、取消原因、取消时间
        log.info("管理员取消订单：{}", ordersCancelDTO);
        ordersMapper.update(Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build());
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        log.info("派送订单：{}", id);
        ordersMapper.updateStatus(id, Orders.DELIVERY_IN_PROGRESS);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        log.info("完成订单：{}", id);
        //1、补充送达时间
        ordersMapper.update(Orders.builder().id(id).status(Orders.COMPLETED).deliveryTime(LocalDateTime.now()).build());
        log.info("订单完成：{}", id);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address 用户收货地址
     */
    private void checkOutOfRange(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address", address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 500000000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

}
