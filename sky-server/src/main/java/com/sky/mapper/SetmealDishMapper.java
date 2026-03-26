package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据id查询菜品关联套餐数量
     * @param id
     */
    Integer countByDishId(Long id);
}
