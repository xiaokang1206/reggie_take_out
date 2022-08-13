package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private RedisTemplate redisTemplate;

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

    @Override
    public R<Page> page_category(int page , int pageSize, String name) {
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
        this.page(pageInfo,lambdaQueryWrapper);

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
     * 查询菜品信息，移动端显示
     * @param dish
     * @return
     */
    @Override
    public List<DishDto> SelectDishAndDish_Flavor(Dish dish) {
        List<DishDto> dishDtoList=null;

        String key="dish_"+dish.getCategoryId()+"_"+dish.getStatus();


        //查询redis，如果redis中有数据直接返回
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if(dishDtoList!=null){

            return dishDtoList;

        }


        //redis中没有数据，查询数据库



        log.info("dish: {}" ,dish);//categoryId=1397844263642378242,status=1
        //获取参数
        Long categoryId = dish.getCategoryId();
        Integer status = dish.getStatus();

        //Dish条件构造
        LambdaQueryWrapper<Dish> lambdaQueryWrapper=new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(Dish::getCategoryId,categoryId);
        lambdaQueryWrapper.eq(Dish::getStatus,status);
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getCreateTime);



        //查询数据库，查询dish
        List<Dish> list = this.list(lambdaQueryWrapper);

        dishDtoList=  list.stream().map(itme ->{

            //创建dishdto对象，对象拷贝
            DishDto dishDto=new DishDto();
            BeanUtils.copyProperties(itme,dishDto);

            //查询category名称

            Category category = categoryService.getById(categoryId);

            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            Long dishId = itme.getId();
            //dish_flavor条件构造器
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper1=new LambdaQueryWrapper();
            lambdaQueryWrapper1.eq(DishFlavor::getDishId,dishId);


            //查询dish_flavor
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper1);

            dishDto.setFlavors(flavors);

            return dishDto;
        }).collect(Collectors.toList());
        //将查询到的数据备份到redis,设置60分钟过期

        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);


        return dishDtoList;
    }



}
