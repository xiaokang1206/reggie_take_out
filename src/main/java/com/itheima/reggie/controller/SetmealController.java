package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@Slf4j
@RequestMapping("/setmeal")
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;


    /**
     * 新增套餐,同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    @ApiOperation("新增套餐接口")
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息: {}" ,setmealDto);

        setmealService.saveWithDith(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */

@GetMapping("/page")
@ApiOperation(value = "套餐分页查询接口")
@ApiImplicitParams({
        @ApiImplicitParam(name = "page",value = "页码",required = true),
        @ApiImplicitParam(name = "pageSize",value = "每页记录数",required = true),
        @ApiImplicitParam(name = "name",value = "套餐名称",required = false)

})
    public R<Page> page(int page,int pageSize,String name){


    //分页构造器对象
    Page<Setmeal> pageInfo=new Page<>(page,pageSize);
    Page<SetmealDto> dtoPage=new Page<>();

    //分页条件构造器
    LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();


    queryWrapper.like(name!=null,Setmeal::getName,name);
    queryWrapper.orderByDesc(Setmeal::getUpdateTime);

    setmealService.page(pageInfo,queryWrapper);



    //拷贝基础分页数据，重新定义pageInfo中的记录
    BeanUtils.copyProperties(pageInfo,dtoPage,"records");
       List<Setmeal> records = pageInfo.getRecords();
       List<SetmealDto> setmealDtoList = null;

    setmealDtoList =  records.stream().map((item)->{
           SetmealDto setmealDto=new SetmealDto();
            //拷贝对象数据
           BeanUtils.copyProperties(item,setmealDto);

           Long categoryId = item.getCategoryId();
           Category category = categoryService.getById(categoryId);

           if(category!=null){
               String categoryName = category.getName();
               setmealDto.setCategoryName(categoryName);
           }

           return setmealDto;

       }).collect(Collectors.toList());

            dtoPage.setRecords(setmealDtoList);

        return R.success(dtoPage);

    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    //allEntries = true:删除该分类下所有的缓存数据
    @CacheEvict(value = "setmealCache",allEntries = true)
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
    log.info("ids: {}" ,ids);
    setmealService.removeWithDish(ids);

    return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询套餐信息
     * @param setmeal
     * @return
     */

    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId+'_'+#setmeal.status")
    public R<List<Setmeal>> list( Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();

        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);


        List<Setmeal> list = setmealService.list(queryWrapper);


        return R.success(list);
    }

    /**
     * 根据id查询套餐信息
     * @param id
     */
    @GetMapping ("/{id}")
    public R<SetmealDto> getById(@PathVariable long id){


     SetmealDto setmeal= setmealService.getSetmealAndDish(id);
             return R.success(setmeal);
    }

    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){

setmealService.updateSetmealAndSetmeal_Dish(setmealDto);

        return R.success("套餐修改成功");

    }
    @PostMapping("/status/{statusCode}")
    public R<String> status(@PathVariable int statusCode ,@RequestParam  List<Long> ids ){

     log.info("statusCode: {} ,ids: {}",statusCode,ids);

     setmealService.updateStatus(statusCode,ids);


           return R.success("菜品状态修改成功");
    }

}
