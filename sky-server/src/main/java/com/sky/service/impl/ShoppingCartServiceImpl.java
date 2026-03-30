package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @ApiOperation("添加购物车")
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        //1、创建购物车对象
        ShoppingCart shoppingCart = new ShoppingCart();
        //2、拷贝属性值
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //3、补充属性值
        //3.1.1判断购物车中是否已存在该商品  --条件：dishId\dishFlavor \ userId\setmealId
        shoppingCart.setUserId(BaseContext.getCurrentId());//设置用户id
        ShoppingCart existShoppingCart = shoppingCartMapper.select(shoppingCart);
        if (existShoppingCart != null) {
            //3.1.2如果已存在，数量加1
            Integer number = existShoppingCart.getNumber();
            existShoppingCart.setNumber(number + 1);
            //3.1.3更新购物车数据
            shoppingCartMapper.updateById(existShoppingCart);
        } else {
            //3.1 判断是新增套餐还是菜品
            if (shoppingCart.getDishId() != null) {//添加的是菜品
                //根据菜品id查询菜品
                Dish dish = dishMapper.getById(shoppingCart.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {//添加的是套餐
                //根据套餐id查询菜品
                Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);//默认数量为1 判断购物车中是否已存在该菜品
            shoppingCart.setCreateTime(LocalDateTime.now());
            //4、将商品数据存入购物车
            shoppingCartMapper.insert(shoppingCart);
        }


    }

    /**
     * 查看购物车
     *
     * @return
     */
    @Override
    public List<ShoppingCart> list() {
        return shoppingCartMapper.list(BaseContext.getCurrentId());
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    /**
     * 删除购物车数据
     *
     * @param shoppingCartDTO
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        //拿到用户Id，并通过Id查购物车中该商品的数据
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        //拷贝属性值
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //1、获取购物车中该商品数据,拿到该商品数据,如果数量为1，则删除该商品数据
        ShoppingCart existShoppingCart = shoppingCartMapper.select(shoppingCart);
        if (existShoppingCart.getNumber() == 1) {
            //判断购物车中是否是菜品
            if (shoppingCart.getDishId() != null) {
                shoppingCartMapper.deleteByDishIdAndUserId(shoppingCart.getDishId(), BaseContext.getCurrentId());
            }
            else {
                shoppingCartMapper.deleteBySetmealIdAndUserId(shoppingCart.getSetmealId(), BaseContext.getCurrentId());
            }
        }
        else {
            //2、数量不为1，则数量减1
            Integer number = existShoppingCart.getNumber();
            existShoppingCart.setNumber(number - 1);
            //3、更新购物车数据
            shoppingCartMapper.updateById(existShoppingCart);
        }

    }


}
