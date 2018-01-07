package com.zero.cloud.eureka.shutdown;

import org.springframework.http.HttpStatus;

/**
 * 控制健康检查返回的状态
 * @author scvzerng
 * Year: 2017-2017/12/7-19:22
 */

public interface ApplicationStatus {
    /**
     * 获取应用当前状态
     * @return
     */
    HttpStatus getStatus();

    /**
     * 空闲状态
     */
    void changeFree();

    /**
     * 忙碌状态
     */
    void changeBusy();
}
