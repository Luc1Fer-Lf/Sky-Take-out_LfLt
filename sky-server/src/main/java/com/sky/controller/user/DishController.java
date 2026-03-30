package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        //缓存优化
        //在查询MySQl之前，判断Redis缓存中是否存在数据
        String key = "dish_" + categoryId;
        List<DishVO> dishVoList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //缓存中有数据，直接返回
        if (dishVoList != null && dishVoList.size() > 0) {
            return Result.success(dishVoList);
        }

        //缓存中没有数据，查询MySQL数据库，将数据放入Redis缓存中以供下次查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        List<DishVO> list = dishService.listWithFlavor(dish);
        //将数据放入Redis缓存中以供下次查询
        redisTemplate.opsForValue().set(key, list);
        log.info("查询MySQL数据库，将数据放入Redis缓存中以供下次查询");
        return Result.success(list);
    }

}
