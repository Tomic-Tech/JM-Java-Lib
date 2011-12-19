/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.w80;

import java.io.IOException;
import java.util.Random;
import jm.io.IPort;
import jm.io.SerialPort;

/**
 *
 * @author Ogilvy
 */
class Box {

    private Shared _shared;
    private IPort _port;

    public Box(IPort port, Shared shared) {
        _shared = shared;
        _port = port;
    }

    private boolean checkIdle() {
        try {
            int rb = D.READY;
            int avail = _port.bytesAvailable();
            if (avail > 20) {
                _port.discardInBuffer();
                return true;
            }
            while (avail > 0) {
                _port.readByte();
                avail--;
            }
            if (rb == D.READY || rb == D.ERROR) {
                return true;
            }
            rb = _port.readByte(200);
            if (rb == D.READY || rb == D.ERROR) {
                return true;
            }
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean checkSend() {
        try {
            int rb = 0;
            rb = _port.readByte(200);
            if (rb == D.RECV_OK) {
                return true;
            }
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean checkResult(int time) {
        try {
            int rb = _port.readByte(time / 1000);
            if (rb == D.READY || rb == D.ERROR) {
                _port.discardInBuffer();
                return true;
            }
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean sendCmd(int cmd, int offset, int count, byte... data) {
        int cs = cmd;
        cmd += _shared.runFlag;
        for (int i = 0; i < count; i++) {
            cs += data[i] & 0xFF;
        }

        byte[] command = new byte[2 + count];
        command[0] = new Integer(cmd).byteValue();
        System.arraycopy(data, offset, command, 1, count);
        command[command.length - 1] = new Integer(cs).byteValue();

        for (int i = 0; i < 3; i++) {
            if (!checkIdle()) {
                continue;
            }
            try {
                _port.write(command, 0, command.length);
            } catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }
            if (checkSend()) {
                return true;
            }
        }
        return false;
    }

    private int readData(byte[] buff, int offset, int count, int microSeconds) {
        try {
            return _port.read(buff, offset, count, microSeconds / 1000);
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                int avail = _port.bytesAvailable();
                if (avail > 0) {
                    if (avail <= count) {
                        return _port.read(buff, offset, avail);
                    } else {
                        return _port.read(buff, offset, count);
                    }
                }
                return avail;
            } catch (IOException ex1) {
                ex1.printStackTrace();
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
            len[0] = new Integer(maxLength).byteValue();
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
        if (cmd != D.WR_DATA && cmd != D.SEND_DATA) {
            cmd |= count; //加上长度位
        }
        if (_shared.isDoNow) {
            //发送到BOX执行
            switch (cmd) {
                case D.WR_DATA:
                    temp = new byte[2 + count];
                    if (count == 0) {
                        return false;
                    }
                    if (_shared.isLink) {
                        temp[0] = new Integer(0xFF).byteValue(); //写链路保持
                    } else {
                        temp[0] = 0; //写通讯命令
                    }
                    temp[1] = new Integer(count).byteValue();
                    System.arraycopy(buff, offset, temp, 2, count);
                    return sendCmd(D.WR_DATA, 0, temp.length, temp);
                case D.SEND_DATA:
                    if (count == 0) {
                        return false;
                    }
                    temp = new byte[4 + count];
                    temp[0] = 0; //写入位置
                    temp[1] = new Integer(count + 2).byteValue(); //数据包长度
                    temp[2] = new Integer(D.SEND_DATA).byteValue(); //命令
                    temp[3] = new Integer(count - 1).byteValue(); //命令长度-1
                    System.arraycopy(buff, offset, temp, 4, count);
                    if (!sendCmd(D.WR_DATA, 0, temp.length, temp)) {
                        return false;
                    }
                    return sendCmd(D.DO_BAT_C, 0, 0, null);
                default:
                    return sendCmd(cmd, 0, 0, buff);
            }
        } else {
            //写命令到缓冲区
            _shared.buf[_shared.pos++] = new Integer(cmd).byteValue();
            if (cmd == D.SEND_DATA) {
                _shared.buf[_shared.pos++] = new Integer(count - 1).byteValue();
            }
            _shared.startPos = _shared.pos;
            System.arraycopy(_shared.buf, _shared.pos, buff, offset, count);
            _shared.pos += count;
            return true;
        }
    }

    private boolean doSet(int cmd, int offset, int count, byte... buff) {
        boolean result = doCmd(cmd, offset, count, buff);
        if (result && _shared.isDoNow) {
            result = checkResult(150000);
        }
        return result;
    }

    private int getBuffData(int addr, byte[] buff, int maxLength) {
        byte[] temp = new byte[2];
        temp[0] = new Integer(addr).byteValue();
        temp[1] = new Integer(buff.length).byteValue();
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
        _shared.isDoNow = true;
        _shared.runFlag = 0;
        byte[] buf = new byte[32];
        for (i = 0; i < 4; i++) {
            buf[i] = new Integer(rand.nextInt()).byteValue();
        }
        for (i = 0; i < 10; i++) {
            run += ((password[i] & 0xFF) ^ ((buf[i % 3] & 0xFF) + 1));
        }
        if (run == 0) {
            run = 0x55;
        }
        if (!doCmd(D.GET_CPU, 1, 3, buf)) {
            return false;
        }
        if (getCmdData(buf, 32) <= 0) {
            return false;
        }
        _shared.runFlag = 0; // Run
        _shared.boxTimeUnit = 0;
        for (i = 0; i < 3; i++) {
            _shared.boxTimeUnit = _shared.boxTimeUnit * 256 + (buf[i] & 0xFF);
        }
        _shared.timeBaseDB = buf[i++] & 0xFF;
        _shared.timeExternDB = buf[i++] & 0xFF;

        for (i = 0; i < D.MAXPORT_NUM; i++) {
            _shared.ports[i] = new Integer(0xFF).byteValue();
        }
        _shared.pos = 0;
        _shared.isDB20 = false;
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
        _shared.boxVer = ((buff[10] & 0xFF) << 8) | (buff[11] & 0xFF);
        return true;
    }

    public boolean setLineLevel(int valueLow, int valueHigh) {
        _shared.ports[1] = new Integer((_shared.ports[1] & 0xFF) & ~valueLow).byteValue();
        _shared.ports[1] = new Integer((_shared.ports[1] & 0xFF) | valueHigh).byteValue();
        return doSet(D.SET_PORT1, 1, 1, _shared.ports);
    }

    public boolean setCommCtrl(int valueOpen, int valueClose) {
        _shared.ports[2] = new Integer((_shared.ports[2] & 0xFF) & ~valueOpen).byteValue();
        _shared.ports[2] = new Integer((_shared.ports[2] & 0xFF) | valueClose).byteValue();
        return doSet(D.SET_PORT2, 2, 1, _shared.ports);
    }

    public boolean setCommLine(int sendLine, int recvLine) {
        if (sendLine > 7) {
            sendLine = 0x0F;
        }
        if (recvLine > 7) {
            recvLine = 0x0F;
        }
        _shared.ports[0] = new Integer(sendLine | (recvLine << 4)).byteValue();
        return doSet(D.SET_PORT0, 0, 1, _shared.ports);
    }

    public boolean turnOverOneByOne() {
        return doSet(D.SET_ONEBYONE, 0, 0, null);
    }

    public boolean keepLink(boolean isRunLink) {
        return doSet(isRunLink ? D.RUN_LINK : D.STOP_LINK, 0, 0, null);
    }

    public boolean setCommLink(int ctrlWord1, int ctrlWord2, int ctrlWord3) {
        byte[] ctrlWord = new byte[3];
        int modeControl = ctrlWord1 & 0xE0;
        int length = 3;
        ctrlWord[0] = new Integer(ctrlWord1).byteValue();
        if ((ctrlWord1 & 0x04) != 0) {
            _shared.isDB20 = true;
        } else {
            _shared.isDB20 = false;
        }
        if (modeControl == D.SET_VPW || modeControl == D.SET_PWM) {
            return doSet(D.SET_CTRL, 0, 1, ctrlWord);
        }
        ctrlWord[1] = new Integer(ctrlWord2).byteValue();
        ctrlWord[2] = new Integer(ctrlWord3).byteValue();
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

    public boolean setCommBaud(int baud) {
        byte[] baudTime = new byte[2];
        double instructNum = ((1000000.0 / _shared.boxTimeUnit) * 1000000) / baud;
        if (_shared.isDB20) {
            instructNum /= 20;
        }
        instructNum += 0.5;
        if (instructNum > 65535 || instructNum < 10) {
            return false;
        }
        baudTime[0] = new Double(instructNum / 256).byteValue();
        baudTime[1] = new Double(instructNum % 256).byteValue();
        if (baudTime[0] == 0) {
            return doSet(D.SET_BAUD, 1, 1, baudTime);
        }
        return doSet(D.SET_BAUD, 0, 2, baudTime);
    }

    public void getLinkTime(int type, int time) {
        switch (type) {
            case D.SETBYTETIME:
                _shared.reqByteToByte = time;
                break;
            case D.SETWAITTIME:
                _shared.reqWaitTime = time;
                break;
            case D.SETRECBBOUT:
                _shared.resByteToByte = time;
                break;
            case D.SETRECFROUT:
                _shared.resWaitTime = time;
                break;
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
            time = new Double(time / (_shared.boxTimeUnit / 1000000.0)).intValue();
        } else {
            time = new Double((time / _shared.timeBaseDB) / (_shared.boxTimeUnit / 1000000.0)).intValue();
        }
        timeBuff[0] = new Integer(time / 256).byteValue();
        timeBuff[1] = new Integer(time % 256).byteValue();
        if (timeBuff[0] == 0) {
            return doSet(type, 1, 1, timeBuff);
        }
        return doSet(type, 0, 2, timeBuff);
    }

    public boolean commboxDelay(int time) {
        byte[] timeBuff = new byte[2];
        int delayWord = D.DELAYSHORT;
        time = new Double(time / (_shared.boxTimeUnit / 1000000.0)).intValue();
        if (time == 0) {
            return false;
        }
        if (time > 65535) {
            time = new Double(time / _shared.timeBaseDB).intValue();
            if (time > 65535) {
                time = new Double((time * _shared.timeBaseDB) / _shared.timeExternDB).intValue();
                if (time > 65535) {
                    return false;
                }
                delayWord = D.DELAYDWORD;
            } else {
                delayWord = D.DELAYTIME;
            }
        }
        timeBuff[0] = new Integer(time / 256).byteValue();
        timeBuff[1] = new Integer(time % 256).byteValue();
        if (timeBuff[0] == 0) {
            return doSet(delayWord, 1, 1, timeBuff);
        }
        return doSet(delayWord, 0, 2, timeBuff);
    }

    public boolean sendOutData(int offset, int count, byte... buffer) {
        return doSet(D.SEND_DATA, offset, count, buffer);
    }

    public boolean runReceive(int type) {
        if (type == D.GET_PORT1) {
            _shared.isDB20 = false;
        }
        return doCmd(type, 0, 0, null);
    }

    public boolean stopNow(boolean isStopExecute) {
        int cmd = isStopExecute ? D.STOP_EXECUTE : D.STOP_REC;
        for (int i = 0; i < 3; i++) {
            try {
                _port.write(new byte[]{new Integer(cmd).byteValue()}, 0, 1);
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
        _shared.lastError = D.DISCONNECT_COMM;
        if (_port.getClass() == SerialPort.class) {
            SerialPort port = (SerialPort) _port;
            if (!openBox(port)) {
                String[] portNames = SerialPort.getSystemPorts();
                for (String name : portNames) {
                    try {
                        port.setPortName(name);
                        if (openBox(port)) {
                            return true;
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
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
        if (_port.getClass() == SerialPort.class) {
            try {
                ((SerialPort) _port).close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public boolean setPCBaud(int baud) {
        int i = 3;
        while ((i--) > 0) {
            doCmd(D.SET_UPBAUD, 0, 1, new byte[]{new Integer(baud).byteValue()}); //该命令BOX返回因PC端的波特率未改变而无法接收
            try {
                SerialPort port = (SerialPort) _port;
                port.discardInBuffer();
                port.discardOutBuffer();
                setRF(D.RF_SET_BAUD, baud); //该命令BOX返回因PC端的波特率未改变而无法接收
                switch (baud) {
                    case D.UP_115200BPS:
                        port.setBaudrate(115200);
                        break;
                    case D.UP_19200BPS:
                        port.setBaudrate(19200);
                        break;
                    case D.UP_38400BPS:
                        port.setBaudrate(38400);
                        break;
                    case D.UP_57600BPS:
                        port.setBaudrate(57600);
                        break;
                    case D.UP_9600BPS:
                        port.setBaudrate(9600);
                        break;
                }
                setRF(D.RF_SET_BAUD, baud); //该命令BOX返回因PC端的波特率已改变而应该接收到
                port.discardInBuffer();
                port.discardOutBuffer();
                if (doCmd(D.SET_UPBAUD, 0, 1, new byte[]{new Integer(baud).byteValue()})) {
                    return true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean updateBuff(int type, int addr, int data) {
        int len = 0;
        byte[] buf = new byte[3];
        buf[0] = new Integer(addr).byteValue();
        buf[1] = new Integer(data).byteValue();
        switch (type) {
            case D.INC_BYTE:
            case D.DEC_BYTE:
            case D.INVERTBYTE:
                len = 1;
                break;
            case D.UPDATE_BYTE:
            case D.ADD_BYTE:
            case D.SUB_BYTE:
                len = 2;
                break;
            case D.COPY_BYTE:
                len = 3;
                break;
        }
        return doSet(type, 0, len, buf);
    }

    public boolean copyBuff(int dest, int src, int len) {
        byte[] buf = new byte[3];
        buf[0] = new Integer(dest).byteValue();
        buf[1] = new Integer(src).byteValue();
        buf[2] = new Integer(len).byteValue();
        return doSet(D.COPY_BYTE, 0, 3, buf);
    }

    public boolean newBatch(int buffID) {
        _shared.pos = 0;
        _shared.isLink = (buffID == D.LINKBLOCK ? true : false);
        _shared.isDoNow = false;
        return true;
    }

    public boolean endBatch() {
        int i = 0;
        _shared.isDoNow = true;
        _shared.buf[_shared.pos++] = 0; //命令块以0x00标记结束
        if (_shared.isLink) { //修改UpdateBuff使用到的地址
            while (_shared.buf[i] != 0) {
                switch (_shared.buf[i] & 0xFC) {
                    case D.COPY_BYTE:
                        _shared.buf[i + 3] = new Integer((_shared.buf[i + 3] & 0xFF) + D.MAXBUFF_LEN - _shared.pos).byteValue();
                    case D.SUB_BYTE:
                        _shared.buf[i + 2] = new Integer((_shared.buf[i + 2] & 0xFF) + D.MAXBUFF_LEN - _shared.pos).byteValue();
                    case D.UPDATE_BYTE:
                    case D.INVERT_BYTE:
                    case D.ADD_BYTE:
                    case D.DEC_BYTE:
                    case D.INC_BYTE:
                        _shared.buf[i + 1] = new Integer((_shared.buf[i + 1] & 0xFF) + D.MAXBUFF_LEN - _shared.pos).byteValue();
                        break;
                }
                if ((_shared.buf[i] & 0xFF) == D.SEND_DATA) {
                    i += (1 + ((_shared.buf[i + 1] & 0xFF) + 1) + 1);
                } else if ((_shared.buf[i] & 0xFF) >= D.REC_LEN_1 && (_shared.buf[i] & 0xFF) <= D.REC_LEN_15) {
                    i++; //特殊
                } else {
                    i = i + ((_shared.buf[i] & 0x03) + 1);
                }
            }
        }
        return doCmd(D.WR_DATA, 0, _shared.pos, _shared.buf);
    }

    public boolean delBatch(int buffID) {
        _shared.isDoNow = true;
        _shared.pos = 0;
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
            _port.discardInBuffer();
            _port.discardOutBuffer();
            for (int i = 0; i < D.MAXPORT_NUM; i++) {
                _shared.ports[i] = new Integer(0xFF).byteValue();
            }
            return doCmd(D.RESET, 0, 0, null);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int getBoxVer() {
        return _shared.boxVer;
    }

    public boolean testConnectorType(int identifyCode) {
        return false;
    }
}
