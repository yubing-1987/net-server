package com.maple.net;

import java.nio.channels.SocketChannel;

/**
 * Created by 余炳 on 16/1/31.
 */
public interface SocketReadListener {
    int read(SocketChannel channel);
}
