/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1;

import jm.io.IPort;

/**
 *
 * @author Ogilvy
 */
public abstract class Box {

    private IPort _port;
    private Shared _shared;
    private D _d;

    public Box(IPort port, Shared shared, D d) {
        _port = port;
        _shared = shared;
        _d = d;
    }

    public IPort getPort() {
        return _port;
    }

    public Shared getShared() {
        return _shared;
    }

    public D getD() {
        return _d;
    }

    public abstract boolean openComm();
    
    public abstract boolean closeComm();
    
    public abstract int readBytes(byte[] buff, int offset, int count);
    
    public abstract boolean setCommCtrl(int valueOpen, int valueClose);
    
    public abstract boolean setCommLine(int sendLine, int recvLine);
    
    public abstract boolean setCommLink(int ctrlWord1, int ctrlWord2, int ctrlWord3);
    
    public abstract boolean setCommBaud(double baud);
    
    public abstract boolean setCommTime(int type, int time);
    
    public abstract boolean setLineLevel(int valueLow, int valueHigh);
    
    public abstract boolean commboxDelay(int time);
    
    public abstract boolean turnOverOneByOne();
    
    public abstract boolean stopNow(boolean isStopExecute);
    
    public abstract boolean newBatch(int buffID);
    
    public abstract boolean delBatch(int buffID);
    
    public abstract boolean checkResult(int microSeconds);
    
    public abstract boolean keepLink(boolean isRunLink);
    
    public abstract boolean sendOutData(int offset, int count, byte... buffer);
    
    public abstract boolean runReceive(int type);
    
    public abstract boolean endBatch();
    
    public abstract boolean runBatch(boolean isExecuteMany, int... buffID);
    
    public abstract int readData(byte[] buff, int offset, int count, int microSeconds);
    
    public abstract boolean updateBuff(int type, byte[] buffer);
    
    public abstract int getAbsAdd(int buffID, int add);
}
