package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.mapper.ShoppingCartMapper;
import com.itheima.reggie.service.ShoppingCartService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {


    /**
     * 购物车新增
     * @param shoppingCart
     * @return
     */
    @Override
    public ShoppingCart add(ShoppingCart shoppingCart) {

        //给userid createtime赋值
        Long currentId = BaseContext.getCurrentId();

        LocalDateTime dateTime = LocalDateTime.now();
        shoppingCart.setCreateTime(dateTime);
        shoppingCart.setUserId(currentId);

        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId,currentId);

        //判断加入购物车的是套餐还是菜品
        if(shoppingCart.getSetmealId()==null){
            //菜品
            lambdaQueryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        }else {
            //套餐
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }

        //查询数据库，判断是新增还是添加
        ShoppingCart one = this.getOne(lambdaQueryWrapper);
        if (one!=null){
            //增加
            Integer number = one.getNumber();
            //数量加一后再存储数据库
            one.setNumber(number+1);
            this.updateById(one);
        }else {
            //存储到数据库
            this.save(shoppingCart);
            one=shoppingCart;
        }

        return one;
    }







    /**
     * 删除购物车
     * @param shoppingCart
     */
    @Override
    public void sub(ShoppingCart shoppingCart) {
        //获取当前用户的id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        //添加删除条件，根据用户ID，菜品/套餐id
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper=new LambdaQueryWrapper<>();

        lambdaQueryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId());

        //判断当前需要删除的是菜品还是套餐
        if(shoppingCart.getSetmealId()!=null){
            //套餐
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());

        }else {
            //菜品
            lambdaQueryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());

        }
        //删除前先查询商品的数量
        ShoppingCart cart = this.getOne(lambdaQueryWrapper);
        Integer number = cart.getNumber();
        if(number==1){
            //数量==1，直接删除
            this.remove(lambdaQueryWrapper);
        }else {
            //数量 大于1 ，number-1
            cart.setNumber(number-1);
            this.updateById(cart);

        }


    }

}
