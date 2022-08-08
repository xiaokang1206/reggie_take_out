package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @RequestMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
log.info("订单数据 : {}",orders);
    orderService.submit(orders);
        return R.success("下单成功");
    }

   //http://localhost:8080/order/page?page=1&pageSize=10

    /**
     * 查看订单明细
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> view_order(int page,int pageSize,String number,String beginTime ,String endTime){

       // log.info("page: {} ,pageSize: {} ,number: {} " ,page,pageSize,number);

    log.info("beginTime: {} ,endTime: {}",beginTime,endTime);//2022-08-25 00:00:00

        Page<Orders> pageInfo=new Page<>(page,pageSize);

        LambdaQueryWrapper<Orders> lqw=new LambdaQueryWrapper<>();
        lqw.like(Strings.isNotEmpty(number),Orders::getNumber,number);
        // where and orderTime > beginTime and orderTime < endTime

            lqw.ge(Strings.isNotEmpty(beginTime),Orders::getOrderTime,beginTime);//大于等于
            lqw.le(Strings.isNotEmpty(endTime),Orders::getOrderTime,endTime);//小于等于


        lqw.orderByDesc(Orders::getOrderTime);

        orderService.page(pageInfo,lqw);

        return R.success(pageInfo);
    }

    /**
     * 派送订单
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> dispatch(@RequestBody Orders orders){

        log.info("Order: {}" ,orders);

        //修改订单的status为 3

           orderService.dispatch(orders);
        return R.success("派送成功");
    }
}
