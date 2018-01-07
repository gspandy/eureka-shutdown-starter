package com.zero.cloud.eureka.shutdown;

import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.zero.cloud.eureka.shutdown.event.FetchRegisterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;

/**
 * eureka客户端缓存信息更新
 * <p>
 * 2017-12-14 16:08
 *
 * @author scvzerng
 **/
@ConditionalOnBean({CachingSpringLoadBalancerFactory.class, EurekaRegistration.class})
@Slf4j
public class EurekaClientCacheUpdater {
    @Resource
    private CachingSpringLoadBalancerFactory cachingSpringLoadBalancerFactory;
    @Resource
    private EurekaRegistration eurekaRegistration;
    @Resource
    private ApplicationStatus applicationStatus;
    private volatile Map<String, FeignLoadBalancer> cache ;
    private Method fetchRegister;
    private CloudEurekaClient cloudEurekaClient;
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        try {
            Field field = cachingSpringLoadBalancerFactory.getClass().getDeclaredField("cache");
            field.setAccessible(true);

            cache = (Map<String, FeignLoadBalancer>) field.get(cachingSpringLoadBalancerFactory);
            field.setAccessible(false);
            if(AopUtils.isAopProxy(eurekaRegistration)|| AopUtils.isCglibProxy(eurekaRegistration)){
                cloudEurekaClient= (CloudEurekaClient) AopProxyUtils.getSingletonTarget(eurekaRegistration.getEurekaClient());
            }else{
                cloudEurekaClient = eurekaRegistration.getEurekaClient();
            }
            fetchRegister =  cloudEurekaClient.getClass().getSuperclass().getDeclaredMethod("fetchRegistry", boolean.class);
            fetchRegister.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 抓取注册中心信息并更新本地缓存
     * @return boolean
     */
    public boolean fetchRegister(){
        if(applicationStatus.getStatus()==MOVED_PERMANENTLY) {
            log.info("application is closing do not need refresh eureka cache");
            return true;
        }
        try {

            fetchRegister.invoke(cloudEurekaClient,false);
            cache.forEach((clientName,balance)->{
                if(balance.getLoadBalancer() instanceof ZoneAwareLoadBalancer){
                    ZoneAwareLoadBalancer zoneAwareLoadBalancer = (ZoneAwareLoadBalancer) balance.getLoadBalancer();
                    zoneAwareLoadBalancer.updateListOfServers();
                }
            });
            log.info("application eureka cache refresh success");

            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 是否真的需要更新缓存
     * 检查load balance cache是否存在该服务的 load balance
     * 如果不存在说明该服务没有依赖或者依赖了从来没有调用过下线的服务
     * 这样的服务不需要更新缓存
     * @param serviceName 下线的服务
     * @return
     */
    private boolean isNeedFetch(String serviceName){
        return serviceName!=null&&cache.keySet().contains(serviceName);
    }


    @EventListener
    public void refresh(FetchRegisterEvent fetchRegisterEvent){
        String service = (String) fetchRegisterEvent.getSource();
        if(isNeedFetch(service)){
            log.info("action from redis refresh eureka cache");
            this.fetchRegister();
        }else{
            if(eurekaRegistration.getServiceId().equals(service)){
                log.info("action from redis {} is self skip refresh eureka cache",service);
            }else{
                log.info("action from redis {} is not use skip refresh eureka cache",service);

            }
        }
    }
    }
