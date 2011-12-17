package jm.device.v1;

//////////////////////////////////////////////////////////////////////
//CommBox 有关信息数据结构	COMMBOXINFOSTRUCT
//组成:
//LONG CommboxTimeUnit	万分之一微妙
//BYTE TimeBaseDB			标准时间的倍数
//BYTE TimeExternDB		扩展时间的倍数
//BYTE CMDBuff_Len		命令缓冲区的总长度
//BYTE Version[2]			版本信息
//BYTE Commbox_ID[10]		COMMBOX的标示号
//BYTE Commbox_Port[3]	端口值
//BYTE HeadPassword		命令头密码
//////////////////////////////////////////////////////////////////////
final class Information {

    public long timeUnit;
    public long timeBaseDB;
    public long timeExternDB;
    public int cmdBuffLen;
    public byte[] version = new byte[D.VERSIONLEN];
    public byte[] id = new byte[D.COMMBOXIDLEN];
    public byte[] port = new byte[D.COMMBOXPORTNUM];
    public int headPassword;
}