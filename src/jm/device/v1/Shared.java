package jm.device.v1;

import java.util.ArrayList;
import jm.device.ConnectorType;
import jm.io.IPort;

final class Shared {
    public ConnectorType connector;
    public int buffID;
    public Buffer buff; //CommBox 有关信息数据
    public Information info; //维护COMMBOX数据缓冲区
    public byte[] cmdTemp; //写入命令缓冲区
    public int lastError; //提供错误查询
    public boolean isDb20;
    public boolean isDoNow;
    public int reqByteToByte;
    public int reqWaitTime;
    public int resByteToByte;
    public int resWaitTime;
    public IPort port;
    public int nextAddress;

    public Shared() {
        connector = ConnectorType.OBDII_16;
        buffID = 0;
        buff = new Buffer();
        info = new Information();
        cmdTemp = new byte[256];
        lastError = D.SUCCESS;
        isDb20 = false;
        isDoNow = true;
        reqByteToByte = 0;
        reqWaitTime = 0;
        resByteToByte = 0;
        resWaitTime = 0;
        nextAddress = 0;
    }
}
