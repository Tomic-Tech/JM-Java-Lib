package jm.device.v1;

//////////////////////////////////////////////////////////////////////
//维护COMMBOX数据缓冲区的数据结构	CMDBUFFINFOSTRUCT
//组成:
//BYTE CMDBuffID = 0;					//当前登记的命令区的索引
//BYTE CMDB_UsedNum=0;				//在命令缓冲区已使用的命令区地址连续纪录数
//BYTE CMDBuff_ADD[MAXIM_BLOCK+2];	//命令缓冲区地址
//BYTE CMDBuff_Used[MAXIM_BLOCK];		//已使用的命令区连续纪录
//////////////////////////////////////////////////////////////////////
final class Buffer {

    public int id;
    public int usedNum;
    public byte[] add = new byte[D.MAXIM_BLOCK + 2];
    public byte[] used = new byte[D.MAXIM_BLOCK];

    public int size() {
        return (add[D.LINKBLOCK] & 0xFF) - (add[D.SWAPBLOCK] & 0xFF);
    }
}
