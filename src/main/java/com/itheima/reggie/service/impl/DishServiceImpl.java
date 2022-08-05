package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Override
    public void saveWithFlavor(DishDto dishDto) {

        //保存菜品的基本信息道菜品表dish
        this.save(dishDto);
        Long dishId = dishDto.getId();//菜品id
              //保存菜品
       // dishFlavorService.saveBatch(dishDto.getFlavors());
        List<DishFlavor> flavors = dishDto.getFlavors();
     //给flavors中的每一个元素的id赋值，再重新给list赋值
        flavors =  flavors.stream().map(item ->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

      /*  for (DishFlavor flavor : flavors) {

            flavor.setDishId(dishId);
        }*/

        //保存菜品的口味数据到dish_flavor表
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
       //查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);

        DishDto dishDto=new DishDto();

        BeanUtils.copyProperties(dish,dishDto);


        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        dishDto.setFlavors(flavors);


        return dishDto;
    }

    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);


        //传统修改：比如两条数据去覆盖三条数据，会比较繁琐
        //清理当前菜品对应的口味数据--dish_flavor的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
         queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

            dishFlavorService.remove(queryWrapper);

                 Long dishId = dishDto.getId();

        //添加当前提交过来的口味数据--dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors =  flavors.stream().map(item ->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);



    }
}
