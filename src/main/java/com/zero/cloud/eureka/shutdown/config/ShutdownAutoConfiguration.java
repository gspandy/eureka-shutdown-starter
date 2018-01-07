package com.zero.cloud.eureka.shutdown.config;


import com.zero.cloud.eureka.shutdown.ApplicationStatus;
import com.zero.cloud.eureka.shutdown.DefaultApplicationStatusImpl;
import com.zero.cloud.eureka.shutdown.EurekaClientCacheUpdater;
import com.zero.cloud.eureka.shutdown.ShutdownManager;
import com.zero.cloud.eureka.shutdown.endpoint.ApplicationStatusEndpoint;
import com.zero.cloud.eureka.shutdown.endpoint.UpdateRegisterEndpoint;
import com.zero.cloud.eureka.shutdown.event.FetchRegisterEvent;
import com.zero.cloud.eureka.shutdown.service.*;
import com.zero.cloud.eureka.shutdown.web.CounterHandlerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * @author scvzerng
 * Year: 2017-2017/12/7-15:47
 */
@Configuration
@ConditionalOnExpression("${eureka.instanse.shutdown.fast:true}")
@EnableConfigurationProperties(ShutdownProperties.class)
public class ShutdownAutoConfiguration {

    /**
     * cloud的配置
     */
    @Configuration
    @AutoConfigureAfter(FeignRibbonClientAutoConfiguration.class)
    @ConditionalOnClass({EurekaRegistration.class, CachingSpringLoadBalancerFactory.class})
    public class EurekaAutoConfiguration{
        @Bean
        public EurekaClientCacheUpdater eurekaClientCacheUpdater(){
            return new EurekaClientCacheUpdater();
        }
        @Bean
        @ConditionalOnMissingBean
        public EurekaStatusService eurekaStatusService(){
            return  new DefaultEurekaStatusServiceImpl();
        }


            /**
             * redis 广播配置
             */
            @Configuration
            @ConditionalOnProperty(prefix = "eureka.instance.shutdown",name = "broadcastType",havingValue = "redis")
            @ConditionalOnClass({StringRedisTemplate.class,RedisConnectionFactory.class})
            @AutoConfigureAfter(RedisAutoConfiguration.class)
            public class BroadcastersRedisConfig{
                /**
                 * redis 广播接收端点
                 * @param redisConnectionFactory
                 * @param eventPublisher
                 * @return
                 */
                @Bean
                @ConditionalOnMissingBean
                public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, ApplicationEventPublisher eventPublisher){
                        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
                    redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
                    redisMessageListenerContainer.addMessageListener((message, bytes) -> eventPublisher.publishEvent(new FetchRegisterEvent(new String(message.getBody()))), new ChannelTopic(RedisBroadcasters.CHANNEL));
                    return redisMessageListenerContainer;
                }

                @Bean
                public Broadcasters broadcasters(){
                    return new RedisBroadcasters();
                }

            }

            /**
             * http广播配置
             */
            @Configuration
            @ConditionalOnProperty(prefix = "eureka.instance.shutdown",name = "broadcastType",havingValue = "http",matchIfMissing = true)
            public class BroadcastersHttpConfig{
                /**
                 * http 广播接收端点
                 * @return
                 */
                @Bean
                public UpdateRegisterEndpoint updateRegisterEndpoint(){
                    return new UpdateRegisterEndpoint();
                }

                @Bean
                public Broadcasters broadcasters(){
                    return new HttpBroadcasters();
                }
            }

    }


    @Bean
    @ConditionalOnMissingBean
    public CounterHandlerInterceptor counterHandlerInterceptor(){
        return new CounterHandlerInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationStatus applicationStatus(){
        return new DefaultApplicationStatusImpl();
    }
    @Bean
    public WebMvcConfigurerAdapter webMvcConfigurerAdapter(CounterHandlerInterceptor counterHandlerInterceptor){
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(counterHandlerInterceptor);
                super.addInterceptors(registry);
            }
        };
    }

    @Bean
    public ShutdownManager shutdownManager(){
        return new ShutdownManager();
    }

    @Bean
    public ApplicationStatusEndpoint applicationStatusEndpoint(){
        return new ApplicationStatusEndpoint();
    }


}
