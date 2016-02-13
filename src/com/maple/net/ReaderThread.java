package com.maple.net;

import java.nio.channels.SocketChannel;

/**
 * Created by 余炳 on 16/1/31.
 */
public class ReaderThread extends Thread {

    private boolean working = false;

    private Object lock = new Object();

    private boolean exit = false;

    private ServiceThread serviceThread = null;
    private SocketChannel channel = null;

    public boolean isWorking() {
        return working;
    }

    public void exit(){
        synchronized (lock){
            exit = true;
        }
    }

    public boolean read(ServiceThread serviceThread,SocketChannel channel){
        if(isWorking()){
            return false;
        }
        synchronized (lock){
            this.serviceThread = serviceThread;
            this.channel = channel;
            working = true;
        }
        return true;
    }

    @Override
    public void run(){
        while(true){
            synchronized (lock) {
                if (exit) {
                    break;
                }
            }
            if(working) {
                serviceThread.read(channel);
                NetThreadPool.readFinish(channel);
            }

            synchronized (lock) {
                working = false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
