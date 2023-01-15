package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utiles.SMSUtils;
import com.itheima.reggie.utiles.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //把前端传过来的数据，用一个user对象来装
        //获取手机号
        String phone = user.getPhone();
        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("code:{}",code);

            //调用阿里云提供的短信服务API完成发送短信
            SMSUtils.sendMessage("哒哒哒外卖","SMS_268455228",phone,code);

            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        //用一个map容器来装前端传过来的phone和code数据，这里的code是用户输入的code，用于和服务端生成的code进行比对校验的
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);

        //进行验证码比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            //这里大写的User表示的用户数据表
            queryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否是新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }

            //如果能够比对成功，说明登录成功
            session.setAttribute("user",user.getId());
            return R.success(user);
        }

        return R.error("用户登录失败");
    }
}
