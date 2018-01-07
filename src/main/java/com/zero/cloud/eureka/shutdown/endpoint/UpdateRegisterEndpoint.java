package com.zero.cloud.eureka.shutdown.endpoint;


import com.zero.cloud.eureka.shutdown.EurekaClientCacheUpdater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;

/**
 * eureka缓存刷新端点
 * <p>
 * 2017-12-14 15:54
 *
 * @author scvzerng
 **/
@Slf4j
public class UpdateRegisterEndpoint extends AbstractEndpoint<ResponseEntity<String>> {

    @Resource
    EurekaClientCacheUpdater eurekaClientCacheUpdater;

    public UpdateRegisterEndpoint() {
        super("refreshEurekaCache",false);
    }

    @Override
    public ResponseEntity<String> invoke() {
        try {
            log.info("action from http refresh eureka cache");
            eurekaClientCacheUpdater.fetchRegister();
            return new ResponseEntity<>("eureka client cache refreshed", HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
