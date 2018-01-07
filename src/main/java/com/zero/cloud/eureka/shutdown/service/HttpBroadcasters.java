package com.zero.cloud.eureka.shutdown.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.zero.cloud.eureka.shutdown.config.ShutdownProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * http广播
 * <p>
 * 2017-12-20 13:53
 *
 * @author scvzerng
 **/
@Slf4j
public class HttpBroadcasters implements Broadcasters,DisposableBean {
    public static final Semaphore SEMAPHORE = new Semaphore(1);
    private ExecutorService broadcastExecutorService;
    private ScheduledExecutorService broadcastScheduledExecutorService;
    @Resource
    private EurekaRegistration registration;
    @Resource
    private ShutdownProperties shutdownProperties;
    private static final ResponseErrorHandler UN_PROCESS_ERROR_HANDLER = new ResponseErrorHandler() {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {

        }
    };

    @Override
    public void broadcast() {
        try {
            SEMAPHORE.acquire();
            log.debug("prepare broadcast all eureka client nodes");
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        Collection<Callable<Void>> tasks = getBroadcastTasks();
        log.debug("{} eureka client need broadcast",tasks.size());
        //%d秒后执行
        delayInvoke(()->{
            broadcastExecutorService = getBroadcastPool(shutdownProperties.getConcurrentLimit(),tasks.size());

            try {
                broadcastExecutorService.invokeAll(tasks);

            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }finally {
                SEMAPHORE.release();
                log.debug("broadcast finish");
            }
        },shutdownProperties.getBroadcastDelay());

    }



    /**
     * 延迟调用 TODO 无法使用Timer或者直接创建Thread
     * @param runnable
     * @param seconds
     */
    private void delayInvoke(Runnable runnable,long seconds){
        log.debug("broadcast task submit {}s will invoke",seconds);
        broadcastScheduledExecutorService =  new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("broadcast-schedule");
            return thread;
        });

        broadcastScheduledExecutorService.schedule(runnable,seconds, TimeUnit.SECONDS);

    }

    /**
     * 获取广播线程池 由于是关闭应用时调用资源可以给大些
     * 临时创建避免挤占应用资源
     * @param concurrentLimit 线程数
     * @param size 队列大小
     * @return
     */
    private ThreadPoolExecutor getBroadcastPool(int concurrentLimit, int size){
        log.debug("broadcast thread pool create concurrent:{} queue:{}",concurrentLimit,size);
        return new ThreadPoolExecutor(
                concurrentLimit,
                concurrentLimit,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(size),
                new ThreadFactory() {
                    int i=0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("broadcast-"+i++);
                        return thread;
                    }
                }
        );
    }

    /**
     * 获取所有广播任务
     * @return
     */
    private Collection<Callable<Void>> getBroadcastTasks(){
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(UN_PROCESS_ERROR_HANDLER);
        return registration.getEurekaClient()
                .getApplications()
                .getRegisteredApplications()
                .stream()
                .map(Application::getInstances)
                .flatMap(List::stream)
                .filter(filterSelf())
                .map(instanceInfo -> (Callable<Void>)()->{
                    try {
                        ResponseEntity<String> responseEntity = restTemplate.getForEntity(instanceInfo.getHomePageUrl() + "refreshEurekaCache", String.class);
                        log.info("{} broadcast success status:{} message:{}", instanceInfo.getHomePageUrl(), responseEntity.getStatusCode(), responseEntity.getBody());
                    }catch (Exception e){
                        log.error("{} broadcast fail {}",instanceInfo.getHomePageUrl(),e.getMessage());
                    }
                    return null;
                })
                .collect(toList());
    }

    private Predicate<InstanceInfo> filterSelf(){
        return instanceInfo->!registration.getApplicationInfoManager().getInfo().equals(instanceInfo);
    }

    @Override
    public void destroy() throws Exception {
        if(broadcastScheduledExecutorService!=null&&broadcastExecutorService!=null){
            try {
                broadcastScheduledExecutorService.shutdown();
                broadcastExecutorService.shutdown();

            }finally {
                List<Runnable> tasks = broadcastExecutorService.shutdownNow();
                log.debug("broadcast tasks {} task fail",tasks.size());
                broadcastScheduledExecutorService.shutdownNow();
            }
        }

    }
}
