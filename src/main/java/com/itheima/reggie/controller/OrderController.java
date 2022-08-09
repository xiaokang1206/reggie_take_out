package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;


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

    /**
     * 查询用户个人中心的订单信息
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize){
        log.info("page: {} ,pageSize: {}",page,pageSize);
        //select * from orders o, order_detail od where o.id=od.order_id and o.id=1554475744128696322
       //分页构造器
        Page<Orders> pageInfo =new Page<>(page,pageSize);
        Page<OrdersDto> pageDto =new Page<>();
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();

        //条件构造器
        LambdaQueryWrapper<Orders> lqw=new LambdaQueryWrapper<>();
        lqw.eq(Orders::getUserId,currentId);
        lqw.orderByDesc(Orders::getOrderTime);

        //查询当前用户的订单信息
        orderService.page(pageInfo,lqw);

        //将当前用户的分页信息，拷贝到Dto中
        BeanUtils.copyProperties(pageInfo,pageDto,"records");

        //获取订单数据
        List<Orders> records = pageInfo.getRecords();



   List<OrdersDto> dtoList =  records.stream().map(item ->{

            OrdersDto ordersDto=new OrdersDto();
            //将订单数据拷贝到Dto中
            BeanUtils.copyProperties(item,ordersDto);
            //查询到所有的订单细节信息
            List<OrderDetail> list = orderDetailService.list();

            List<OrderDetail> currentOrderDetail=new ArrayList<OrderDetail>();


           //将OrderDetail数据拷到Dto中
            for(OrderDetail orderDetail : list ){

               long orderId = orderDetail.getOrderId();
               long id= item.getId();

                //判断订单明细是不是当前用户的订单明细
                if(orderId==id){
                    //是当前用户的订单明细
                    currentOrderDetail.add(orderDetail);
                    //把订单明细加到Dto中
                    ordersDto.setOrderDetails(currentOrderDetail);
                }

            }

            return ordersDto ;
        }).collect(Collectors.toList());
         //把Dto放到records中
        pageDto.setRecords(dtoList);

        //返回OrdersDto，同时显示OrdersDto和OrderDetile中的内容
        return R.success(pageDto);
    }
}
