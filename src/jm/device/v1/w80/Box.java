/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1.w80;

import java.io.IOException;
import java.util.Random;
import jm.device.v1.D;
import jm.device.v1.Shared;
import jm.io.IPort;
import jm.io.SerialPort;

/**
 *
 * @author Ogilvy
 */
public final class Box extends jm.device.v1.Box {

    public Box(IPort port, Shared shared) {
        super(port, shared, new D(true));
    }

    private boolean checkIdle() {
        try {
            int rb = D.READY;
            int avail = getPort().bytesAvailable();
            if (avail > 20) {
                getPort().discardInBuffer();
                return true;
            }
            while (avail > 0) {
                getPort().readByte();
                avail--;
            }
            if (rb == D.READY || rb == D.ERROR) {
                return true;
            }
            rb = getPort().readByte(200);
            if (rb == D.READY || rb == D.ERROR) {
                return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean checkSend() {
        try {
            int rb = 0;
            rb = getPort().readByte(200);
            if (rb == D.RECV_OK) {
                return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean checkResult(int time) {
        try {
            int rb = getPort().readByte(time / 1000);
            if (rb == D.READY || rb == D.ERROR) {
                getPort().discardInBuffer();
                return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean sendCmd(int cmd, int offset, int count, byte... data) {
        int cs = cmd;
        cmd += getShared().runFlag;
        for (int i = 0; i < count; i++) {
            cs += data[i + offset] & 0xFF;
        }

        byte[] command = new byte[2 + count];
        command[0] = (byte)(cmd);
        if (count > 0) {
            System.arraycopy(data, offset, command, 1, count);
        }
        command[command.length - 1] = (byte)(cs);

        for (int i = 0; i < 3; i++) {
            if (!checkIdle()) {
                continue;
            }
            try {
                getPort().write(command, 0, command.length);
            } catch (IOException ex) {
                continue;
            }
            if (checkSend()) {
                return true;
            }
        }
        return false;
    }

    public int readData(byte[] buff, int offset, int count, int microSeconds) {
        try {
            return getPort().read(buff, offset, count, microSeconds / 1000);
        } catch (IOException ex) {
            try {
                int avail = getPort().bytesAvailable();
                if (avail > 0) {
                    if (avail <= count) {
                        return getPort().read(buff, offset, avail);
                    } else {
                        return getPort().read(buff, offset, count);
                    }
                }
                return avail;
            } catch (IOException ex1) {
                return 0;
            }
        }
    }

    public int readBytes(byte[] buff, int offset, int count) {
        return readData(buff, offset, count, 500000);
    }

    public int getCmdData(byte[] receiveBuffer, int maxLength) {
        byte[] cs = new byte[1];
        byte[] len = new byte[1];
        if ((readBytes(receiveBuffer, 0, 1) != 1)
                || (readBytes(len, 0, 1) != 1)) {
            return 0;
        }
        if ((len[0] & 0xFF) > maxLength) {
            len[0] = (byte)(maxLength);
        }
        if (readBytes(receiveBuffer, 0, len[0] & 0xFF) != (len[0] & 0xFF)) {
            return 0;
        }
        if (readBytes(cs, 0, 1) != 1) {
            return 0;
        }
        return len[0] & 0xFF;
    }

    private boolean doCmd(int cmd, int offset, int count, byte... buff) {
        byte[] temp = null;
        getShared().startPos = 0;
        if (cmd != getD().WR_DATA && cmd != getD().SEND_DATA) {
            cmd |= count; //加上长度位
        }
        if (getShared().isDoNow) {
            //发送到BOX执行
            if (cmd == getD().WR_DATA) {
                temp = new byte[2 + count];
                if (count == 0) {
                    return false;
                }
                if (getShared().isLink) {
                    temp[0] = (byte)(0xFF); //写链路保持
                } else {
                    temp[0] = 0; //写通讯命令
                }
                temp[1] = (byte)(count);
                System.arraycopy(buff, offset, temp, 2, count);
                return sendCmd(getD().WR_DATA, 0, temp.length, temp);
            } else if (cmd == getD().SEND_DATA) {
                if (count == 0) {
                    return false;
                }
                temp = new byte[4 + count];
                temp[0] = 0; //写入位置
                temp[1] = (byte)(count + 2); //数据包长度
                temp[2] = (byte)(getD().SEND_DATA); //命令
                temp[3] = (byte)(count - 1); //命令长度-1
                System.arraycopy(buff, offset, temp, 4, count);
                if (!sendCmd(getD().WR_DATA, 0, temp.length, temp)) {
                    return false;
                }
                return sendCmd(D.DO_BAT_C, 0, 0, null);
            } else {
                return sendCmd(cmd, offset, count, buff);
            }
        } else {
            //写命令到缓冲区
            getShared().buf[getShared().pos++] = (byte)(cmd);
            if (cmd == getD().SEND_DATA) {
                getShared().buf[getShared().pos++] = (byte)(count - 1);
            }
            getShared().startPos = getShared().pos;
            if (count > 0) {
                System.arraycopy(buff, offset, getShared().buf, getShared().pos, count);
            }
            getShared().pos += count;
            return true;
        }
    }

    private boolean doSet(int cmd, int offset, int count, byte... buff) {
        boolean result = doCmd(cmd, offset, count, buff);
        if (result && getShared().isDoNow) {
            result = checkResult(150000);
        }
        return result;
    }

    private int getBuffData(int addr, byte[] buff, int maxLength) {
        byte[] temp = new byte[2];
        temp[0] = (byte)(addr);
        temp[1] = (byte)(buff.length);
        if (!doCmd(D.GET_BUF, 0, 2, temp)) {
            return 0;
        }
        return getCmdData(buff, maxLength);
    }

    private boolean initBox() {
        int i;
        int run = 0;
        byte[] password = new byte[10];
        password[0] = 0x0C;
        password[1] = 0x22;
        password[2] = 0x17;
        password[3] = 0x41;
        password[4] = 0x57;
        password[5] = 0x2D;
        password[6] = 0x43;
        password[7] = 0x17;
        password[8] = 0x2D;
        password[9] = 0x4D;

        Random rand = new Random();
        getShared().isDoNow = true;
        getShared().runFlag = 0;
        byte[] buf = new byte[32];
        for (i = 1; i < 4; i++) {
            buf[i] = (byte)(rand.nextInt());
        }
        for (i = 0; i < 10; i++) {
            run += ((password[i] & 0xFF) ^ (buf[i % 3 + 1] & 0xFF)) & 0xFF;
        }
        if ((run & 0xFF) == 0) {
            run = 0x55;
        }
        if (!doCmd(D.GET_CPU, 1, 3, buf)) {
            return false;
        }
        if (getCmdData(buf, 32) <= 0) {
            return false;
        }
        getShared().runFlag = 0; // Run
        getShared().boxTimeUnit = 0;
        for (i = 0; i < 3; i++) {
            getShared().boxTimeUnit = getShared().boxTimeUnit * 256 + (buf[i] & 0xFF);
        }
        getShared().timeBaseDB = buf[i++] & 0xFF;
        getShared().timeExternDB = buf[i++] & 0xFF;

        for (i = 0; i < D.MAXPORT_NUM; i++) {
            getShared().ports[i] = (byte)(0xFF);
        }
        getShared().pos = 0;
        getShared().isDb20 = false;
        return true;
    }

    private boolean checkBox() {
        byte[] buff = new byte[32];
        if (!doCmd(D.GET_BOXID, 0, 0, null)) {
            return false;
        }
        if (getCmdData(buff, 32) <= 0) {
            return false;
        }
        getShared().boxVer = ((buff[10] & 0xFF) << 8) | (buff[11] & 0xFF);
        return true;
    }

    public boolean setLineLevel(int valueLow, int valueHigh) {
        getShared().ports[1] = (byte)((getShared().ports[1] & 0xFF) & ~valueLow);
        getShared().ports[1] = (byte)((getShared().ports[1] & 0xFF) | valueHigh);
        return doSet(D.SET_PORT1, 1, 1, getShared().ports);
    }

    public boolean setCommCtrl(int valueOpen, int valueClose) {
        getShared().ports[2] = (byte)((getShared().ports[2] & 0xFF) & ~valueOpen);
        getShared().ports[2] = (byte)((getShared().ports[2] & 0xFF) | valueClose);
        return doSet(D.SET_PORT2, 2, 1, getShared().ports);
    }

    public boolean setCommLine(int sendLine, int recvLine) {
        if (sendLine > 7) {
            sendLine = 0x0F;
        }
        if (recvLine > 7) {
            recvLine = 0x0F;
        }
        getShared().ports[0] = (byte)(sendLine | (recvLine << 4));
        return doSet(D.SET_PORT0, 0, 1, getShared().ports);
    }

    public boolean turnOverOneByOne() {
        return doSet(getD().SET_ONEBYONE, 0, 0, null);
    }

    public boolean keepLink(boolean isRunLink) {
        return doSet(isRunLink ? D.RUN_LINK : D.STOP_LINK, 0, 0, null);
    }

    public boolean setCommLink(int ctrlWord1, int ctrlWord2, int ctrlWord3) {
        byte[] ctrlWord = new byte[3];
        int modeControl = ctrlWord1 & 0xE0;
        int length = 3;
        ctrlWord[0] = (byte)(ctrlWord1);
        if ((ctrlWord1 & 0x04) != 0) {
            getShared().isDb20 = true;
        } else {
            getShared().isDb20 = false;
        }
        if (modeControl == D.SET_VPW || modeControl == D.SET_PWM) {
            return doSet(D.SET_CTRL, 0, 1, ctrlWord);
        }
        ctrlWord[1] = (byte)(ctrlWord2);
        ctrlWord[2] = (byte)(ctrlWord3);
        if (ctrlWord3 == 0) {
            length--;
            if (ctrlWord2 == 0) {
                length--;
            }
        }
        if (modeControl == D.EXRS_232 && length < 2) {
            return false;
        }
        return doSet(D.SET_CTRL, 0, length, ctrlWord);
    }

    public boolean setCommBaud(double baud) {
        byte[] baudTime = new byte[2];
        double instructNum = ((1000000.0 / getShared().boxTimeUnit) * 1000000) / baud;
        if (getShared().isDb20) {
            instructNum /= 20;
        }
        instructNum += 0.5;
        if (instructNum > 65535 || instructNum < 10) {
            return false;
        }
        baudTime[0] = (byte)(instructNum / 256);
        baudTime[1] = (byte)(instructNum % 256);
        if (baudTime[0] == 0) {
            return doSet(D.SET_BAUD, 1, 1, baudTime);
        }
        return doSet(D.SET_BAUD, 0, 2, baudTime);
    }

    public void getLinkTime(int type, int time) {
        if (type == D.SETBYTETIME) {
            getShared().reqByteToByte = time;
        } else if (type == D.SETWAITTIME) {
            getShared().reqWaitTime = time;
        } else if (type == D.SETRECBBOUT) {
            getShared().resByteToByte = time;
        } else {
            getShared().resWaitTime = time;
        }
    }

    public boolean setCommTime(int type, int time) {
        byte[] timeBuff = new byte[2];
        getLinkTime(type, time);
        if (type == D.SETVPWSTART || type == D.SETVPWRECS) {
            if (type == D.SETVPWRECS) {
                time = (time * 2) / 3;
            }
            type = type + (D.SETBYTETIME & 0xF0);
            time = new Double(time / (getShared().boxTimeUnit / 1000000.0)).intValue();
        } else {
            time = new Double((time / getShared().timeBaseDB) / (getShared().boxTimeUnit / 1000000.0)).intValue();
        }
        timeBuff[0] = (byte)(time / 256);
        timeBuff[1] = (byte)(time % 256);
        if (timeBuff[0] == 0) {
            return doSet(type, 1, 1, timeBuff);
        }
        return doSet(type, 0, 2, timeBuff);
    }

    public boolean commboxDelay(int time) {
        byte[] timeBuff = new byte[2];
        int delayWord = getD().DELAYSHORT;
        time = new Double(time / (getShared().boxTimeUnit / 1000000.0)).intValue();
        if (time == 0) {
            return false;
        }
        if (time > 65535) {
            time = new Double(time / getShared().timeBaseDB).intValue();
            if (time > 65535) {
                time = new Double((time * getShared().timeBaseDB) / getShared().timeExternDB).intValue();
                if (time > 65535) {
                    return false;
                }
                delayWord = D.DELAYDWORD;
            } else {
                delayWord = getD().DELAYTIME;
            }
        }
        timeBuff[0] = (byte)(time / 256);
        timeBuff[1] = (byte)(time % 256);
        if (timeBuff[0] == 0) {
            return doSet(delayWord, 1, 1, timeBuff);
        }
        return doSet(delayWord, 0, 2, timeBuff);
    }

    public boolean sendOutData(int offset, int count, byte... buffer) {
        return doSet(getD().SEND_DATA, offset, count, buffer);
    }

    public boolean runReceive(int type) {
        if (type == getD().GET_PORT1) {
            getShared().isDb20 = false;
        }
        return doCmd(type, 0, 0, null);
    }

    public boolean stopNow(boolean isStopExecute) {
        int cmd = isStopExecute ? getD().STOP_EXECUTE : getD().STOP_REC;
        for (int i = 0; i < 3; i++) {
            try {
                getPort().write(new byte[]{(byte)(cmd)}, 0, 1);
                if (checkSend()) {
                    if (isStopExecute && !checkResult(200000)) {
                        continue;
                    }
                    return true;
                }
            } catch (IOException e) {
                continue;
            }
        }
        return false;
    }

    public boolean setRF(int cmd, int data) {
        return false;
    }

    private boolean openBox(SerialPort port) {
        try {
            port.setBaudrate(115200);
            port.setStopbits(SerialPort.Stopbits.Two);
            port.open();
            port.setDtr(true);
            Thread.sleep(50);
            if (initBox() && checkBox()) {
                port.discardInBuffer();
                port.discardOutBuffer();
                return true;
            }
            port.close();
            return false;
        } catch (InterruptedException | IOException ex) {
            return false;
        }
    }

    public boolean openComm() {
        getShared().lastError = getD().DISCONNECT_COMM;
        if (getPort().getClass() == SerialPort.class) {
            SerialPort port = (SerialPort) getPort();
            if (!openBox(port)) {
                String[] portNames = SerialPort.getSystemPorts();
                for (String name : portNames) {
                    try {
                        port.setPortName(name);
                        if (openBox(port)) {
                            return true;
                        }
                    } catch (IOException ex) {
                    }
                }
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean closeComm() {
        reset();
        setRF(D.RF_RESET, 0);
        if (getPort().getClass() == SerialPort.class) {
            try {
                SerialPort port = (SerialPort) getPort();
                port.setDtr(false);
                port.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public boolean setPCBaud(int baud) {
        int i = 3;
        while ((i--) > 0) {
            doCmd(getD().SET_UPBAUD, 0, 1, new byte[]{(byte)(baud)}); //该命令BOX返回因PC端的波特率未改变而无法接收
            try {
                SerialPort port = (SerialPort) getPort();
                port.discardInBuffer();
                port.discardOutBuffer();
                setRF(D.RF_SET_BAUD, baud); //该命令BOX返回因PC端的波特率未改变而无法接收
                if (baud == D.UP_115200BPS) {
                    port.setBaudrate(115200);
                } else if (baud == D.UP_19200BPS) {
                    port.setBaudrate(19200);
                } else if (baud == D.UP_38400BPS) {
                    port.setBaudrate(38400);
                } else if (baud == D.UP_57600BPS) {
                    port.setBaudrate(57600);
                } else {
                    port.setBaudrate(9600);
                }
                setRF(D.RF_SET_BAUD, baud); //该命令BOX返回因PC端的波特率已改变而应该接收到
                port.discardInBuffer();
                port.discardOutBuffer();
                if (doCmd(getD().SET_UPBAUD, 0, 1, new byte[]{(byte)(baud)})) {
                    return true;
                }
            } catch (IOException ex) {
                return false;
            }
        }
        return false;
    }

    public int getAbsAdd(int buffID, int add) {
        return 0;
    }

    // buffer[0] = addr, buffer[1] = data
    public boolean updateBuff(int type, byte[] buffer) {
        int len = 0;
        byte[] buf = new byte[3];
        buf[0] = buffer[0];
        buf[1] = buffer[1];
        if ((type == D.INC_BYTE) || (type == D.DEC_BYTE) || (type == D.INVERTBYTE)) {
            len = 1;
        } else if ((type == D.UPDATE_BYTE) || (type == D.ADD_BYTE) || (type == getD().SUB_BYTE)) {
            len = 2;
        } else {
            len = 3;
        }
        return doSet(type, 0, len, buf);
    }

    public boolean copyBuff(int dest, int src, int len) {
        byte[] buf = new byte[3];
        buf[0] = (byte)(dest);
        buf[1] = (byte)(src);
        buf[2] = (byte)(len);
        return doSet(D.COPY_BYTE, 0, 3, buf);
    }

    public boolean newBatch(int buffID) {
        getShared().pos = 0;
        getShared().isLink = (buffID == D.LINKBLOCK ? true : false);
        getShared().isDoNow = false;
        return true;
    }

    public boolean endBatch() {
        int i = 0;
        getShared().isDoNow = true;
        getShared().buf[getShared().pos++] = 0; //命令块以0x00标记结束
        if (getShared().isLink) { //修改UpdateBuff使用到的地址
            while (getShared().buf[i] != 0) {
                int mode = getShared().buf[i] & 0xFC;
                if (mode == D.COPY_BYTE) {
                    getShared().buf[i + 3] = (byte)((getShared().buf[i + 3] & 0xFF) + D.MAXBUFF_LEN - getShared().pos);
                } else if (mode == getD().SUB_BYTE) {
                    getShared().buf[i + 2] = (byte)((getShared().buf[i + 2] & 0xFF) + D.MAXBUFF_LEN - getShared().pos);
                } else {
                    getShared().buf[i + 1] = (byte)((getShared().buf[i + 1] & 0xFF) + D.MAXBUFF_LEN - getShared().pos);

                }
                if ((getShared().buf[i] & 0xFF) == getD().SEND_DATA) {
                    i += (1 + ((getShared().buf[i + 1] & 0xFF) + 1) + 1);
                } else if ((getShared().buf[i] & 0xFF) >= D.REC_LEN_1 && (getShared().buf[i] & 0xFF) <= D.REC_LEN_15) {
                    i++; //特殊
                } else {
                    i = i + ((getShared().buf[i] & 0x03) + 1);
                }
            }
        }
        return doCmd(getD().WR_DATA, 0, getShared().pos, getShared().buf);
    }

    public boolean delBatch(int buffID) {
        getShared().isDoNow = true;
        getShared().pos = 0;
        return true;
    }

    public boolean runBatch(boolean isExecuteMany, int... buffID) {
        int cmd;
        if ((buffID[0] & 0xFF) == D.LINKBLOCK) {
            cmd = isExecuteMany ? D.DO_BAT_LN : D.DO_BAT_L;
        } else {
            cmd = isExecuteMany ? D.DO_BAT_CN : D.DO_BAT_C;
        }
        return doCmd(cmd, 0, 0, null);
    }

    public boolean reset() {
        try {
            stopNow(true);
            getPort().discardInBuffer();
            getPort().discardOutBuffer();
            for (int i = 0; i < D.MAXPORT_NUM; i++) {
                getShared().ports[i] = (byte)(0xFF);
            }
            return doCmd(getD().RESET, 0, 0, null);
        } catch (IOException ex) {
            return false;
        }
    }

    public int getBoxVer() {
        return getShared().boxVer;
    }

    public boolean testConnectorType(int identifyCode) {
        return false;
    }
}
