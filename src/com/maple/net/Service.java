package com.maple.net;

import com.maple.utils.lifecycle.LifecycleBase;
import com.maple.utils.lifecycle.LifecycleException;

import java.io.IOException;

/**
 * Created by 余炳 on 16/1/31.
 */
public class Service extends LifecycleBase {

    private ServiceThread serviceThread = null;
    private Integer port;
    private Integer timeout = 20000;

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    protected void initInternal() throws LifecycleException {
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            serviceThread = new ServiceThread(port,timeout);
            serviceThread.start();
        } catch (IOException e) {
            throw new LifecycleException(e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        serviceThread.exit();
    }

    @Override
    protected void destroyInternal() throws LifecycleException {

    }
}
