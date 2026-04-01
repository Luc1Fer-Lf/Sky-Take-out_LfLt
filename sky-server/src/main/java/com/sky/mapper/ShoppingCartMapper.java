package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 查询购物车数据
     * @param shoppingCart
     */
    ShoppingCart select(ShoppingCart shoppingCart);

    /**
     * 添加购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 修改购物车数据
     * @param existShoppingCart
     */
    void updateById(ShoppingCart existShoppingCart);


    /**
     * 查询当前用户的购物车数据
     *
     */
    List<ShoppingCart> list(Long userId);

    /**
     * 删除购物车数据
     * @param userId
     */
    void deleteByUserId(Long userId);

    /**
     * 根据菜品id和用户id删除购物车数据
     */
    void deleteByDishIdAndUserId(Long dishId, Long userId);

    /**
     * 根据套餐id和用户id删除购物车数据
     */
    void deleteBySetmealIdAndUserId(Long setmealId, Long userId);

    /**
     * 根据用户id查询购物车数据
     */
    List<ShoppingCart> listByUserId(Long userId);

    /**
     * 批量插入购物车数据
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
