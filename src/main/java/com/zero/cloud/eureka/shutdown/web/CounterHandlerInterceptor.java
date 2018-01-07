package com.zero.cloud.eureka.shutdown.web;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 调用计数器
 *
 * @author scvzerng
 * Year: 2017-2017/12/7-18:44
 */
@Slf4j
@Getter
public class CounterHandlerInterceptor implements HandlerInterceptor {

    /**
     * 记录正在处理的请求
     */
    private final AtomicLong requestingCount = new AtomicLong(0);
    /**
     * 记录已经处理完成的请求（20171210-CLuo）
     */
    private final AtomicLong requestCount = new AtomicLong(0);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        requestingCount.getAndIncrement();
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        requestingCount.getAndDecrement();
        //requestCount溢出重置，避免超出long型上限。
        if (requestCount.incrementAndGet() >= Long.MAX_VALUE) {
            requestCount.set(0);
        }
    }
}
