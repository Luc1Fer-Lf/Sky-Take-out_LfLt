package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 统计金额
     * @param confirmed
     * @param beginTime
     * @param endTime
     * @return
     */
    Double sumByStatusAndOrderTime(Integer status, LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计当日新增用户数
     * @param beginTime
     * @param endTime
     * @return
     */
    Integer countByCreateTime(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计截止时间的总人数
     * @param endTime
     * @return
     */
    Integer countSumByCreateTime(LocalDateTime endTime);

    /**
     * 统计每日订单数
     * @param beginTime
     * @param endTime
     * @return
     */
    Integer sumOrderCountByOrderTime(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计每日订单数
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    Integer sumValidOrderCountByOrderTime(Integer status, LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计销量排名top10
     * @param map
     * @return
     */
    @MapKey("name")
    List<Map> sumTop10(Map map);

    /**
     * 工作台统计每日数据
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 工作台统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap(Map map);
}
