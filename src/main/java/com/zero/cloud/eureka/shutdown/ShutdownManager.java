package com.zero.cloud.eureka.shutdown;


import com.zero.cloud.eureka.shutdown.config.ShutdownProperties;
import com.zero.cloud.eureka.shutdown.service.Broadcasters;
import com.zero.cloud.eureka.shutdown.service.EurekaStatusService;
import com.zero.cloud.eureka.shutdown.service.HttpBroadcasters;
import com.zero.cloud.eureka.shutdown.web.CounterHandlerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 停机管理器
 * 用于控制服务停止之前的预备工作（修改健康检查页、修改SpringCloud注册中心中的该节点状态）
 * 确保该服务不再接受新请求，且已接收流量都处理完毕后，再进行关机。
 * 最终实现优雅停机。（业务请求0失败）
 *
 * @author scvzerng
 * Year: 2017-2017/12/7-19:22
 */
@Slf4j
public class ShutdownManager extends ShutdownEndpoint {

    @Resource
    private ShutdownProperties shutdownProperties;

    @Autowired(required = false)
    private EurekaStatusService eurekaStatusService;
    @Autowired(required = false)
    private Broadcasters broadcasters;

    @Resource
    private ApplicationStatus applicationStatus;

    @Resource
    private CounterHandlerInterceptor counterHandlerInterceptor;

    @Override
    public Map<String, Object> invoke() {

        int timeout = shutdownProperties.getTimeout();
        int interval = shutdownProperties.getInterval();
        applicationStatus.changeBusy();
        log.info("will shutdown timeout:{} interval:{} status:{}", timeout, interval, applicationStatus.getStatus().toString());
        doEurekaActions();
        long lastRequestCount = -1;
        while (true) {
            //超时处理
            if (timeout <= 0) {
                log.info("ShutdownManager timeout, will force shutdown.");
                break;
            }
            //正在处理的请求为0，且总处理请求数与上次一致，则认为流量已经切换完成。
            long requestingCount = counterHandlerInterceptor.getRequestingCount().get();
            long requestCount = counterHandlerInterceptor.getRequestCount().get();
            if (lastRequestCount >= 0
                    && requestingCount == 0
                    && requestCount == lastRequestCount) {
                log.info("shutdown success");
                break;
            } else {
                //记录下上次检测时的requestCount，用于确认是否有新流量进入
                lastRequestCount = requestCount;
                try {
                    timeout = timeout - interval;
                    log.info("ShutdownManager waiting left {}s, requestingCount: {}, requestCount: {}"
                            , timeout, requestingCount, requestCount);
                    Thread.sleep(interval * 1000);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        waitingBroadcastFinish();
        return super.invoke();
    }
    private void doEurekaActions(){
        if(eurekaStatusService!=null){
            //首先从注册中心移除自身
            eurekaStatusService.down();
            if(broadcasters!=null){
                //广播其他节点重新刷新缓存
                broadcasters.broadcast();
            }
        }
    }

    private void waitingBroadcastFinish(){
        if(broadcasters!=null){
            if(broadcasters instanceof HttpBroadcasters){
                try {
                    HttpBroadcasters.SEMAPHORE.acquire();
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }finally {
                    HttpBroadcasters.SEMAPHORE.release();
                }
            }
        }
    }

}