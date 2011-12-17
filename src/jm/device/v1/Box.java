package jm.device.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import jm.io.IPort;
import jm.io.SerialPort;

final class Box {

    private IPort _port;
    private Shared _shared;

    public Box(IPort port, Shared shared) {
        _port = port;
        _shared = shared;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // 静态函数：bool CheckIdle()
    //
    // 返回值： bool : 执行成功返回1,否则返回0
    // 参数： 无
    // 功能：
    // 检查当前Commbox是否在作链路保持工作。
    // a.读取接受缓存中最后一个数据。
    // b.是否为成功标志？成功则退出
    // c.否则在2/3倍的链路保持时间中，查找成功标志？成功则退出
    // d.否则失败。
    // 失败原因:
    // a.链路保持通讯失败，需发停止命令，重新尝试,失败需复位
    // b.数据接受缓存在接受时未检查结束
    // ///////////////////////////////////////////////////////////////////////////
    private boolean checkIdle() {
        try {
            int receiveBuffer = D.SUCCESS;
            long avail = _port.bytesAvailable();
            if (avail > 240) {
                _port.discardInBuffer();
                _port.discardOutBuffer();
                return true;
            }
            while (avail > 0) {
                receiveBuffer = _port.readByte();
                avail--;
            }
            if (receiveBuffer == D.SUCCESS) {
                return true;
            }
            receiveBuffer = _port.readByte(200);
            if (receiveBuffer == D.SUCCESS) {
                return true;
            }
        } catch (IOException e) {
            _shared.lastError = D.KEEPLINK_ERROR;
            return false;
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // 全局函数：SendOk()
    //
    // 返回值： bool : 执行成功接纳1,否则接纳0
    // 功能：
    // 检查并发送出去的命令是否被接纳。
    // ///////////////////////////////////////////////////////////////////////////
    protected boolean sendOk(long millicSeconds) {
        try {
            int receiveBuffer = 0;
            receiveBuffer = _port.readByte(millicSeconds);
            if (receiveBuffer == D.SEND_OK) {
                return true;
            }
            _shared.lastError = D.SENDDATA_ERROR;
        } catch (IOException e) {
            _shared.lastError = D.TIMEOUT_ERROR;
        }
        return false;
    }

    protected int getBoxVer() {
        return ((_shared.info.version[0] & 0xFF) << 8)
                | (_shared.info.version[1] & 0xFF);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // 全局函数：bool CommboxDo(BYTE commandword,BYTE length,BYTE * buff)
    //
    // 返回值： bool : 执行成功返回1,否则返回0
    // 参数： 三个BYTE commandword,BYTE length,BYTE * buff
    // BYTE commandword: 要commbox做事的命令字
    // BYTE length : 命令参数信息
    // BYTE * buff : 参数缓存
    // 功能：
    // 检查Commbox是否空闲，根据参数组合，并发送出去。
    // a.检查Commbox是否空闲，否，则返回0。
    // b.发送命令，成功则退出
    // 失败原因:
    // a.检查链路保持通讯失败。
    // b.计算机通讯串口有错误，需提示操作。
    // c.不符合命令格式和定义。
    // d.在发送转发命令时错误。
    // ///////////////////////////////////////////////////////////////////////////
    protected boolean commboxDo(int commandWord, byte[] buff) {
        if (buff.length > D.CMD_DATALEN) {
            return sendDataToECU(commandWord, buff);
        } else {
            return commboxCommand(commandWord, buff);
        }
    }

    private boolean sendDataToECU(int commandWord, byte[] buff) {
        if (commandWord == D.SEND_DATA && buff.length <= D.SEND_LEN) {
            if (_shared.buff.size() < (buff.length + 1)) {
                _shared.lastError = D.NOBUFF_TOSEND;
                return false;
            }
            if (getBoxVer() > 0x400) {
                // 增加发送长命令
                if (!sendDataToECUNew(commandWord, buff)) {
                    return false;
                }
            } else {
                // 保持与旧盒子兼容
                if (!sendDataToECUOld(commandWord, buff)) {
                    return false;
                }
            }
            buff = new byte[1];
            buff[0] = _shared.buff.add[D.SWAPBLOCK];
            return commboxDo(D.D0_BAT, buff);
        }
        _shared.lastError = D.ILLIGICAL_LEN;
        return false;
    }

    private boolean sendDataToECUNew(int commandWord, byte[] buff) {
        int i;
        byte[] command = new byte[buff.length + 6];
        command[0] = new Integer(D.WR_DATA
                + _shared.info.headPassword).byteValue();
        command[1] = new Integer(buff.length + 3).byteValue();
        command[2] = _shared.buff.add[D.SWAPBLOCK];
        command[3] = new Integer(D.SEND_CMD).byteValue();
        command[4] = new Integer(buff.length - 1).byteValue();
        int checksum = D.WR_DATA + command[1] & 0xFF + command[2] & 0xFF
                + command[3] & 0xFF + command[4] & 0xFF;

        for (i = 0; i < buff.length; i++) {
            command[i + 5] = buff[i];
            checksum += buff[i] & 0xFF;
        }
        command[i + 5] = new Integer(checksum).byteValue();
        for (i = 0; i < 3; i++) {
            try {
                if (!checkIdle() || (_port.write(command) != command.length)) {
                    _shared.lastError = D.SENDDATA_ERROR;
                    continue;
                }
            } catch (IOException e) {
                _shared.lastError = D.SENDDATA_ERROR;
                continue;
            }
            if (sendOk(20 * (command.length + 7))) {
                return true;
            }
        }
        return false;
    }

    private boolean sendDataToECUOld(int commandWord, byte[] buff) {
        byte[] command = new byte[buff.length + 5];
        command[0] = new Integer(D.WR_DATA
                + _shared.info.headPassword).byteValue();
        command[1] = new Integer(buff.length + 2).byteValue();
        command[2] = _shared.buff.add[D.SWAPBLOCK];
        command[3] = new Integer(buff.length - 1).byteValue();
        int checksum = D.WR_DATA + command[1] & 0xFF + command[2] & 0xFF
                + command[3] & 0xFF;
        int i;
        for (i = 0; i < buff.length; i++) {
            command[i + 4] = buff[i];
            checksum += buff[i] & 0xFF;
        }
        command[i + 4] = new Integer(checksum).byteValue();
        for (i = 0; i < 3; i++) {
            try {
                if (!checkIdle() || (_port.write(command) != command.length)) {
                    _shared.lastError = D.SENDDATA_ERROR;
                    continue;
                }
            } catch (IOException e) {
                _shared.lastError = D.SENDDATA_ERROR;
                continue;
            }
            if (sendOk(20 * (command.length + 6))) {
                return true;
            }
        }
        return false;
    }

    private boolean commboxCommand(int commandWord, byte[] buff) {
        if (commandWord < D.WR_DATA) {
            if ((buff == null) || (buff.length == 0)) {
                _shared.lastError = D.ILLIGICAL_LEN;
                return false;
            }
        } else {
            if ((buff != null) || (buff.length != 0)) {
                _shared.lastError = D.ILLIGICAL_LEN;
                return false;
            }
        }
        byte[] command = null;

        if ((buff == null) || (buff.length == 0)) {
            command = new byte[2];
            command[0] = new Integer(commandWord
                    + _shared.info.headPassword).byteValue();
            command[1] = new Integer(commandWord).byteValue();
        } else {
            int i;
            int checksum = commandWord + buff.length;
            command = new byte[2 + buff.length];
            command[0] = new Integer(checksum
                    + _shared.info.headPassword).byteValue();
            for (i = 1; i <= buff.length; i++) {
                command[i] = buff[i - 1];
                checksum += buff[i - 1] & 0xFF;
            }
            command[i] = new Integer(checksum).byteValue();
        }
        for (int i = 0; i < 3; i++) {
            if (commandWord != D.STOP_REC && commandWord != D.STOP_EXECUTE) {
                try {
                    if (!checkIdle()
                            || (_port.write(command) != command.length)) {
                        _shared.lastError = D.SENDDATA_ERROR;
                        continue;
                    }
                } catch (IOException e) {
                    _shared.lastError = D.SENDDATA_ERROR;
                    continue;
                }
            } else {
                try {
                    _port.write(command);
                } catch (IOException e) {
                    _shared.lastError = D.SENDDATA_ERROR;
                    continue;
                }
            }
            if (sendOk(20 * command.length)) {
                return true;
            }
        }
        return false;
    }

    // /////////////////////////////////////////////////////////////////
    // 全局函数 bool CheckResult(LONG m_Time)
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : m_Time 超时错误时间
    // 功能:
    // 1.在指定的时间内读取成功标志。
    // 2.返回成功结果。
    // /////////////////////////////////////////////////////////////////
    protected boolean checkResult(long microSeconds) {
        try {
            int receiveBuffer = _port.readByte(microSeconds / 1000);
            if (receiveBuffer == D.SUCCESS) {
                return true;
            }
            while (_port.bytesAvailable() > 0) {
                receiveBuffer = _port.readByte();
            }
            _shared.lastError = receiveBuffer;
            return false;
        } catch (IOException e) {
            _shared.lastError = D.TIMEOUT_ERROR;
            return false;
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // bool StopNow(bool IsStopExcute)
    //
    // 返回值: bool ：成功返回1，失败返回0。
    // 参数: 一个：
    // bool IsStopExcute :
    // 功能: 当IsStopExcute为真，停止Commbox当前执行的操作，
    // 否则，停止当前接受数据，
    // ////////////////////////////////////////////////////////////////////
    protected boolean stopNow(boolean isStopExecute) {
        if (isStopExecute) {
            int receiveBuffer = 0;
            int times = D.REPLAYTIMES;
            while (times-- > 0) {
                try {
                    if (!commboxDo(D.STOP_EXECUTE, null)) {
                        return false;
                    } else {
                        receiveBuffer = _port.readByte(600);
                        if (receiveBuffer == D.RUN_ERR) {
                            return true;
                        }
                        _shared.lastError = D.TIMEOUT_ERROR;
                    }
                } catch (IOException e) {
                    _shared.lastError = D.TIMEOUT_ERROR;
                }
            }
            return false;
        } else {
            return commboxDo(D.STOP_REC, null);
        }
    }

    private boolean doSet(int commandWord, byte[] buff) {
        int times = D.REPLAYTIMES;
        while (times-- > 0) {
            if (times == 0) {
                return false;
            }
            if (!commboxDo(commandWord, buff)) {
                continue;
            } else if (checkResult(50000)) {
                return true;
            }
            stopNow(true);
        }
        return false;
    }

    // /////////////////////////////////////////////////////////////////
    // 全局函数 :
    // WORD ReadData(WORD length,BYTE * recivebuffer, LONG m_Time,INT RecBBTime)
    //
    // 返回: 返回得到的数据长度,或超时错误00
    // 参数 :
    // INT length 指定接收数据长度,当指定的长度为0时，以时间间隔读取数据。
    // recivebuffer 接收数据缓冲区
    // m_Time 超时错误时间
    // RecBBTime 读取数据的字节时间间隔大于此时间为结束。
    // 注意CheckResult的处理，适合与接受命令在批处理最后位置。
    // 功能:
    // 1.在指定的时间内读取数据
    // 2.在指定时间内读完数据则退出
    // 3.超出指定时间则退出
    // /////////////////////////////////////////////////////////////////
    public byte[] readData(long count, long microSeconds) {
        try {
            return _port.read(count, microSeconds / 1000);
        } catch (IOException e) {
            return null;
        }
    }

    // /////////////////////////////////////////////////////////////////
    // 全局函数 GetCMDData(BYTE command,BYTE * recivebuffer)
    //
    // 返回值: BYTE : 执行成功返回1,否则返回0
    // 参数 : BYTE command 执行命令的类型
    // recivebuffer 接收数据缓冲区
    // 功能:
    // 1.在一定的时间内读取指定命令的数据
    // 2.检查校验和
    // 3.如在指定时间内未能读完数据则错误退出
    // 4.未能读到指定命令的数据错误退出
    // /////////////////////////////////////////////////////////////////
    private byte[] getCmdData(int command) {
        int checksum = command;
        byte[] temp = readData(2, 300);
        if (temp == null) {
            return null;
        }
        if ((temp[0] & 0xFF) != command) {
            _shared.lastError = temp[0] & 0xFF;
            try {
                _port.read(_port.bytesAvailable());
            } catch (IOException e) {
            }
            return null;
        }
        int length = temp[1] & 0xFF;
        byte[] result = readData(length, 150 * length);
        if (result == null) {
            return null;
        }
        temp = readData(1, 150);
        if (temp == null) {
            return null;
        }

        checksum += length;
        for (int i = 0; i < length; i++) {
            checksum += result[i] & 0xFF;
        }
        if (checksum != (temp[0] & 0xFF)) {
            _shared.lastError = D.CHECKSUM_ERROR;
            return null;
        }
        return result;
    }

    // ////////////////////////////////////////////////////////////////////
    // 通讯串口和Commbox初始化设定
    // 并进行握手通信，以能正常通讯
    // 关闭串口复位Commbox.
    // ////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////
    // 静态函数 :
    // bool CheckBox()
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : 无
    // 功能:
    // 1.产生随机数据，校验CommBox合法性
    // 2.合法正常退出，否则异常错误。
    // /////////////////////////////////////////////////////////////////
    private boolean checkBox() {
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
        byte[] temp = new byte[5];
        temp[4] = 0x00;
        int i = 0;
        while (i < 4) {
            temp[i] = new Integer(rand.nextInt() % 256).byteValue();
            temp[4] = new Integer(temp[4] & 0xFF + temp[i] & 0xFF).byteValue();
            i++;
        }
        try {
            _port.write(temp);
        } catch (IOException e) {
            _shared.lastError = D.SENDDATA_ERROR;
            return false;
        }
        int checksum = temp[4] & 0xFF + temp[4] & 0xFF;
        i = 0;
        while (i < temp.length) {
            checksum += (password[0] & 0xFF) ^ (temp[i % 5] & 0xFF);
            i++;
        }
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
        }
        temp = getCmdData(D.GETINFO);
        if (temp == null) {
            return false;
        }
        _shared.info.headPassword = temp[0] & 0xFF;

        if (checksum != _shared.info.headPassword) {
            _shared.lastError = D.CHECKSUM_ERROR;
            return false;
        }
        if (_shared.info.headPassword == 0) {
            _shared.info.headPassword = 0x55;
        }
        return true;
    }

    // ///////////////////////////////////////////////////////////////////
    // 静态函数 :
    // bool InitBox()
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : 无
    // 功能:
    // 1.读取Commbox相关信息，并计算赋值
    // 2.对通讯参数初始化
    // 3.调用CheckBox()检查。
    // /////////////////////////////////////////////////////////////////
    private boolean initBox() {
        _shared.isDb20 = false;

        if (!commboxDo(D.GETINFO, null)) {
            return false;
        }

        byte[] temp = getCmdData(D.GETINFO);
        if (temp == null) {
            return false;
        }
        if (temp.length < D.COMMBOXINFOLEN) {
            _shared.lastError = D.LOST_VERSIONDATA;
            return false;
        }
        _shared.info.timeUnit = 0;
        int pos = 0;
        for (int i = 0; i < D.MINITIMELEN; i++) {
            _shared.info.timeUnit = _shared.info.timeUnit
                    * 256 + (temp[pos++] & 0xFF);
        }
        _shared.info.timeBaseDB = temp[pos++] & 0xFF;
        _shared.info.timeExternDB = temp[pos++] & 0xFF;
        _shared.info.cmdBuffLen = temp[pos++] & 0xFF;
        if (_shared.info.timeBaseDB == 0
                || _shared.info.timeUnit == 0
                || _shared.info.cmdBuffLen == 0) {
            _shared.lastError = D.COMMTIME_ZERO;
            return false;
        }
        for (int i = 0; i < D.COMMBOXIDLEN; i++) {
            _shared.info.id[i] = temp[pos++];
        }
        for (int i = 0; i < D.VERSIONLEN; i++) {
            _shared.info.version[i] = temp[pos++];
        }
        _shared.info.port[0] = new Integer(D.NULLADD).byteValue();
        _shared.info.port[1] = new Integer(D.NULLADD).byteValue();
        _shared.info.port[2] = new Integer(D.NULLADD).byteValue();

        _shared.buff.id = D.NULLADD;
        _shared.buff.usedNum = 0;
        for (int i = 0; i < D.MAXIM_BLOCK; i++) {
            _shared.buff.add[i] = new Integer(D.NULLADD).byteValue();
        }
        _shared.buff.add[D.LINKBLOCK] = new Integer(
                _shared.info.cmdBuffLen).byteValue();
        _shared.buff.add[D.SWAPBLOCK] = 0;
        return true;
    }

    // ///////////////////////////////////////////////////////////////////
    // 全局函数 :
    // bool SetPCBaud(BYTE Baud)
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : 一个,有宏定义指明参数
    // BYTE Baud:上位同Commbox通讯速率
    // #define UP_9600BPS 0x00 9600bps
    // #define UP_19200BPS 0x01 19200bps
    // #define UP_38400BPS 0x02 38400bps
    // #define UP_57600BPS 0x03 57600bps
    // #define UP_115200BPS 0x04 115200bps
    // 功能: 用于切换通讯的波特率
    // /////////////////////////////////////////////////////////////////
    private boolean doSetPCBaud(SerialPort port, int baud) {
        try {
            _shared.lastError = 0;
            byte[] temp = new byte[1];
            temp[0] = new Integer(baud).byteValue();
            if (!commboxDo(D.SET_UPBAUD, temp)) {
                return false;
            }
            Thread.sleep(50);
            checkResult(50000);
            setRF(D.SETRFBAUD, baud);
            checkResult(50000);
            switch (baud) {
                case D.UP_9600BPS:
                    port.setBaudrate(9600);
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
                case D.UP_115200BPS:
                    port.setBaudrate(115200);
                    break;
                default:
                    _shared.lastError = D.ILLIGICAL_CMD;
                    return false;
            }
            setRF(D.SETRFBAUD, baud);
            if (!commboxDo(D.SET_UPBAUD, temp)) {
                return false;
            }
            if (!checkResult(150000)) {
                return false;
            }
            port.discardInBuffer();
            port.discardOutBuffer();
            return true;
        } catch (InterruptedException e) {
        } catch (IOException e) {
            _shared.lastError = D.DISCONNECT_COMM;
        }
        return false;

    }

    private boolean setPCBaud(SerialPort port, int baud) {
        int times = D.REPLAYTIMES;
        while (times-- > 0) {
            if (doSetPCBaud(port, baud)) {
                return true;
            }
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////////////
    // 全局函数 :
    // bool OpenComm(BYTE Port)
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : 无
    // 功能: 打开串口，设置通讯时间，初始化Commbox
    // 1.设置通讯时间
    // 2.打开指定串口，设置波特率为START_BAUD
    // 3.调用CheckBox()检查。
    // /////////////////////////////////////////////////////////////////
    private boolean openBox(SerialPort port) {
        try {
            port.setBaudrate(9600);
            port.open();
            for (int i = 0; i < 3; i++) {
                setRF(D.RESET_RF, 0);
                setRF(D.SETDTR_L, 0);
                if (initBox() && checkBox()) {
                    if (setPCBaud(port, D.UP_57600BPS)) {
                        return true;
                    }
                }
            }
            port.close();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean openComm() {
        if (_port.getClass() == SerialPort.class) {
            SerialPort port = (SerialPort) _port;
            if (openBox(port)) {
                return true;
            }
            String[] portNames = SerialPort.getSystemPorts();
            for (String name : portNames) {
                try {
                    port.setPortName(name);
                } catch (IOException e) {
                    continue;
                }
                if (openBox(port)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////////////
    // 全局函数 :
    // bool CloseComm()
    //
    // 返回值: bool : 执行成功返回1,否则返回0
    // 参数 : 无
    // 功能: 复位Commbox,停止链路保持，复位校验，关闭串口
    // /////////////////////////////////////////////////////////////////
    public boolean closeComm() {
        stopNow(true);
        doSet(D.RESET, null);
        setRF(D.RESET_RF, 0);
        if (_port.getClass() == SerialPort.class) {
            try {
                ((SerialPort) _port).close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数: bool NewBatch(BYTE BuffID)
    // 申请一个新的数据缓冲区块,并指定其标识
    // 参数: BYTE BuffID： 指定其标识，
    // 返回: 下一命令的块内地址
    // 功能:
    // 0.数据块标识登记是否有其他申请位清除?
    // 1.检查申请的数据块标识是否已存在?
    // 2.检查申请的数据块标识是否在指定的范围内?
    // 3.检查成功,填写CMD_Temp的命令及校验和
    // 4.登记写入的数据块标识.
    // 5.仅处理链路保持区和命令缓冲区,不处理交换区
    // ////////////////////////////////////////////////////////////////////
    public boolean newBatch(int buffID) {
        if (buffID > D.MAXIM_BLOCK) {
            _shared.lastError = D.NODEFINE_BUFF;
            return false;
        }
        if (_shared.buff.id != D.NULLADD) {
            _shared.lastError = D.APPLICATION_NOW;
            return false;
        }
        if ((_shared.buff.add[buffID] & 0xFF) != D.NULLADD
                && buffID != D.LINKBLOCK && !delBatch(buffID)) {
            return false;
        }
        _shared.cmdTemp.clear();
        _shared.cmdTemp.add(new Integer(D.WR_DATA).byteValue());
        _shared.cmdTemp.add((byte) 0x01);
        if (buffID == D.LINKBLOCK) {
            _shared.cmdTemp.add((byte) 0xFF);
            _shared.buff.add[D.LINKBLOCK] = new Integer(
                    _shared.info.cmdBuffLen).byteValue();
        } else {
            _shared.cmdTemp.add(_shared.buff.add[D.SWAPBLOCK]);
        }
        if (_shared.buff.size() <= 1) {
            _shared.lastError = D.BUFFFLOW;
            return false;
        }
        _shared.cmdTemp.add(new Integer(D.WR_DATA + 0x01
                + (_shared.cmdTemp.get(2) & 0xFF)).byteValue());
        _shared.cmdTemp.set(
                0,
                new Integer(_shared.cmdTemp.get(0) & 0xFF
                + _shared.info.headPassword).byteValue());
        _shared.buff.id = buffID;
        _shared.isDoNow = false;
        return true;
    }

    // ////////////////////////////////////////////////////////////////////
    // 静态函数:
    // BYTE AddToBuff(BYTE commandword ,BYTE length,char * data)
    // 向已申请的新数据缓冲区块填入命令
    //
    // 返回: 下一命令的块内地址
    // 参数: commandword 操作命令字
    // length 要写入数据的长度
    // data 写入的数据
    // 功能:
    // 0.数据块标识登记是否有申请?
    // 1.检查是否有足够的空间存储?
    // 2.检查命令的合法性,是否为缓冲区命令?
    // 3.写入命令
    // 4.计算校验
    // ////////////////////////////////////////////////////////////////////
    public boolean addToBuff(int commandWord, byte[] data) {
        ArrayList<Byte> cmdTemp = _shared.cmdTemp;
        int dataLength = cmdTemp.get(1) & 0xFF;
        int checksum = cmdTemp.get(cmdTemp.size() - 1) & 0xFF;
        Shared shared = _shared;
        shared.nextAddress = dataLength + (data != null ? data.length : 0) + 1;
        Buffer buffer = shared.buff;
        if (buffer.id == D.NULLADD) {
            // 数据块标识登记是否有申请?
            shared.lastError = D.NOAPPLICATBUFF;
            shared.isDoNow = true;
            return false;
        }
        if (buffer.size() < shared.nextAddress) {
            // 检查是否有足够的空间存储?
            shared.lastError = D.BUFFFLOW;
            shared.isDoNow = true;
            return false;
        }
        if (commandWord < D.RESET && commandWord != D.CLR_LINK
                && commandWord != D.DO_BAT_00 && commandWord != D.D0_BAT
                && commandWord != D.D0_BAT_FOR && commandWord != D.WR_DATA) {
            // 是否为缓冲区命令?
            if ((data == null || data.length <= D.CMD_DATALEN)
                    || (commandWord == D.SEND_DATA && data.length < D.SEND_LEN)) {
                // 是否合法命令?
                if (commandWord == D.SEND_DATA && getBoxVer() > 0x400) {
                    // 增加发送长命令
                    int index = cmdTemp.size() - 1;
                    byte element = new Integer(D.SEND_CMD).byteValue();
                    cmdTemp.set(index, element);
                    checksum += D.SEND_CMD;
                    dataLength++;
                    element = new Integer(commandWord
                            + (data != null ? data.length : 0)).byteValue();

                    if (data != null && data.length != 0) {
                        element = new Integer(element & 0xFF - 1).byteValue();
                    }
                    cmdTemp.add(element);
                    if (data != null) {
                        for (byte d : data) {
                            dataLength++;
                            cmdTemp.add(d);
                            checksum += d & 0xFF;
                        }
                    }
                    checksum += (data != null ? data.length : 0) + 2;
                    dataLength++;
                    cmdTemp.add(new Integer(checksum).byteValue());
                    cmdTemp.set(1, new Integer(dataLength).byteValue());
                    shared.nextAddress++;
                } else {
                    int index = cmdTemp.size() - 1;
                    byte element = new Integer(commandWord
                            + (data != null ? data.length : 0)).byteValue();
                    if (data != null && data.length != 0) {
                        element = new Integer(element & 0xFF - 1).byteValue();
                    }
                    cmdTemp.set(index, element);
                    checksum += element & 0xFF;
                    if (data != null) {
                        for (byte d : data) {
                            dataLength++;
                            cmdTemp.add(d);
                            checksum += d & 0xFF;
                        }
                    }
                    dataLength++;
                    checksum += (data != null ? data.length : 0) + 1;
                    cmdTemp.add(new Integer(checksum).byteValue());
                    cmdTemp.set(1, new Integer(dataLength).byteValue());
                    shared.nextAddress++;
                }
                return true;
            }
            shared.lastError = D.ILLIGICAL_LEN;
            shared.isDoNow = true;
            return false;
        }
        shared.lastError = D.UNBUFF_CMD;
        shared.isDoNow = true;
        return false;
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:bool EndBatch()
    //
    // 返回: bool : 成功返回1，失败返回0
    // 参数: 无
    // 功能:
    // 0.数据块标识登记是否有申请?
    // 1.发送命令到Commbox
    // 2.接受结果信息
    // 3.如超时接到数据,或返回错误信息
    // 4.修改命令缓冲区信息
    // 5.清除登记
    // ////////////////////////////////////////////////////////////////////
    public boolean endBatch() {
        int times = D.REPLAYTIMES;
        Shared shared = _shared;
        shared.isDoNow = true;
        Buffer buff = shared.buff;
        ArrayList<Byte> cmdTemp = shared.cmdTemp;
        if (buff.id == D.NULLADD) {
            // 数据块标识登记是否有申请?
            shared.lastError = D.NOAPPLICATBUFF;
            return false;
        }
        if ((cmdTemp.size() - 2) == 0x01) {
            buff.id = D.NULLADD;
            shared.lastError = D.NOADDDATA;
            return false;
        }
        while ((times--) > 0) {
            try {
                if (times == 0) {
                    buff.id = D.NULLADD;
                    return false;
                }
                byte[] temp = new byte[cmdTemp.size()];
                for (int i = 0; i < cmdTemp.size(); i++) {
                    temp[i] = cmdTemp.get(i);
                }
                if (!checkIdle() || (_port.write(temp) != temp.length)) {
                    continue;
                } else if (sendOk(20 * temp.length)) {
                    break;
                }
                if (!stopNow(true)) {
                    buff.id = D.NULLADD;
                    return false;
                }
            } catch (IOException e) {
                continue;
            }
        }
        Information info = shared.info;
        if (buff.id == D.LINKBLOCK) {
            buff.add[D.LINKBLOCK] = new Integer(info.cmdBuffLen
                    - (cmdTemp.get(1) & 0xFF)).byteValue();
        } else {
            buff.add[buff.id] = buff.add[D.SWAPBLOCK];
            buff.used[buff.usedNum] = new Integer(buff.id).byteValue();
            buff.usedNum++;
            buff.add[D.SWAPBLOCK] = new Integer((buff.add[D.SWAPBLOCK] & 0xFF)
                    + (cmdTemp.get(1) & 0xFF)).byteValue();
        }
        buff.id = D.NULLADD;
        return true;
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // bool DelBatch(BYTE BuffID)
    //
    // 参数: BYTE BuffID 指出要删除的命令缓存标号
    // 返回: 成功返回1，失败返回0。
    // 功能: 删除的指定已使用命令缓存块
    // 0.缓冲区数据块不存在，返回错误
    // 1.是否缓冲区数据块正在申请，则删除正常退出。
    // 2.复位申请缓冲区记录，复位申请缓冲区的地址。
    // ////////////////////////////////////////////////////////////////////
    public boolean delBatch(int buffID) {
        if (buffID > D.MAXIM_BLOCK) {
            // 数据块不存在
            _shared.lastError = D.NODEFINE_BUFF;
            return false;
        }
        if (_shared.buff.id == buffID) {
            _shared.buff.id = D.NULLADD;
            return true;
        }
        if ((_shared.buff.add[buffID] & 0xFF) == D.NULLADD) {
            // 数据块标识登记是否有申请?
            _shared.lastError = D.NOUSED_BUFF;
            return false;
        }

        if (buffID == D.LINKBLOCK) {
            _shared.buff.add[D.LINKBLOCK] = new Integer(
                    _shared.info.cmdBuffLen).byteValue();
        } else {
            int i = 0;
            for (; i < _shared.buff.usedNum; i++) {
                if ((_shared.buff.used[i] & 0xFF) == buffID) {
                    break;
                }
            }
            byte[] data = new byte[3];
            data[0] = _shared.buff.add[buffID];
            if (i < _shared.buff.usedNum - 1) {
                data[1] = _shared.buff.add[_shared.buff.used[i + 1] & 0xFF];
                data[2] = new Integer(
                        (_shared.buff.add[D.SWAPBLOCK] & 0xFF)
                        - (data[1] & 0xFF)).byteValue();
                if (!doSet(D.COPY_DATA - D.COPY_DATA % 4, data)) {
                    return false;
                }
            } else {
                data[1] = _shared.buff.add[D.SWAPBLOCK];
            }
            int deleteBuffLen = (data[1] & 0xFF) - (data[0] & 0xFF);
            for (i = i + 1; i < _shared.buff.usedNum; i++) {
                _shared.buff.used[i - 1] = _shared.buff.used[i];
                _shared.buff.add[_shared.buff.used[i] & 0xFF] = new Integer(
                        (_shared.buff.add[_shared.buff.used[i] & 0xFF] & 0xFF)
                        - deleteBuffLen).byteValue();
            }
            _shared.buff.usedNum--;
            _shared.buff.add[D.SWAPBLOCK] = new Integer(
                    (_shared.buff.add[D.SWAPBLOCK] & 0xFF)
                    - deleteBuffLen).byteValue();
            _shared.buff.add[buffID] = new Integer(D.NULLADD).byteValue();
        }
        return true;
    }

    private boolean sendToBox(int commandWord, byte[] buff) {
        if (_shared.isDoNow) {
            return doSet(commandWord, buff);
        } else {
            return addToBuff(commandWord, buff);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetLineLevel(BYTE ValueLow , BYTE ValueHigh)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE ValueLow :指出要设低的断口
    // BYTE ValueHigh:指出要设高的断口
    // 功能: 设置通讯口线的电平位置，
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setLineLevel(int valueLow, int valueHigh) {
        // 只有一个字节的数据，设定端口1
        Information info = _shared.info;
        info.port[1] = new Integer((info.port[1] & 0xFF) & ~valueLow).byteValue();
        info.port[1] = new Integer((info.port[1] & 0xFF) | valueHigh).byteValue();
        byte[] temp = new byte[1];
        temp[0] = info.port[1];
        return sendToBox(D.SETPORT1, temp);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetCommCtrl(BYTE ValueOpen , BYTE ValueClose )
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE ValueOpen :指出要打开的开关
    // BYTE ValueClose:指出要关闭的开关
    // 功能: 设置通讯物理曾的位置，
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setCommCtrl(int valueOpen, int valueClose) {
        // 只有一个字节的数据，设定端口2
        Information info = _shared.info;
        info.port[2] = new Integer((info.port[2] & 0xFF) & ~valueOpen).byteValue();
        info.port[2] = new Integer((info.port[2] & 0xFF) | valueClose).byteValue();
        byte[] temp = new byte[1];
        temp[0] = info.port[2];
        return sendToBox(D.SETPORT2, temp);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetCommLine(BYTE SendLine , BYTE RecLine)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE SendLine :指出要发送的数据断口
    // BYTE RecLine :指出要接受的数据断口
    // 功能: 设置通讯线路的断口
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setCommLine(int sendLine, int recvLine) {
        // 只有一个字节的数据，设定端口0
        if (sendLine > 7) {
            sendLine = 0x0F;
        }
        if (recvLine > 7) {
            recvLine = 0x0F;
        }
        _shared.info.port[0] = new Integer(sendLine + recvLine * 16).byteValue();
        byte[] temp = new byte[1];
        temp[0] = _shared.info.port[0];
        return sendToBox(D.SETPORT0, temp);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE TurnOverOneByOne()
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 两个：
    // 功能: 设置通讯链路为标准下的，翻转OneByOne标志
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean turnOverOneByOne() {
        // 将原有的接受一个发送一个的标志翻转
        return sendToBox(D.SET_ONEBYONE, null);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetEchoData(BYTE EchoLen , BYTE * EchoBuff)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE EchoLen :回传数据长度
    // BYTE * EchoBuff :回传数据缓存
    // 功能: 设置执行将EchoBuff中的数值原样返回。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setEchoData(byte[] echoBuff) {
        if (echoBuff == null || echoBuff.length == 0 || echoBuff.length > 4) {
            _shared.lastError = D.ILLIGICAL_LEN;
            return false;
        }
        if (_shared.isDoNow) {
            if (!commboxDo(D.Echo, echoBuff)) {
                return false;
            }
            byte[] temp = readData(echoBuff.length, 100);
            if (temp == null) {
                return false;
            }
            for (int i = 0; i < echoBuff.length; i++) {
                if (temp[i] != echoBuff[i]) {
                    _shared.lastError = D.CHECKSUM_ERROR;
                    return false;
                }
            }
            return checkResult(100000);
        } else {
            return addToBuff(D.Echo, echoBuff);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE KeepLink(bool IsRunLink)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 三个：
    // bool IsRunLink :启动链路保持设1，停止链路保持为0
    // 功能: 设置启动或停止链路保持。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean keepLink(boolean isRunLink) {
        if (isRunLink) {
            return sendToBox(D.RUNLINK, null);
        } else {
            return sendToBox(D.STOPLINK, null);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetCommLink(BYTE CtrlWord1,BYTE CtrlWord2,BYTE CtrlWord3)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 五个：
    // BYTE CtrlWord1 :通讯方式设定
    // #define RS_232 0x00 //标准RS232方式
    // #define EXRS_232 0x20 //扩展RS232方式
    // #define SET_VPW 0x40 //vpw方式
    // #define SET_PWM 0x60 //pwm方式
    // #define BIT9_SPACE 0x00 //标准RS232方式的校验位为0
    // #define BIT9_MARK 0x01 //标准RS232方式的校验位为1
    // #define BIT9_EVEN 0x02 //标准RS232方式的校验位为偶校验
    // #define BIT9_ODD 0x03 //标准RS232方式的校验位为寄校验
    // #define SEL_SL 0x00 //选择标准通讯通道
    // #define SEL_DL0 0x08 //选择DL0
    // #define SEL_DL1 0x10 //选择DL1
    // #define SEL_DL2 0x18 //选择DL2
    // #define SET_DB20 0x04 //设置通讯波特率为DB20方式
    // #define UN_DB20 0x00 //设置通讯波特率为正常方式
    //
    // BYTE CtrlWord2 :在扩展RS232方式下，为数据位的个数，在标准RS232方式下
    // 为接受一怔数据的长度偏移
    // BYTE CtrlWord3 :在标准RS232方式下，为长度所在接受的位置，将该位置的数值同CtrlWord2
    // 相加，表示以后要接的数据长度。
    // #define ONEBYONE 0x80 //在标准RS232方式下，设定发一个接一个链路
    // #define INVERTBYTE 0x40 //在标准RS232方式下，设定接一个取反发回
    // #define ORIGNALBYTE 0X00 //在标准RS232方式下，设定接一个原样发回
    // 若在标准RS232方式下，CtrlWord3的长度所在接受的位置为零，则按BBTIMEOUT超时取帧。
    // 功能: 设置启动或停止链路保持。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setCommLink(int ctrlWord1, int ctrlWord2, int ctrlWord3) {
        byte[] ctrlWord = new byte[3]; // 通讯控制字3
        int modeControl = ctrlWord1 & 0xE0;
        ctrlWord[0] = new Integer(ctrlWord1).byteValue();
        if ((ctrlWord1 & 0x04) != 0) {
            _shared.isDb20 = true;
        } else {
            _shared.isDb20 = false;
        }
        if (modeControl == D.SET_VPW || modeControl == D.SET_PWM) {
            byte[] temp = new byte[1];
            temp[0] = ctrlWord[0];
            return sendToBox(D.SETTING, temp);
        } else {
            ctrlWord[1] = new Integer(ctrlWord2).byteValue();
            ctrlWord[2] = new Integer(ctrlWord3).byteValue();
            byte[] temp = null;
            if (ctrlWord3 == 0) {
                if (ctrlWord2 == 0) {
                    temp = new byte[1];
                    temp[0] = ctrlWord[0];
                } else {
                    temp = new byte[2];
                    temp[0] = ctrlWord[0];
                    temp[1] = ctrlWord[1];
                }
            } else {
                temp = new byte[3];
                temp[0] = ctrlWord[0];
                temp[1] = ctrlWord[1];
                temp[2] = ctrlWord[2];
            }
            return sendToBox(D.SETTING, temp);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetCommBaud(DWORD Baud)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 三个：
    // DWORD Baud :设定波特率
    // 功能: 设置启动或停止链路保持。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean setCommBaud(double baud) {
        byte[] baudTime = new byte[2];
        double instructNum = ((1000000.0 / (_shared.info.timeUnit)) * 1000000)
                / baud;
        if (_shared.isDb20) {
            instructNum /= 20;
        }
        instructNum += 0.5;
        if (instructNum > 65535 || instructNum < 10) {
            _shared.lastError = D.COMMBAUD_OUT;
            return false;
        }
        baudTime[0] = new Integer(new Double(instructNum / 256).intValue()).byteValue();
        baudTime[1] = new Integer(new Double(instructNum % 256).intValue()).byteValue();
        if (baudTime[0] == 0) {
            byte[] temp = new byte[1];
            temp[0] = baudTime[1];
            return sendToBox(D.SETBAUD, temp);
        } else {
            return sendToBox(D.SETBAUD, baudTime);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE SetCommTime(BYTE Type,DWORD Time)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 三个：
    // BYTE Type :设定时间类型
    // DWORD Time :设定时间值
    // 功能: 设置各种通讯时间。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    private void getLinkTime(int type, int time) {
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
            if (D.SETVPWRECS == type) {
                time = (time * 2) / 3;
            }
            type = type + (D.SETBYTETIME & 0xF0);
            time = new Long(time
                    / ((_shared.info.timeUnit / 1000000))).intValue();
        } else {
            time = new Long((time / _shared.info.timeBaseDB)
                    / (_shared.info.timeUnit / 1000000)).intValue();
        }
        if (time > 65535) {
            _shared.lastError = D.COMMTIME_OUT;
            return false;
        }
        if (type == D.SETBYTETIME || type == D.SETWAITTIME
                || type == D.SETRECBBOUT || type == D.SETRECFROUT
                || type == D.SETLINKTIME) {
            timeBuff[0] = new Integer(time / 256).byteValue();
            timeBuff[1] = new Integer(time % 256).byteValue();
            if (timeBuff[0] == 0) {
                byte[] temp = new byte[1];
                temp[0] = timeBuff[1];
                return sendToBox(type, temp);
            } else {
                return sendToBox(type, timeBuff);
            }
        }
        _shared.lastError = D.UNDEFINE_CMD;
        return false;
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局通讯函数:
    // BYTE RunReceive(BYTE Type)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 三个：
    // BYTE Type :设定接受数据类型
    // 功能: 设置各种通讯时间，凡为通讯函数，都要在读取数据后判断操作是否完成。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean runReceive(int type) {
        if (type == D.GET_PORT1) {
            _shared.isDb20 = false;
        }
        if (type == D.GET_PORT1 || type == D.SET55_BAUD
                || (type >= D.REC_FR && type <= D.RECEIVE)) {
            if (_shared.isDoNow) {
                return commboxDo(type, null);
            } else {
                return addToBuff(type, null);
            }
        }
        _shared.lastError = D.UNDEFINE_CMD;
        return false;
    }

    // ////////////////////////////////////////////////////////////////////
    // 静态函数:
    // bool GetAstractAdd(BYTE BuffID ,BYTE * BuffAdd)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。
    // 参数: 两个：
    // BYTE BuffID :指定块标号
    // BYTE * BuffAdd :指定块内地址，返回绝对地址
    // 功能: 得到缓存中的绝对地址
    // ////////////////////////////////////////////////////////////////////
    public int getAbsAdd(int buffID, int add) {
        Buffer buff = _shared.buff;
        Information info = _shared.info;
        int length = 0;
        int startAdd = 0;
        if (buff.id != buffID) {
            if ((buff.add[buffID] & 0xFF) == D.NULLADD) {
                _shared.lastError = D.NOUSED_BUFF;
                return -1;
            }
            if (buffID == D.LINKBLOCK) {
                length = info.cmdBuffLen - (buff.add[D.LINKBLOCK] & 0xFF);
            } else {
                int i;
                for (i = 0; i < buff.usedNum; i++) {
                    if ((buff.used[i] & 0xFF) == buffID) {
                        break;
                    }
                }
                if (i == (buff.usedNum - 1)) {
                    length = buff.add[D.SWAPBLOCK] - (buff.add[buffID] & 0xFF);
                } else {
                    length = buff.add[buffID + 1] - buff.add[buffID];
                }
            }
            startAdd = buff.add[buffID] & 0xFF;
        } else {
            length = (buff.add[D.LINKBLOCK] & 0xFF)
                    - (buff.add[D.SWAPBLOCK] & 0xFF);
            startAdd = buff.add[D.SWAPBLOCK] & 0xFF;
        }
        if (add < length) {
            return add + startAdd;
        }
        _shared.lastError = D.OUTADDINBUFF;
        return -1;
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // BYTE UpdateBuff(BYTE Type, BYTE * Buffer)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE Type :对缓冲区操作的类型
    // BYTE * Buffer :操作的数据和地址，地址有2个字节表示，低字节表示块，高字节表示块内地址
    // 功能: 修改数据缓存取，详见各个类型的功能
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean updateBuff(int type, byte[] buffer) {
        byte[] cmdTemp = new byte[4];
        _shared.lastError = 0;
        int ret = getAbsAdd(buffer[0] & 0xFF, buffer[1] & 0xFF);
        if (ret == -1) {
            return false;
        }
        cmdTemp[0] = new Integer(ret).byteValue();

        switch (type) {
            case D.INVERT_DATA: // add
            case D.DEC_DATA:
            case D.INC_DATA:
                break;
            case D.UPDATE_1BYTE: // add + data
            case D.SUB_BYTE:
                cmdTemp[1] = buffer[2];
                break;
            case D.INC_2DATA: // add + add
                ret = getAbsAdd(buffer[2] & 0xFF, buffer[3] & 0xFF);
                if (ret == -1) {
                    return false;
                }
                cmdTemp[1] = new Integer(ret).byteValue();
                break;
            case D.COPY_DATA: // add + add + data
            case D.ADD_1BYTE:
                ret = getAbsAdd(buffer[2] & 0xFF, buffer[3] & 0xFF);
                if (ret == -1) {
                    return false;
                }
                cmdTemp[1] = new Integer(ret).byteValue();
                cmdTemp[2] = buffer[4];
                break;
            case D.UPDATE_2BYTE: // add + data + add + data
            case D.ADD_2BYTE:
                ret = getAbsAdd(buffer[3] & 0xFF, buffer[4] & 0xFF);
                if (ret == -1) {
                    return false;
                }
                cmdTemp[1] = buffer[2];
                cmdTemp[2] = new Integer(ret).byteValue();
                cmdTemp[3] = buffer[5];
                break;
            case D.ADD_DATA: // add + add + add
            case D.SUB_DATA:
                ret = getAbsAdd(buffer[2], buffer[3] & 0xFF);
                if (ret == -1) {
                    return false;
                }
                cmdTemp[1] = new Integer(ret).byteValue();
                ret = getAbsAdd(buffer[4], buffer[5] & 0xFF);
                if (ret == -1) {
                    return false;
                }
                cmdTemp[2] = new Integer(ret).byteValue();
                break;
            default:
                _shared.lastError = D.UNDEFINE_CMD;
                return false;
        }
        int size = type % 4 + 1;
        byte[] temp = new byte[size];
        System.arraycopy(cmdTemp, 0, temp, 0, size);
        return sendToBox(type - type % 4, temp);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局通讯函数:
    // BYTE CommboxDelay(DWORD Time)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // DWORD Time :设置下位机延时时间以微秒单位
    // 功能: 使用后，需判断是否执行结束。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean commboxDelay(int time) {
        byte[] timeBuff = new byte[2];
        int delayWord = D.DELAYSHORT;
        Information info = _shared.info;
        time = new Long((time * 1000) / (info.timeUnit / 1000000)).intValue();
        if (time == 0) {
            _shared.lastError = D.SETTIME_ERROR;
            return false;
        }
        if (time > 65535) {
            time = new Long(time / info.timeBaseDB).intValue();
            if (time > 65535) {
                time = new Long(time / info.timeBaseDB).intValue();
                if (time > 65535) {
                    _shared.lastError = D.COMMTIME_OUT;
                    return false;
                }
                delayWord = D.DELAYLONG;
            } else {
                delayWord = D.DELAYTIME;
            }
        }
        timeBuff[0] = new Integer(time / 256).byteValue();
        timeBuff[1] = new Integer(time % 256).byteValue();
        if (timeBuff[0] == 0) {
            if (_shared.isDoNow) {
                return commboxDo(delayWord, new byte[]{timeBuff[1]});
            } else {
                return addToBuff(delayWord, new byte[]{timeBuff[1]});
            }
        } else {
            if (_shared.isDoNow) {
                return commboxDo(delayWord, timeBuff);
            } else {
                return addToBuff(delayWord, timeBuff);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局通讯函数:
    // BYTE SendOutData(BYTE length,BYTE * Buffer)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。在写入缓存时，成功返回下调指令存放位置
    // 参数: 四个：
    // BYTE length :发送数据长度
    // BYTE * Buffer :发送数据缓存
    // 功能: 使用后，需判断是否执行结束。
    // 如要立即执行，则返回执行结果，如写入缓冲区，返回下条命令的地址
    // ////////////////////////////////////////////////////////////////////
    public boolean sendOutData(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            _shared.lastError = D.ILLIGICAL_LEN;
            return false;
        }
        if (_shared.isDoNow) {
            return commboxDo(D.SEND_DATA, buffer);
        } else {
            return addToBuff(D.SEND_DATA, buffer);
        }
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局通讯函数:
    // bool RunBatch(BYTE Lenght , BYTE *BuffID , bool IsExcutManyTime)
    //
    // 返回值: BYTE ：成功返回1，失败返回0。
    // 参数: 三个：
    // BYTE length :执行命令块的个数
    // BYTE * Buffer :执行命令块的标识，按执行的先后顺序存放的缓存
    // bool IsExcutManyTime :须不断循环执行吗
    // 功能: 使用后，需判断是否执行结束。
    // ////////////////////////////////////////////////////////////////////
    public boolean runBatch(boolean isExecuteMany, int... buffID) {
        int commandWord = D.D0_BAT;
        Buffer buff = _shared.buff;
        for (int i = 0; i < buffID.length; i++) {
            if ((buff.add[buffID[i]] & 0xFF) == D.NULLADD) {
                _shared.lastError = D.NOUSED_BUFF;
                return false;
            }
        }
        if (isExecuteMany) {
            commandWord = D.D0_BAT_FOR;
        }
        if (commandWord == D.D0_BAT && buffID[0] == buff.used[0]) {
            commandWord = D.DO_BAT_00;
            return commboxDo(commandWord, null);
        }
        byte[] temp = new byte[buffID.length];
        for (int i = 0; i < buffID.length; i++) {
            temp[i] = new Integer(buffID[0]).byteValue();
        }
        return commboxDo(commandWord, temp);
    }

    // ////////////////////////////////////////////////////////////////////
    // 全局函数:
    // SetRF(BYTE CMD,BYTE CMDInfo)
    //
    // 返回值: bool ：返回值 0:通讯错误或不连通
    // 1:是连通的
    // 参数: 两个
    // BYTE CMD, 设定命令
    // BYTE CMDInfo, 命令信息
    // 功能: 1.用以设定无线通讯的参数
    // ////////////////////////////////////////////////////////////////////
    private boolean setRF(int cmd, int cmdInfo) {
        try {
            int times = D.REPLAYTIMES;
            cmdInfo += cmd;
            if (cmd == D.SETRFBAUD) {
                times = 2;
            }
            byte[] temp = new byte[1];
            temp[0] = new Integer(cmdInfo).byteValue();
            Thread.sleep(6);
            while (times-- > 0) {
                if (checkIdle() && (_port.write(temp) == 1)) {
                    if (!sendOk(50)) {
                        continue;
                    }
                    if ((_port.write(temp) != 1) || !checkResult(150000)) {
                        continue;
                    }
                    Thread.sleep(100);
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        return false;
    }

    byte[] readBytes(int count) {
        return readData(count, _shared.resWaitTime / 1000);
    }
}
