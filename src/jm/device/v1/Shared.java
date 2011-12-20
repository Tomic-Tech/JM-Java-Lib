package jm.device.v1;

import jm.device.ConnectorType;
import jm.io.IPort;

public final class Shared {
    public ConnectorType connector = ConnectorType.OBDII_16;
    public int buffID = 0;
    public Buffer buff = new Buffer(); //CommBox 有关信息数据
    public Information info = new Information(); //维护COMMBOX数据缓冲区
    public byte[] cmdTemp = new byte[256];; //写入命令缓冲区
    public int lastError = D.SUCCESS; //提供错误查询
    public boolean isDb20 = false;
    public boolean isDoNow = true;
    public int reqByteToByte = 0;
    public int reqWaitTime = 0;
    public int resByteToByte = 0;
    public int resWaitTime = 0;
    public int nextAddress = 0;
    // only for w80
    public int boxTimeUnit = 0; //万分之一微妙
    public int timeBaseDB = 0; //标准时间的倍数
    public int timeExternDB = 0; //扩展时间的倍数
    public byte[] ports = new byte[D.MAXPORT_NUM]; //端口
    public byte[] buf = new byte[D.MAXBUFF_LEN]; //缓冲区
    public int pos = 0;
    public boolean isLink = false; //是否是链路保持块
    public int runFlag = 0;
    public int boxVer = 0;
    public int startPos = 0;
}
