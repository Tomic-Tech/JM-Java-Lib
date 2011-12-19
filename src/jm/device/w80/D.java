/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.w80;

/**
 *
 * @author Ogilvy
 */
class D {

    public static final int BOXINFO_LEN = 12;
    public static final int MAXPORT_NUM = 4;
    public static final int MAXBUFF_NUM = 4;
    public static final int MAXBUFF_LEN = 0xA8;
    public static final int LINKBLOCK = 0x40;
    //批处理执行次数
    public static final int RUN_ONCE = 0x00;
    public static final int RUN_MORE = 0x01;
    //通讯校验和方式
    public static final int CHECK_SUM = 0x01;
    public static final int CHECK_REVSUM = 0x02;
    public static final int CHECK_REC = 0x03;
    ///////////////////////////////////////////////////////////////////////////////
    //  通讯口 PORT
    ///////////////////////////////////////////////////////////////////////////////
    public static final int DH = 0x80; //高电平输出,1为关闭,0为打开
    public static final int DL2 = 0x40; //低电平输出,1为关闭,0为打开,正逻辑发送通讯线
    public static final int DL1 = 0x20; //低电平输出,1为关闭,0为打开,正逻辑发送通讯线,带接受控制
    public static final int DL0 = 0x10; //低电平输出,1为关闭,0为打开,正逻辑发送通讯线,带接受控制
    public static final int PWMS = 0x08; //PWM发送线
    public static final int PWMR = 0x04;
    public static final int COMS = 0x02; //标准发送通讯线路
    public static final int COMR = 0x01;
    public static final int SET_NULL = 0x00; //不选择任何设置
    ///////////////////////////////////////////////////////////////////////////////
    //  通讯物理控制口
    ///////////////////////////////////////////////////////////////////////////////
    public static final int PWC = 0x80; //通讯电平控制,1为5伏,0为12伏
    public static final int REFC = 0x40; //通讯比较电平控制,1为通讯电平1/5,0为比较电平控制1/2
    public static final int CK = 0x20; //K线控制开关,1为双线通讯,0为单线通讯
    public static final int SZFC = 0x10; //发送逻辑控制,1为负逻辑,0为正逻辑
    public static final int RZFC = 0x08; //接受逻辑控制,1为负逻辑,0为正逻辑
    public static final int DLC0 = 0x04; //DLC1接受控制,1为接受关闭,0为接受打开
    public static final int DLC1 = 0x02; //DLC0接受控制,1为接受关闭,0为接受打开
    public static final int SLC = 0x01; //线选地址锁存器控制线(待用)
    public static final int CLOSEALL = 0x08; //关闭所有发送口线，和接受口线
    ///////////////////////////////////////////////////////////////////////////////
    //  通讯控制字1设定
    ///////////////////////////////////////////////////////////////////////////////
    public static final int RS_232 = 0x00;
    public static final int EXRS_232 = 0x20;
    public static final int SET_VPW = 0x40;
    public static final int SET_PWM = 0x60;
    public static final int BIT9_SPACE = 0x00;
    public static final int BIT9_MARK = 0x01;
    public static final int BIT9_EVEN = 0x02;
    public static final int BIT9_ODD = 0x03;
    public static final int SEL_SL = 0x00;
    public static final int SEL_DL0 = 0x08;
    public static final int SEL_DL1 = 0x10;
    public static final int SEL_DL2 = 0x18;
    public static final int SET_DB20 = 0x04;
    public static final int UN_DB20 = 0x00;
    
    ///////////////////////////////////////////////////////////////////////////////
    //  通讯控制字3设定
    ///////////////////////////////////////////////////////////////////////////////
    public static final int ONEBYONE = 0x80;
    public static final int INVERTBYTE = 0x40;
    public static final int ORIGNALBYTE = 0x00;
    ///////////////////////////////////////////////////////////////////////////////
    //  接受命令类型定义
    ///////////////////////////////////////////////////////////////////////////////
    public static final int WR_DATA = 0x00;
    public static final int WR_LINK = 0xFF;
    public static final int STOP_REC = 0x04;
    public static final int STOP_EXECUTE = 0x08;
    public static final int SET_UPBAUD = 0x0C;
    public static final int UP_9600BPS = 0x00;
    public static final int UP_19200BPS = 0x01;
    public static final int UP_38400BPS = 0x02;
    public static final int UP_57600BPS = 0x03;
    public static final int UP_115200BPS = 0x04;
    public static final int RESET = 0x10;
    public static final int GET_CPU = 0x14;
    public static final int GET_TIME = 0x18;
    public static final int GET_SET = 0x1C;
    public static final int GET_LINK = 0x20;
    public static final int GET_BUF = 0x24;
    public static final int GET_CMD = 0x28;
    public static final int GET_PORT = 0x2C;
    public static final int GET_BOXID = 0x30;
    
