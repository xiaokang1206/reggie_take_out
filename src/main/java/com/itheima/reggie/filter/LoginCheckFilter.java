package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

//检查用户是否登录
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    private static final AntPathMatcher PATH_MATCHER =new AntPathMatcher();


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response= (HttpServletResponse) servletResponse;

        //1.获取本次请求的URI
        String requestURI=  request.getRequestURI();
        log.info("拦截到的路径: {}",requestURI);
         //定义不需要处理的请求,静态资源，登录退出页面放行
        String[] urls=new String[]{
                       "/employee/login",
                       "/employee/logout",
                       "/backend/**",
                       "/front/**",
                      "/common/**",//上传下载
                      "/user/sendMsg",//移动端发送短信
                      "/user/login",//移动端登录
                      "/doc.html",
                      "/webjars/**",
                      "/swagger-resources",
                       "/v2/api-docs"
        };

        //2.判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3.如果不需要处理，则直接放行
           if(check){
               log.info("本次请求{}不需要处理",requestURI);
               filterChain.doFilter(request,response);
               return;
           }
       // 4.判断登录状态，如果已登录，则直接放行
       if(  request.getSession().getAttribute("employee")!= null) {
           log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("employee"));

           Long empId = (Long) request.getSession().getAttribute("employee");

           BaseContext.setThreadLocal(empId);

           filterChain.doFilter(request, response);
           return;
       }

        // 4.1判断移动端用户登录状态，如果已登录，则直接放行
        if(  request.getSession().getAttribute("user")!= null) {
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");

            BaseContext.setThreadLocal(userId);


            filterChain.doFilter(request, response);
            return;
        }
        //  5.如果未登录则返回未登录结果，通过输出流向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));

        return;

    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURL
     * @return
     */
    public boolean check(String[] urls,String requestURL){

        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURL);
            if (match){
                return true;//不匹配
            }
        }
       return false;
    }


}
