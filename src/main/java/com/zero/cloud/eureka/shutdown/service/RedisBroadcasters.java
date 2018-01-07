package com.zero.cloud.eureka.shutdown.service;

import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

/**
 * redis 发布订阅广播
 * <p>
 * 2017-12-20 13:53
 *
 * @author scvzerng
 **/
public class RedisBroadcasters implements Broadcasters {

    public  static final String CHANNEL = "yazuo:framework:shutdown:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private EurekaRegistration registration;
    @Override
    public void broadcast() {
        stringRedisTemplate.convertAndSend(CHANNEL,registration.getServiceId());

    }


}
