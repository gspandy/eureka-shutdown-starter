package com.zero.cloud.eureka.shutdown.endpoint;

import com.zero.cloud.eureka.shutdown.ApplicationStatus;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;

/**
 * 健康检查地址 http://ip:port/status
 * 301正在停止服务
 * 200 正常
 * @author scvzerng
 * Year: 2017-2017/12/7-19:28
 */

public class ApplicationStatusEndpoint extends AbstractEndpoint<ResponseEntity<String>> {

    @Resource
    ApplicationStatus applicationStatus;

    public ApplicationStatusEndpoint() {
        super("status",false);
    }


    @Override
    public ResponseEntity<String> invoke() {
        return new ResponseEntity<>(applicationStatus.getStatus());
    }
}
