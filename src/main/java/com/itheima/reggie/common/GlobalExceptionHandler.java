package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

/*
* 全局异常处理器
* */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 用户名称重复异常处理方法
     * @return
     */

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException exception){
        log.error(exception.getMessage());
// Duplicate entry 'zhangsan' for key 'idx_username'

        if(exception.getMessage().contains("Duplicate entry")){

            String[] split = exception.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return  R.error(msg);

        }

        return R.error("未知错误");
    }

    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException exception){
        log.error(exception.getMessage());

        return R.error(exception.getMessage());
    }


}
