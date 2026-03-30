package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        log.info("用户登录：{}", userLoginDTO);
        //1、通过httpclient,构造登录凭证校验请求
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", userLoginDTO.getCode());
        paramMap.put("grant_type", "authorization_code");
        String res = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", paramMap);
        log.info("登录凭证校验请求结果：{}", res);
        //2、解析响应结果，获取openid
        JSONObject jsonObject = JSON.parseObject(res);
        String openid = jsonObject.getString("openid");
        if (openid == null) {
            log.error("登录失败，openid为空");
            throw new LoginFailedException(MessageConstant.USER_NOT_LOGIN);
        }
        //3、判断是否为新用户，根据openid查询数据库User表
        User user = userMapper.getByOpenid(openid);
        //4、如果是新用户，自动完成注册
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            user.setName("用户" + openid.substring(0,5));
            userMapper.insert(user);
        }
        //5、返回登录结果
        return user;
    }
}
