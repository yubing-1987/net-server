package com.maple.net;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 余炳 on 16/1/31.
 */
public class NetThreadPool {

    private static NetThreadPool instance = null;

    private ReaderThread [] readerThread;

    private Object lock = new Object();

    private Map<SocketChannel,ServiceThread> readReady = new HashMap<>();
    private List<SocketChannel> readReadyList = new ArrayList<>();
    private Map<SocketChannel,ReaderThread> reading = new HashMap<>();

    public static void create(int readCount, int writeCount){
        instance = new NetThreadPool(readCount,writeCount);
    }

    public static NetThreadPool getInstance(){
        return instance;
    }

    public static void read(ServiceThread thread,SocketChannel channel){
        instance._read(thread,channel);
    }

    public NetThreadPool(int readCount, int writeCount){
        if(readCount <= 0){
            readCount = 8;
        }
        if(writeCount <= 0){
            writeCount = 8;
        }
        readerThread = new ReaderThread[readCount];
        for(int i = 0;i<readCount;i++){
            readerThread[i] = new ReaderThread();

            readerThread[i].start();
        }
    }

    public static void readFinish(SocketChannel channel){
        instance._readFinish(channel);
    }

    private void _readFinish(SocketChannel channel){
        synchronized (lock) {
            reading.remove(channel);
            readReady.remove(channel);
            readReadyList.remove(channel);
        }
        if (readReady.size() > 0) {
            SocketChannel c;
            ServiceThread thread;
            synchronized (lock) {
                c = readReadyList.get(0);
                thread = readReady.get(c);
                readReady.remove(c);
                readReadyList.remove(c);
            }
            _read(thread, c);
        }
    }

    private void _read(ServiceThread thread,SocketChannel channel){
        synchronized (lock) {
            if (readReady.containsKey(channel) || reading.containsKey(channel)) {
                return;
            }
            if (readReady.size() > 0) {
                readReady.put(channel, thread);
                readReadyList.add(channel);
                return;
            }
            for (ReaderThread th : readerThread) {
                if (!th.isWorking()) {
                    if (th.read(thread, channel)) {
                        reading.put(channel, th);
                        return;
                    }
                }
            }

            readReady.put(channel, thread);
            readReadyList.add(channel);
        }
    }

    public void exit(){
        for(ReaderThread thread:readerThread){
            thread.exit();
        }
    }
}
