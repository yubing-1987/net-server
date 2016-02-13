package com.maple.net.command;

import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * Created by 余炳 on 16/1/31.
 */
public abstract class BaseCommand {
    private Date lastDate = new Date();
    protected long timeout = 10000;
    protected boolean valid = true;
    public boolean loadData(byte [] data){
        Date now = new Date();
        if(now.getTime() - lastDate.getTime() > timeout){
            valid = false;
            return true;
        }else{
            lastDate = now;
            return false;
        }
    }
    public abstract void    doCommand(SocketChannel channel);
    public boolean isValid(){
        return valid;
    }
}
