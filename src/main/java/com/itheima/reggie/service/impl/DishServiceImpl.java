package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
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

        //清理当前菜品对应的口味数据--dish_flavor的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
         queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

            dishFlavorService.remove(queryWrapper);

                 Long dishId = dishDto.getId();//获得DishId

        //添加当前提交过来的口味数据--dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors =  flavors.stream().map(item ->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);



    }

    /**
     * 修改菜品状态信息
     * @param statu ids
     */
    @Override
    public void updateStatus(int statu, long[] ids) {
       //通过id查询到菜品信息


        for (long id : ids) {
            Dish dish = this.getById(id);
            //设置菜品状态
            dish.setStatus(statu);
            //修改菜品状态
            this.updateById(dish);
        }


    }
// 删除菜品信息
    @Override
    public void delete(List<Long> ids) {

        Dish  dish=null;
        //判断菜品的状态
        for (Long id : ids) {
            dish = this.getById(id);
            if(dish==null || dish.getStatus()==1){
                //状态为1 启售，不可以删除，抛出异常
                throw new CustomException("删除失败");
            }
        }


        //状态为0 停售 ，可以删除，先删除dish
        this.removeByIds(ids);

        //删除dish_flavor中的数据
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(lambdaQueryWrapper);


    }


}
