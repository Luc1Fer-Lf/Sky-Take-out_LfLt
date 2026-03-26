package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
   /**
     * 批量插入口味数据
     * @param flavors
     */
    void addDishFlavor(List<DishFlavor> flavors);

    /**
     * 批量删除菜品口味数据
     * @param ids
     */
    void deleteByDishId(List<Long> ids);

    /**
     * 根据菜品id查询菜品口味数据
     * @param id
     * @return
     */
    List<DishFlavor> getByDishId(Long id);
}
