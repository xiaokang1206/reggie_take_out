package com.itheima.reggie.service.impl;

import com.alibaba.druid.util.Utils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.ws.Action;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class OrdersServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;



    /**
     * 用户下单
     * @param orders
     */
    @Override
    public void submit(Orders orders) {
         //获得当前用户的id
        Long userId = BaseContext.getCurrentId();


        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        if(shoppingCarts==null || shoppingCarts.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }
        //查询用户数据
        User user = userService.getById(userId);

        //查询地址信息
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook==null){
            throw new CustomException("用户地址信息有误，不能下单");
        }


        long orderId = IdWorker.getId();//订单号


        AtomicInteger amount=new AtomicInteger(0);//初始值为0

     //把购物车中的信息放入订单明细表
     List<OrderDetail> orderDetails =  shoppingCarts.stream().map((item)->{

         OrderDetail orderDetail=new OrderDetail();
         orderDetail.setOrderId(orderId);
         orderDetail.setNumber(item.getNumber());//商品的数量
         orderDetail.setDishFlavor(item.getDishFlavor());
         orderDetail.setDishId(item.getDishId());
         orderDetail.setSetmealId(item.getSetmealId());
         orderDetail.setName(item.getName());
         orderDetail.setImage(item.getImage());
         orderDetail.setAmount(item.getAmount());
         //初始值0 +商品1单价 X 商品1数量 + 商品2单价 X 商品2数量 + 商品3单价 X 商品3数量 ...
         //以原子方式将给定值添加到当前值，并在添加后返回新值。
         amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());

         return orderDetail;

        }).collect(Collectors.toList());

        //向订单表插入数据，一条数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);//待派送
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);


        //清空购物车
         shoppingCartService.remove(queryWrapper);

    }

    /**
     * 订单派送
     * @param orders
     */
    @Override
    public void dispatch(Orders orders) {
       //查询需要派送的订单
        Orders ordersInfo = this.getById(orders.getId());

        //修改订单的状态为3
        ordersInfo.setStatus(orders.getStatus());
        this.updateById(ordersInfo);

    }
}
