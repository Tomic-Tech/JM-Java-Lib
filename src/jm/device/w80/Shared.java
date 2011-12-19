/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.w80;

import jm.io.IPort;

/**
 *
 * @author Ogilvy
 */
class Shared {
    public int boxTimeUnit = 0; //万分之一微妙
    public int timeBaseDB = 0; //标准时间的倍数
    public int timeExternDB = 0; //扩展时间的倍数
    public byte[] ports = new byte[D.MAXPORT_NUM]; //端口
    public boolean isDB20 = false;
    public boolean isDoNow = false;
    public byte[] buf = new byte[D.MAXBUFF_LEN]; //缓冲区
    public int pos = 0;
    public boolean isLink = false; //是否是链路保持块
    public int runFlag = 0;
    public int boxVer = 0;
    public int startPos = 0;
    public int reqByteToByte = 0;
    public int reqWaitTime = 0;
    public int resByteToByte = 0;
    public int resWaitTime = 0;
    public int lastError = 0;
}
