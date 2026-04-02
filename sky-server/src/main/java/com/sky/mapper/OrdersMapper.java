package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrdersMapper {
    //插入订单数据
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQueryByUser(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 根据ID查询订单信息
     * @param id
     * @return
     */
    Orders getById(Long id);

    /**
     * 管理端条件分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各订单状态数量
     * @param status
     * @return
     */
    Integer countByStatus(Integer status);

    /**
     * 接单
     * @param id
     * @param status
     */
    void updateStatus(Long id, Integer status);

    /**
     *处理超时订单
     * @return
     */
    List<Orders> selectOutOfTimeOrders(Integer status, LocalDateTime orderTime);

    /**
     *在一点时集中处理未派送订单
     * @return
     */
    List<Orders> selectOutOfTimeOrdersAtOne(Integer status, LocalDateTime deliveryTime);
}
