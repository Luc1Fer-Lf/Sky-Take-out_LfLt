package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 处理超时订单:每分钟执行一次 , 下单超过15分钟未支付，则取消该订单
     */
    @Scheduled(cron = "0 0/1 * * * ?")//每分钟执行一次
    public void processTimeoutOrder(){
        log.info("处理超时订单");
        //1、查询数据库中的Orders表  条件 订单状态为1，下单时间小于当前时间减15分钟
        LocalDateTime orderTime = LocalDateTime.now().minusMinutes(15);//
        List<Orders> list =  ordersMapper.selectOutOfTimeOrders(Orders.PENDING_PAYMENT, orderTime);//订单状态为1，下单时间小于当前时间减15分钟
        //2、如果存在，则进行取消，将订单状态改为6
        if (list != null && list.size() > 0) {
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("支付超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                ordersMapper.update(orders);
            }
        }
    }


    /**
     * 每天凌晨一点：处理处于待派送状态的订单,如果存在，则修改为已完成，将订单状态改为5
     */
    @Scheduled(cron = "0 29 17 * * ?")
    public void processPendingDeliveryOrder() {
        log.info("处理处于待派送状态的订单");
        //1、查询数据库中的Orders表  条件 订单状态为4，下单时间小于当前时间减2 小时
        LocalDateTime deliveryTime = LocalDateTime.now().minusHours(2);
        List<Orders> list = ordersMapper.selectOutOfTimeOrdersAtOne(Orders.DELIVERY_IN_PROGRESS, deliveryTime);
        //2、如果存在，则进行取消，将订单状态改为5
        if (list != null && list.size() > 0) {
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                ordersMapper.update(orders);
            }
        }
    }
}
