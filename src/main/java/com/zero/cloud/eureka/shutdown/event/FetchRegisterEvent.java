package com.zero.cloud.eureka.shutdown.event;

import org.springframework.context.ApplicationEvent;

/**
 * eureka缓存刷新事件
 * <p>
 * 2017-12-20 13:34
 *
 * @author scvzerng
 **/
public class FetchRegisterEvent extends ApplicationEvent {
    public FetchRegisterEvent(Object source) {
        super(source);
    }
}
