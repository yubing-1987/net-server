package com.maple.net.command;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 余炳 on 16/1/31.
 */
public class CommandManager {
    private Map<SocketChannel,BaseCommand> commandMap = new HashMap<>();

    private static CommandManager manager = new CommandManager();

    public static CommandManager getManager(){
        return manager;
    }

    public void clearCommand(SocketChannel channel){
        commandMap.remove(channel);
    }

    public int doCommand(SocketChannel socketChannel,byte [] data){
        BaseCommand command = null;

        byte [] temp = data;
        if(data[0] == 'b'
                && data[1]=='&'
                && data[2] == '\16'
                && data[3] == '\30'){
            command = makeCommand(data[4]);
            if(command != null){
                temp = new byte[data.length - 5];
                System.arraycopy(data,5,temp,0,data.length - 5);
            }
        }
        if(command != null){
            commandMap.remove(socketChannel);
        }
        else{
            command = commandMap.get(socketChannel);
        }
        if(command == null){
            return -1;
        }
        if(command.loadData(temp)){
            commandMap.remove(socketChannel);
            if(command.isValid()) {
                command.doCommand(socketChannel);
            }
        }
        else{
            commandMap.put(socketChannel,command);
        }
        return 0;
    }

    private BaseCommand makeCommand(byte key){
        return null;
    }
}
