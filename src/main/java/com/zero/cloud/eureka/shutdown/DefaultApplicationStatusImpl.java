package com.zero.cloud.eureka.shutdown;

import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author scvzerng
 * Year: 2017-2017/12/7-19:16.
 */
public class DefaultApplicationStatusImpl implements ApplicationStatus {
    private AtomicInteger status = new AtomicInteger(0);

    @Override
    public HttpStatus getStatus() {
        return status.get()==0? HttpStatus.OK: HttpStatus.MOVED_PERMANENTLY;
    }

    @Override
    public void changeFree() {
        status.set(0);
    }

    @Override
    public void changeBusy() {
        status.set(1);
    }

}
