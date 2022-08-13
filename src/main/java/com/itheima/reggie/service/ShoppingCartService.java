package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {

    //删除购物车中的商品
    public void sub(ShoppingCart shoppingCart);

    //购物车新增
    ShoppingCart add(ShoppingCart shoppingCart);
}
