package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表 dish dishFlavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);

   //更新菜品信息和口味信息
   public void updateWithFlavor(DishDto dishDto);

    /**
     * 修改菜品状态信息
     * @param statu ids
     */
   public void updateStatus(int statu,long[] ids);


   //删除菜品信息
   public void delete(List<Long> ids);
}
