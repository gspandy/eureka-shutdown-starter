package com.zero.cloud.eureka.shutdown.config;

import com.zero.cloud.eureka.shutdown.enums.BroadcastType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author scvzerng
 * Year: 2017-2017/12/7-15:48
 */

@ConfigurationProperties(prefix = "eureka.instance.shutdown")
@Getter
@Setter
public class ShutdownProperties {
    /**
     * 广播类型
     */
    private BroadcastType broadcastType= BroadcastType.HTTP;
    /**
     * 快速关闭 使自身关闭的时候迅速的从注册中心删除
     */
    private boolean fast = true;
    /**
     * 超时强制关闭 单位秒
     */
    private int timeout = 30;
    /**
     * 检测间隔 单位秒
     */
    private int interval = 5;
    /**
     * 延迟广播其他client的时间 单位秒
     */
    private int broadcastDelay = 3;
    /**
     * 广播刷新缓存时的线程数
     */
    private int concurrentLimit = 40;
}
