package com.maple.net;

import com.maple.net.command.CommandManager;
import com.maple.utils.log.Log;
import com.maple.utils.log.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by 余炳 on 16/2/13.
 */
public class ServiceThread extends Thread {
    private static Log log = LogFactory.getLog(ServiceThread.class);

    private Map<SocketChannel,Date> socketMap = new HashMap<>();

    private Integer timeout = 20000;

    private Selector selector;
    private ServerSocketChannel server;

    private Object lock = new Object();

    private boolean exit = false;

    public ServiceThread(int port,int timeout) throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        this.timeout = timeout;
    }

    public void exit(){
        synchronized (lock){
            exit = true;
        }
    }


    @Override
    public void run(){
        while (true){
            synchronized (lock){
                if(exit){
                    break;
                }
            }

            try {
                listen();
            }
            catch (Exception e){
                break;
            }

            checkHand();

            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        try {
            selector.close();
        }
        catch (IOException e){
            log.error("Close selector error!",e);
        }

        for(SocketChannel channel:socketMap.keySet()){
            try {
                CommandManager.getManager().clearCommand(channel);
                channel.close();
            }
            catch (IOException e){
                log.error("Close socket error!",e);
            }
        }
        try {
            server.close();
        }
        catch (IOException e){
            log.error("Close server error!",e);
        }
    }

    private void checkHand(){
        Date date = new Date();
        List<SocketChannel> list = new ArrayList<>();
        for(Map.Entry<SocketChannel,Date> entry:socketMap.entrySet()){
            if(date.getTime() - entry.getValue().getTime() > timeout){
                list.add(entry.getKey());
            }
        }

        for(SocketChannel socketChannel:list){
            socketMap.remove(socketChannel);
            CommandManager.getManager().clearCommand(socketChannel);
            try {
                log.info("Socket ["+socketChannel.getRemoteAddress().toString()+"] time out");
                socketChannel.close();
            }
            catch (IOException e){
                log.error("Close socket error!",e);
            }
        }
    }

    public void read(SocketChannel channel){
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int count = 0;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while(true){
            try {
                int c = channel.read(buffer);
                if(c < 0){
                    log.error("Socket ["+channel+"] read error!");
                    synchronized (lock) {
                        socketMap.remove(channel);
                        CommandManager.getManager().clearCommand(channel);
                        channel.close();
                    }
                    return;
                }
                else if(c == 0){
                    break;
                }
                else{
                    out.write(buffer.array(),count,c);
                    count += c;
                }
            }
            catch (IOException e){
                log.error("Socket [" + channel + "] read error!");
                try {
                    synchronized (lock) {
                        socketMap.remove(channel);
                        CommandManager.getManager().clearCommand(channel);
                        channel.close();
                    }
                }
                catch (IOException ignored){
                }
                return;
            }
        }

        CommandManager.getManager().doCommand(channel,out.toByteArray());
    }

    private void listen() throws IOException {
        selector.select(100);
        Iterator iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();

            if(key.isAcceptable()){
                ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                try {
                    SocketChannel socketChannel = channel.accept();
                    socketMap.put(socketChannel,new Date());

                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);

                    log.info("Socket [" + socketChannel.getRemoteAddress().toString()+"] connected!");
                }
                catch (IOException e){
                    log.error("Accept error!",e);
                }

            }
            else if(key.isReadable()){
                synchronized (lock) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    try {
                        if (socketMap.containsKey(channel)) {
                            socketMap.put(channel, new Date());
                            NetThreadPool.read(this, channel);

                        } else {
                            channel.close();
                        }
                    } catch (IOException e) {
                        log.error("Read data error!", e);
                    }
                }
            }
            iterator.remove();
        }
    }
}
