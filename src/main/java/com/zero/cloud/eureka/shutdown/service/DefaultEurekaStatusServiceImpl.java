package com.zero.cloud.eureka.shutdown.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import javax.annotation.Resource;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.DOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;

/**
 * @author scvzerng
 * Year: 2017-2017/12/7-15:48
 */
@Slf4j
public class DefaultEurekaStatusServiceImpl implements EurekaStatusService {


    @Resource
    private EurekaRegistration registration;



    @Override
    public void up() {
        registration.getEurekaClient().setStatus(UP, registration.getApplicationInfoManager().getInfo());
    }

    @Override
    public void down() {
        registration.getEurekaClient().setStatus(DOWN, registration.getApplicationInfoManager().getInfo());
    }


}
