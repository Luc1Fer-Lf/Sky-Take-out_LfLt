package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "套餐相关接口")
@Slf4j
@RestController
@RequestMapping("/admin/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @ApiOperation("套餐分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuerySetmeal(setmealPageQueryDTO);
        return Result.success(pageResult);
    }


    /**
     * 新增套餐
     * @param setmealDTO
     */
    @CacheEvict(value = "setmealCache", key = "#setmealDTO.categoryId")//清理指定分类上的缓存
    @ApiOperation("新增套餐")
    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}", setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success();
    }

    /**
     * 批量删除
     * @param ids
     */
    @CacheEvict(value = "setmealCache", allEntries = true)//清除全部缓存
    @ApiOperation("批量删除")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除：{}", ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /**
     * 根据ID查询
     * @param id
     */
    @ApiOperation("根据ID查询")
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询：{}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }


    /**
     * 修改套餐
     * @param setmealDTO
     */
    @CacheEvict(value = "setmealCache", allEntries = true )//清除全部缓存
    @ApiOperation("修改套餐")
    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 起售、停售
     *
     */
    @CacheEvict(value = "setmealCache", allEntries = true )//清除全部缓存
    @ApiOperation("起售、停售")
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("起售、停售：{}", status);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
