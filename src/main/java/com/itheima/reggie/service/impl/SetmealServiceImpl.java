package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //map是把stream流里的每个元素给映射出来，操作完了后再collect收集起来
        setmealDishes.stream().map((item) ->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，操作setmeal_dish，执行insert操作
        //关联关系的逻辑要在setmealDishService里头去写
        setmealDishService.saveBatch(setmealDishes);
        //IService有的功能可以直接调，没有的功能就要自己写逻辑了，saveBatch批量保存

    }

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    @Transactional
    @Override
    public void removeWithDish(List<Long> ids) {
        //select count(*) from Setmeal where id in (1,2,3) and status = 1
        //查询套餐状态，确定是否可以删除，只要一看到有查询就要想到LambdaQueryWrapper
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);

        //如果不可以删除，抛出一个业务异常
        if(this.count(queryWrapper) != 0){
            throw new CustomException("套餐正在售卖中，不能删除");
            //这里可以返回一个R.error()吗？？？？？
        }

        //如果可以删除，先删除套餐表中的数据————————Setmeal
        this.removeByIds(ids);   //调用的框架的方法


        //前面是Setmeal可以直接用this，这里是Setmeal_Dish，所以要构建一个查询来删除
        //delete from Setmeal_Dish where setmeal_id in (1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        //删除关系表中的数据——————Setmeal_Dish
        setmealDishService.remove(lambdaQueryWrapper);
    }


}
