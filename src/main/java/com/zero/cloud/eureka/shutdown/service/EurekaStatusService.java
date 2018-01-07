package com.zero.cloud.eureka.shutdown.service;


/**
 * 控制当前eureka实例再注册中心的状态变更
 * @author scvzerng
 * Year: 2017-2017/12/7-15:38
 */

public interface EurekaStatusService {
    /**
     * 从注册中心手动上线
     */
    void up();
    /**
     * 从注册中心手动下线
     */
    void down();



}
