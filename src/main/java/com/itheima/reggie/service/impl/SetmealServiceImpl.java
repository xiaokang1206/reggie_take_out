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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {


    @Autowired
    private SetmealDishService setmealDishService;




    /**
     * 新增套餐,同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     * @return
     */
    @Override
    public void saveWithDith(SetmealDto setmealDto) {

        //保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);
        /*
        * 因为setmealDot继承了Setmeal,保存后，setmealid就有值了，(MP自动生成id)
        * */
        //保存套餐和菜品的关联信息，操作setmeal_dish,执行insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
         setmealDishes.stream().map((item)->{

            item.setSetmealId(setmealDto.getId());
            return item;

        }).collect(Collectors.toList());



        setmealDishService.saveBatch(setmealDishes);


    }

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     * @param ids
     */

    @Override
    public void removeWithDish(List<Long> ids) {
            //查询到餐的状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);//1表示售卖中

        int count = this.count(queryWrapper);
        if (count > 0){
            //如果入过不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }



          //如果可以删除，则先删除套餐表中的数据  --setmeal
          this.removeByIds(ids);



          //再删除关系表中的数据  setmeal_Dish
//delete from setmeal_dish where setmeal_id in (1,2,3)
  LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper=new LambdaQueryWrapper<>();

  lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);

  setmealDishService.remove(lambdaQueryWrapper);




    }

    /**
     * 根据id查询套餐信息和菜品
     * @param id
     * @return
     */
    @Override
    public SetmealDto getSetmealAndDish(long id) {
        //思路：把查询到的setmeal信息和setmeal_dish信息都放到setmealDto，用对象拷贝
        //查询setmeal信息
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto=new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        LambdaQueryWrapper<SetmealDish> lqw=new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId,setmeal.getId());

        //查询多条setmeal_dish信息
        List<SetmealDish> list = setmealDishService.list(lqw);

        //合并setmealDish
        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    /**
     * 修改套餐信息
     * @param setmealDto
     */
    @Override
    public void updateSetmealAndSetmeal_Dish(SetmealDto setmealDto) {

        //修改setmeal信息
        this.updateById(setmealDto);


        //清空setmeal_Dish
        LambdaQueryWrapper<SetmealDish> lqw=new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId,setmealDto.getId());

        setmealDishService.remove(lqw);

        Long setmealId=setmealDto.getId();

        //将新的setmeal_Dish信息存入数据库，在存入之前设置setmealId,因为setmealId之前被清除了
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        setmealDishes =   setmealDishes.stream().map(item ->{
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());


        setmealDishService.saveBatch(setmealDishes);



    }

    /**
     * 修改套餐状态
     * @param statusCode
     * @param ids
     */
    @Override
    public void updateStatus(int statusCode, List<Long> ids) {


        for (Long id : ids) {
             //查询套餐信息
             Setmeal setmeal = this.getById(id);
             //修改状态
             setmeal.setStatus(statusCode);

              this.updateById(setmeal);
        }



    }
}
