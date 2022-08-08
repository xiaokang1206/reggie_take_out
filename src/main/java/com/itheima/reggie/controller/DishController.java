package com.itheima.reggie.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

     @Autowired
     private RedisTemplate redisTemplate;

     @Resource
     private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody  DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清理某个分类下面的菜品缓存数据
        String key= "dish_"+dishDto.getCategoryId() + "_1";

        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page ,int pageSize,String name){
        //构造分页构造器对象
        Page<Dish> pageInfo=new Page<Dish>(page,pageSize);
        Page<DishDto> dishDtoPage=new Page<DishDto>();
        //条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper=new LambdaQueryWrapper<Dish>();
        //添加过滤条件
        lambdaQueryWrapper.like(name!=null,Dish::getName,name);
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,lambdaQueryWrapper);

       //对象拷贝,拷贝的是分页相关信息，忽略records（分页数据）
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

                List<Dish> records=pageInfo.getRecords();

        List<DishDto> list= records.stream().map((item)->{

                    DishDto dishDto=new DishDto();
                    //拷贝的是数据信息
                    BeanUtils.copyProperties(item,dishDto);

                    Long categoryId = item.getCategoryId();
                    Category category = categoryService.getById(categoryId);
                    if(category!=null){
                        String categoryName = category.getName();
                        dishDto.setCategoryName(categoryName);
                    }

                   return dishDto;
                   //收集dishDto对象，转换成集合
                }).collect(Collectors.toList());


                dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public  R<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */

    @PutMapping
    public R<String> update(@RequestBody  DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
       String key= "dish_"+dishDto.getCategoryId() + "_1";

       redisTemplate.delete(key);


        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应的菜品分类
     * @param dish
     * @return
     */
 /*   @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<Dish>();

        queryWrapper.eq(dish!=null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        //排序
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList=null;
        //动态构造key
        String key ="dish_"+dish.getCategoryId()+"_"+dish.getStatus();//dish_15154846545848_1

        //先从redis中获得缓存数据

         dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

         if (dishDtoList !=null){
        //如果存在，直接返回，无需查询数据库
        return R.success(dishDtoList);
         }



           //缓存中不存在
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<Dish>();

        queryWrapper.eq(dish!=null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        //排序
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList= list.stream().map((item)->{

            DishDto dishDto=new DishDto();
            //拷贝的是数据信息
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);

            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper=new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);

            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
            //收集dishDto对象，转换成集合
        }).collect(Collectors.toList());
        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis

        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);



        return R.success(dishDtoList);
    }


  //修改菜品状态

    @PostMapping("/status/{statu}")
    public R<String> updateStatus(@PathVariable int statu , long[] ids ){

          log.info("status:",statu,"ids:", Arrays.asList(ids));


        dishService.updateStatus(statu,ids);

      return R.success("菜品状态修改成功");
    }

    /**
     * 删除菜品并删除菜品的口味信息
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){

        log.info("ids:",ids);

        dishService.delete(ids);

        return R.success("删除成功");

    }

}
