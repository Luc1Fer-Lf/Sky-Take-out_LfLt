package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.anno.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish
     */
    //获取主键值，并赋值给ID
    @AutoFill(value = OperationType.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into dish (name, category_id, price, image ,status, create_time, update_time, create_user, update_user,description)" +
            " values (#{name}, #{categoryId}, #{price}, #{image}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser},#{description})")
    void addDish(Dish dish);


    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 修改菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);



    /**
     * 批量删除菜品
     * @param ids
     */
    void delete(List<Long> ids); // 批量删除菜品

    /**
     * 根据ID查询菜品数据
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);

    /**
     * 条件查询
     * @param dish
     * @return
     */
    List<Dish> listById(Dish dish);
}
