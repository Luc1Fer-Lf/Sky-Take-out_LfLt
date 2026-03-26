package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     * @param dishDTO
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDish(DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        // 保存菜品数据
        //拷贝属性值
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.addDish(dish);
        // 保存菜品口味数据
        log.info("保存菜品口味数据：{}", dishDTO.getFlavors());
        // 遍历菜品口味数据，设置dishId并保存
        List<DishFlavor> flavors = dishDTO.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }
        dishFlavorMapper.addDishFlavor(flavors);
    }

    /**
     * 菜品管理分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品管理分页查询：{}", dishPageQueryDTO);
        //1、设置分页参数
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        //2、执行分页查询，强转为Page<DishVO>
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        //3、封装返回结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品起售停售
     * @param status
     *
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        log.info("菜品起售停售：{},{}", status,id);
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
                dishMapper.update(dish);
    }
    /**
     * 删除菜品
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> ids) {
        log.info("删除菜品：{}", ids);
        //0、判断当前菜品是否在售，如果正在售，则不能删除
        ids.forEach(id -> {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        //00、判断当前菜品是否关联了套餐，如果关联了套餐，则不能删除
        ids.forEach(id -> {
            Integer count = setmealDishMapper.countByDishId(id);
            if (count > 0) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        });
        //1、删除菜品数据
        dishMapper.delete(ids);
        //2、删除菜品口味数据
        dishFlavorMapper.deleteByDishId(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DishVO getById(Long id) {
        //1、查询菜品数据
        Dish dish = dishMapper.getById(id);
        DishVO dishVO = DishVO.builder()
                .id(dish.getId())
                .name(dish.getName())
                .categoryId(dish.getCategoryId())
                .price(dish.getPrice())
                .image(dish.getImage())
                .description(dish.getDescription())
                .status(dish.getStatus())
                .build();
                dishVO.setFlavors(dishFlavorMapper.getByDishId(id));
                return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        //1、修改菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        //2、修改菜品口味数据  先删后加
        dishFlavorMapper.deleteByDishId(Collections.singletonList(dishDTO.getId()));
        // 遍历菜品口味数据，设置dishId并保存
        if(dishDTO.getFlavors() != null && !dishDTO.getFlavors().isEmpty()) {
            List<DishFlavor> flavors = dishDTO.getFlavors();
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
            }
            dishFlavorMapper.addDishFlavor(flavors);
        }
    }
}
