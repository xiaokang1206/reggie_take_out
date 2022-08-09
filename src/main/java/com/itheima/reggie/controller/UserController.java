package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private JavaMailSender javaMailSender;

    //发送人
    private String form = "1833976463@qq.com";
    //接收人
    private String to;
    //标题
    private String subject = "菩提阁登录验证";
    //正文
    private String context ;



    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user , HttpSession session){
      /*    //获得邮箱
        String phone= user.getPhone();
        //生成随机的4位验证码
        String code = ValidateCodeUtils.generateValidateCode(4).toString();

        context=code;
        log.info("code= {}",code);

        if (StringUtils.isNotEmpty(phone)) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
                helper.setFrom(form);//发件人
                helper.setTo(phone);//收件人
                helper.setSubject(subject);//标题
                helper.setText("本次验证码为：" + context);//正文

                javaMailSender.send(mimeMessage);
                session.setAttribute(phone,code);
                return R.success("邮箱验证码短信发送成功");

            } catch (Exception e) {
                e.printStackTrace();
                throw new CustomException("短信发送失败");
            }

        }

*/
        //获取手机号
        String phone = user.getPhone();
        if (StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
         log.info("code= {}",code);

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存到Session
            //session.setAttribute(phone,code);

            //将生成的验证码存储到redis中，并设置有效时间5分钟
           redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map , HttpSession session){

        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取保存的验证码
        //String codeInSession = (String) session.getAttribute(phone);

        //从redis中获取验证码
        String codeInSession = (String) redisTemplate.opsForValue().get(phone);

        //进行验证码的比对 (页面提交的验证码和Session中保存的验证码比对)
        if(codeInSession !=null && codeInSession.equals(code)){

            //如果比对成功，说明登录成功
            //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
            LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
               lambdaQueryWrapper.eq(User::getPhone,phone);
             User user= userService.getOne(lambdaQueryWrapper);

             if(user==null){
                //用户不存在，则注册
                 user =new User();
                 user.setPhone(phone);
                 user.setStatus(1);

                 userService.save(user);

             }
              session.setAttribute("user",user.getId());
             //如果用户登录成功，删除Redis中缓存的验证码
            redisTemplate.delete(phone);

         return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> loginOut(HttpSession session){

       //删除Session
        session.removeAttribute("user");

        return R.success("退出成功");
    }

}