    public static final int DO_BAT_C = 0x34;
    public static final int DO_BAT_CN = 0x38;
    public static final int DO_BAT_L = 0x3C;
    public static final int DO_BAT_LN = 0x40;
    
    public static final int SET55_BAUD = 0x44;
    public static final int SET_ONEBYONE = 0x48;
    public static final int SET_BAUD = 0x4C;
    public static final int RUN_LINK = 0x50;
    public static final int STOP_LINK = 0x54;
    public static final int CLEAR_LINK = 0x58;
    public static final int GET_PORT1 = 0x5C;
    
    public static final int SEND_DATA = 0x60;
    public static final int SET_CTRL = 0x64;
    public static final int SET_PORT0 = 0x68;
    public static final int SET_PORT1 = 0x6C;
    public static final int SET_PORT2 = 0x70;
    public static final int SET_PORT3 = 0x74;
    public static final int DELAYSHORT = 0x78;
    public static final int DELAYTIME = 0x7C;
    public static final int DELAYDWORD = 0x80;
    
    public static final int SETBYTETIME = 0x88;
    public static final int SETVPWSTART = 0x08; //最终要将SETVPWSTART转换成SETBYTETIME
    public static final int SETWAITTIME = 0x8C;
    public static final int SETLINKTIME = 0x90;
    public static final int SETRECBBOUT = 0x94;
    public static final int SETRECFROUT = 0x98;
    public static final int SETVPWRECS = 0x14; //最终要将SETVPWRECS转换成SETRECBBOUT
    
    public static final int COPY_BYTE = 0x9C;
    public static final int UPDATE_BYTE = 0xA0;
    public static final int INC_BYTE = 0xA4;
    public static final int DEC_BYTE = 0xA8;
    public static final int ADD_BYTE = 0xAC;
    public static final int SUB_BYTE = 0xB0;
    public static final int INVERT_BYTE = 0xB4;
    
    public static final int REC_FR = 0xE0;
    public static final int REC_LEN_1 = 0xE1;
    public static final int REC_LEN_2 = 0xE2;
    public static final int REC_LEN_3 = 0xE3;
    public static final int REC_LEN_4 = 0xE4;
    public static final int REC_LEN_5 = 0xE5;
    public static final int REC_LEN_6 = 0xE6;
    public static final int REC_LEN_7 = 0xE7;
    public static final int REC_LEN_8 = 0xE8;
    public static final int REC_LEN_9 = 0xE9;
    public static final int REC_LEN_10 = 0xEA;
    public static final int REC_LEN_11 = 0xEB;
    public static final int REC_LEN_12 = 0xEC;
    public static final int REC_LEN_13 = 0xED;
    public static final int REC_LEN_14 = 0xEE;
    public static final int REC_LEN_15 = 0xEF;
    public static final int RECIEVE = 0xF0;
    
    public static final int RECV_ERR = 0xAA; //接收错误
    public static final int RECV_OK = 0x55; //接收正确
    public static final int BUSY = 0xBB; //开始执行
    public static final int READY = 0xDD; //执行结束
    public static final int ERROR = 0xEE; //执行错误
    
    //RF多对一的设定接口,最多16个
    public static final int RF_RESET = 0xD0;
    public static final int RF_SETDTR_L = 0xD1;
    public static final int RF_SETDTR_H = 0xD2;
    public static final int RF_SET_BAUD = 0xD3;
    public static final int RF_SET_ADDR = 0xD4;
    
    public static final int COMMBOXID_ERR = 1;
    public static final int DISCONNECT_COMM = 2;
    public static final int DISCONNECT_COMMBOX = 3;
    public static final int OTHER_ERROR = 4;
    
    
}
