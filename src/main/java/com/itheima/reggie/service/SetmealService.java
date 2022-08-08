package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDith(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     * @param ids
     */
    public  void removeWithDish(List<Long> ids);

     //根据id查询套餐信息和菜品
   public SetmealDto getSetmealAndDish(long id);
    //修改套餐信息
   public void updateSetmealAndSetmeal_Dish(SetmealDto setmealDto);

   //修改套餐状态
   public void updateStatus(int statusCode, List<Long> ids);
}

