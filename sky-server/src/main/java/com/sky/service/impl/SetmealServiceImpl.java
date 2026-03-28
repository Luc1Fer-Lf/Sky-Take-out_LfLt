package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @ApiOperation("套餐分页查询")
    @Override
    public PageResult pageQuerySetmeal(SetmealPageQueryDTO setmealPageQueryDTO) {
        //1、设置分页参数
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        //2、执行查询并强转为 Page
        Page<SetmealVO> page = setmealMapper.pageQuerySetmeal(setmealPageQueryDTO);
        //3、返回结果
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @ApiOperation("新增套餐")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addSetmeal(SetmealDTO setmealDTO) {
        //1、插入套餐数据
        //拷贝属性值
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.addSetmeal(setmeal);
        //2、插入套餐菜品关系数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
        }
        setmealMapper.addSetmealDish(setmealDishes);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @ApiOperation("批量删除套餐")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> ids) {
        //0、判断当前套餐是否在售
        List<Setmeal> setmeals = setmealMapper.getByIds(ids);
        for (Setmeal setmeal : setmeals) {
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //1、批量删除套餐数据
        setmealMapper.delete(ids);
        //2、批量删除套餐菜品关系数据
        setmealMapper.deleteSetmealDish(ids);
    }

    /**
     * 根据id查询套餐数据
     * @param id
     * @return
     */
    @ApiOperation("根据id查询套餐数据")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SetmealVO getById(Long id) {
        //1、查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        //2、查询套餐菜品关系数据
        List<SetmealDish> setmealDishes = setmealMapper.getSetmealDishBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @ApiOperation("修改套餐")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SetmealDTO setmealDTO) {
        //1、插入套餐数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        //2、删除套餐菜品关系数据
        setmealMapper.deleteSetmealDish(Arrays.asList(new Long[]{setmealDTO.getId()}));
        //3、插入新的套餐菜品关系数据
        if (setmealDTO.getSetmealDishes() != null && setmealDTO.getSetmealDishes().size() > 0) {
            List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmeal.getId());
            }
            setmealMapper.addSetmealDish(setmealDishes);
        }

    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    @ApiOperation("套餐起售、停售")
    @Override
    public void startOrStop(Integer status, Long id) {
        //1、套餐中包含停售的菜品则不能起售，所有查询停售数量，如果大于0则不能起售
        int onSaleSetmealCount = setmealMapper.getSetmealDishCountBySetmealId(id);
        if (onSaleSetmealCount > 0 && status == StatusConstant.ENABLE) {
            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

}
