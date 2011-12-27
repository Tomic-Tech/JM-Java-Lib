/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jm.core.utils;
import jm.device.CanbusBaud;
import jm.device.CanbusFilterMask;
import jm.device.CanbusFrameType;
import jm.device.CanbusIDMode;

/**
 *
 * @author Ogilvy
 */
class ISO15765 extends jm.device.Canbus implements IProtocol {

    private static int[] SJA_BTR_CODETABLE = {
        0xBF, 0xFF, // 5KBPS
        0xEF, 0xFF, // 10KBPS
        0xD7, 0xFF, // 20KBPS
        0xCB, 0xFF, // 40KBPS
        0x47, 0x2F, // 50KBPS
        0xC5, 0xFF, // 80KBPS
        0xC9, 0xA7, // 100KBPS
        0x45, 0x2B, // 125KBPS
        0x04, 0xA7, // 200KBPS
        0x01, 0x1C, // 250KBPS
        0x02, 0x25, // 400KBPS
        0x01, 0x45, // 500KBPS
        0x80, 0xB6, // 666KBPS
        0x00, 0x16, // 800KBPS
        0x01, 0x12 // 1000KBPS
    };
    private static final int SET_CANBEGIN = 0xF0;
    private static final int SET_CANBAUD = 0xF1;
    private static final int SET_CANACR = 0xF2;
    private static final int SET_CANAMR = 0xF3;
    private static final int SET_CANMODE = 0xF4;
    private static final int SET_CANCHAN = 0xF5;
    private static final int SET_CANPRIOR = 0xF6;
    private static final int SET_CANEND = 0xFA;
    private static final int SET_CANRESET = 0xFB;
    private static final int CAN_ACR_ACF1 = 0x20;
    private static final int CAN_ACR_ACF2 = 0x28;
    private static final int CAN_ACR_ACF3 = 0x30;
    private static final int CAN_ACR_ACF4 = 0x38;
    private static final int CAN_AMR_ACF1 = 0x24;
    private static final int CAN_AMR_ACF2 = 0x2C;
    private static final int CAN_AMR_ACF3 = 0x34;
    private static final int CAN_AMR_ACF4 = 0x3C;
    private static final int ACF1_FT1_PRIO = 0x01;
    private static final int ACF1_FT2_PRIO = 0x02;
    private static final int ACF2_FT1_PRIO = 0x04;
    private static final int ACF2_FT2_PRIO = 0x08;
    private static final int ACF3_FT1_PRIO = 0x10;
    private static final int ACF3_FT2_PRIO = 0x20;
    private static final int ACF4_FT1_PRIO = 0x40;
    private static final int ACF4_FT2_PRIO = 0x80;
    private static final int ACF1_FT1_CHAN = 0x01;
    private static final int ACF1_FT2_CHAN = 0x02;
    private static final int ACF2_FT1_CHAN = 0x04;
    private static final int ACF2_FT2_CHAN = 0x08;
    private static final int ACF3_FT1_CHAN = 0x10;
    private static final int ACF3_FT2_CHAN = 0x20;
    private static final int ACF4_FT1_CHAN = 0x40;
    private static final int ACF4_FT2_CHAN = 0x80;
    private static final int STD_FRAMEID_LENGTH = 0x02;
    private static final int EXT_FRAMEID_LENGTH = 0x04;
    private static final int SJA_OK = 0;
    private Box _box;
    private Shared _shared;
    private Default<ISO15765> _default;

    public ISO15765(Box box, Shared shared) {
        _box = box;
        _shared = shared;
        _default = new Default<>(_box, _shared, this);
    }

    public void prepare() throws IOException {
        int localID = _idArray[0];
        if (!_box.setCommCtrl(D.RZFC, D.SET_NULL)
                || !_box.setCommLine(D.SK5, D.SK7)
                || !_box.setCommLink(D.RS_232 | D.BIT9_MARK | D.SEL_SL | D.UN_DB20, D.SET_NULL, D.SET_NULL)
                || !_box.setCommBaud(57600)
                || !_box.setCommTime(D.SETBYTETIME, 5000)
                || !_box.setCommTime(D.SETWAITTIME, 55000)
                || !_box.setCommTime(D.SETRECBBOUT, 100000)
                || !_box.setCommTime(D.SETRECFROUT, 200000)
                || !_box.setCommTime(D.SETLINKTIME, 500000)) {
            throw new IOException();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(ISO15765.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!beginSet()
                || !setBaud(_baud)
                || !setAcr(CAN_ACR_ACF1, utils.highByte(utils.highWord(localID << 21)), utils.lowByte(utils.highWord(localID << 21)), utils.highByte(utils.lowWord(localID << 21)), utils.lowByte(utils.lowWord(localID << 21)))
                || !setAmr(CAN_AMR_ACF1, 0x00, 0x1F, 0xFF, 0xFF)
                || !setMode(0x55)
                || !setPrior(0xFF)
                || !setChan(ACF1_FT1_CHAN)
                || !endSet()) {
            throw new IOException();
        }
    }

    @Override
    public void setLines(int high, int low) throws IllegalArgumentException {
        _high = high;
        _low = low;
    }

    @Override
    public void setFilter(int... idArray) throws IllegalArgumentException {
        _idArray = idArray;
    }

    @Override
    public void setOptions(int id, CanbusBaud baud, CanbusIDMode idMode, CanbusFilterMask mask, CanbusFrameType frame) throws IllegalArgumentException {
        _baud = baud;
        _idMode = idMode;
        _mask = mask;
        _frameType = frame;
        if (_idMode.equals(CanbusIDMode.STD)) {
            _targetID = (id << 5) & 0x0000FFFF;
        } else {
            _targetID = id;
        }
    }

    @Override
    public void sendOneFrame(byte[] data) throws IOException {
        _default.sendOneFrame(data, false);
    }

    @Override
    public void sendFrames(byte[] data) throws IOException {
        if ((data.length <= 0) || (data.length > 0xFFF)) {
            throw new IOException();
        }

        if (data.length < 8) {
            byte[] temp = new byte[8];
            temp[0] = (byte)(data.length);
            System.arraycopy(data, 0, temp, 1, data.length);
            _default.sendOneFrame(temp, true);
        } else {
            int frameCount = (data.length + 1) / 7;
            int lastData = (data.length + 1) % 7;
            int pos = 0;
            byte seq = 0x21;
            int frameIndex = 0;
            if (lastData != 0) {
                frameCount++;
            }
            for (; frameIndex < frameCount; ++frameIndex) {
                if (0 == frameIndex) {
                    byte[] temp = new byte[8];
                    temp[0] = (byte)(0x10 | (data.length >> 8));
                    temp[1] = (byte)(data.length);
                    System.arraycopy(data, pos, temp, 2, 6);
                    pos += 6;
                    _default.sendOneFrame(temp, true);
                    byte[] ret = readOneFrame(true);
                } else if (frameIndex == (frameCount - 1)) {
                    byte[] temp = new byte[8];
                    temp[0] = seq;
                    System.arraycopy(data, pos, temp, 1, data.length - pos);
                    _default.sendOneFrame(temp, true);
                } else {
                    byte[] temp = new byte[8];
                    temp[0] = seq;
                    System.arraycopy(temp, 1, data, pos, 7);
                    pos += 7;
                    _default.sendOneFrame(temp, false);
                    if (seq == 0x2F) {
                        seq = 0x20;
                    } else {
                        seq++;
                    }
                }
            }
        }
    }

    @Override
    public byte[] readOneFrame() throws IOException {
        return readOneFrame(true);
    }

    @Override
    public byte[] readFrames() throws IOException {
        byte[] firstFrame = readOneFrame(true);

        if (firstFrame == null) {
            throw new IOException();
        }

        if (firstFrame[0] == 0x30) {
            finishExecute(true);
            return firstFrame;
        }

        byte[] result = null;
        if ((firstFrame[0] & 0x10) == 0x10) {
            // Multi-frame
            int userDataCount;
            int restFrameCount;
            int dataLength;
            int restDataCount;
            int i;
            byte[] restFrames;
            byte[] unpackedBuff = null;
            finishExecute(true);
            _default.sendOneFrame(_flowControl, false);

            userDataCount = ((firstFrame[0] & 0x0F) << 8) | (firstFrame[1] & 0xFF);
            restFrameCount = ((userDataCount - 6) / 7) + ((((userDataCount - 6) % 7) != 0) ? 1 : 0);
            result = new byte[userDataCount];
            System.arraycopy(firstFrame, 2, result, 0, firstFrame.length - 2);
            int pos = firstFrame.length - 2;

            if (_idMode.equals(CanbusIDMode.STD)) {
                dataLength = STD_FRAMEID_LENGTH + 8;
            } else {
                dataLength = EXT_FRAMEID_LENGTH + 8;
            }

            restDataCount = dataLength * restFrameCount;
            restFrames = new byte[restDataCount];
            if (_box.readBytes(restFrames, 0, restDataCount) <= 0) {
                finishExecute(true);
                throw new IOException();
            }
            finishExecute(true);
            for (i = 0; i < restFrameCount; i++) {
                byte[] temp = new byte[dataLength];
                System.arraycopy(restFrames, i * dataLength, temp, 0, dataLength);
                unpackedBuff = unpack(temp);
                System.arraycopy(unpackedBuff, 0, result, pos, unpackedBuff.length);
                pos += unpackedBuff.length;
            }
        } else {
            while (firstFrame[1] == 0x7F && firstFrame[3] == 0x78) {
                firstFrame = readOneFrame(false);
            }
            finishExecute(true);
            result = unpack(firstFrame);
        }
        return result;
    }

    @Override
    public void finishExecute(boolean isFinish) {
        if (isFinish) {
            _box.stopNow(true);
            _box.delBatch(_shared.buffID);
            _box.checkResult(200000);
        }
    }

    @Override
    public void setKeepLink(byte[] data) throws IOException {
        byte[] buff = pack(data);
        _default.setKeepLink(buff);
    }

    public boolean reset() {
        return doCmd(SET_CANRESET, 0);
    }

    private boolean sendCmd(byte[] data) {
        _shared.buffID = 0;
        if (!_box.newBatch(_shared.buffID)) {
            return false;
        }
        if (!_box.sendOutData(0, data.length, data)
                || !_box.runReceive(D.RECEIVE)
                || !_box.endBatch()
                || !_box.runBatch(false, _shared.buffID)) {
            _box.delBatch(_shared.buffID);
            return false;
        }
        try {
            Thread.sleep((data.length * _shared.reqByteToByte + _shared.reqWaitTime) / 1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ISO15765.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private byte[] readCmd() {
        byte[] temp = new byte[3];
        if (_box.readBytes(temp, 0, 3) <= 0) {
            return null;
        }
        
        ArrayList<Byte> result = new ArrayList<>();
        for (int i = 0; i < temp.length; i++) {
            result.add(temp[i]);
        }

        int length = temp[0] & 0x0F;
        int mode = temp[0] & 0xC0;
        if ((mode == (CanbusIDMode.STD.value() | CanbusFrameType.Data.value())) || (mode == (CanbusIDMode.STD.value() | CanbusFrameType.Remote.value()))) {
            length += STD_FRAMEID_LENGTH;
        } else {
            length += EXT_FRAMEID_LENGTH;
        }

        length -= 2;
        if (length > 0) {
            temp = new byte[length];
            if (_box.readBytes(temp, 0, length) <= 0) {
                finishExecute(true);
                return null;
            }
            for (int i = 0; i < length; i++) {
                result.add(temp[i]);
            }
        }
        finishExecute(true);
        byte[] ret = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            ret[i] = result.get(i);
        }
        return ret;
    }

    private boolean doCmd(int cmd, int inf) {
        byte[] buff = new byte[5];
        buff[0] = 0x20 | 2;
        buff[1] = 0x55;
        buff[2] = (byte)(0xAA);
        buff[3] = (byte)(cmd);
        buff[4] = (byte)(inf);
        if (sendCmd(buff)) {
            byte[] recv = readCmd();
            if (recv != null) {
                if (recv[4] == SJA_OK) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean beginSet() {
        return doCmd(SET_CANBEGIN, 0);
    }

    private boolean setMode(int mode) {
        return doCmd(SET_CANMODE, mode);
    }

    private boolean setPrior(int prior) {
        return doCmd(SET_CANPRIOR, prior);
    }

    private boolean setChan(int chan) {
        return doCmd(SET_CANCHAN, chan);
    }

    private boolean endSet() {
        return doCmd(SET_CANEND, 0);
    }

    private boolean setBaud(CanbusBaud baud) {
        byte[] buff = new byte[6];
        buff[0] = 0x20 | 5;
        buff[1] = 0x55;
        buff[2] = (byte)(0xAA);
        buff[3] = (byte)(SET_CANBAUD);
        if (baud.equals(CanbusBaud.B5K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[0]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[1]);
        } else if (baud.equals(CanbusBaud.B10K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[2]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[3]);
        } else if (baud.equals(CanbusBaud.B20K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[4]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[5]);
        } else if (baud.equals(CanbusBaud.B40K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[6]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[7]);
        } else if (baud.equals(CanbusBaud.B50K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[8]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[9]);
        } else if (baud.equals(CanbusBaud.B80K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[10]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[11]);
        } else if (baud.equals(CanbusBaud.B100K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[12]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[13]);
        } else if (baud.equals(CanbusBaud.B125K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[14]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[15]);
        } else if (baud.equals(CanbusBaud.B200K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[16]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[17]);
        } else if (baud.equals(CanbusBaud.B250K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[18]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[19]);
        } else if (baud.equals(CanbusBaud.B400K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[20]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[21]);
        } else if (baud.equals(CanbusBaud.B500K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[22]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[23]);
        } else if (baud.equals(CanbusBaud.B666K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[24]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[25]);
        } else if (baud.equals(CanbusBaud.B800K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[26]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[27]);
        } else if (baud.equals(CanbusBaud.B1000K)) {
            buff[4] = (byte)(SJA_BTR_CODETABLE[28]);
            buff[5] = (byte)(SJA_BTR_CODETABLE[29]);
        }
        if (sendCmd(buff)) {
            byte[] recv = readCmd();
            if (recv != null) {
                if (recv[4] == SJA_OK) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean setAcr(int acr, int acr0, int acr1, int acr2, int acr3) {
        byte[] buff = new byte[9];
        buff[0] = 0x20 | 6;
        buff[1] = 0x55;
        buff[2] = (byte)(0xAA);
        buff[3] = (byte)(SET_CANACR);
        buff[4] = (byte)(acr);
        buff[5] = (byte)(acr0);
        buff[6] = (byte)(acr1);
        buff[7] = (byte)(acr2);
        buff[8] = (byte)(acr3);
        if (sendCmd(buff)) {
            byte[] recv = readCmd();
            if (recv != null) {
                if (recv[4] == SJA_OK) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean setAmr(int amr, int amr0, int amr1, int amr2, int amr3) {
        byte[] buff = new byte[9];

        buff[0] = 0x20 | 6;
        buff[1] = 0x55;
        buff[2] = (byte)(0xAA);
        buff[3] = (byte)(SET_CANAMR);
        buff[4] = (byte)(amr);
        buff[5] = (byte)(amr0);
        buff[6] = (byte)(amr1);
        buff[7] = (byte)(amr2);
        buff[8] = (byte)(amr3);

        if (sendCmd(buff)) {
            byte[] recv = readCmd();
            if (recv != null) {
                if (recv[4] == SJA_OK) {
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] readOneFrame(boolean isFinish) throws IOException {
        byte[] temp = new byte[3];
        if (_box.readBytes(temp, 0, 3) <= 0) {
            finishExecute(isFinish);
            throw new IOException();
        }

        int length = temp[0] & 0x0F;
        int mode = temp[0] & (CanbusIDMode.EXT.value() | CanbusFrameType.Remote.value());
        if ((mode == (CanbusIDMode.STD.value() | CanbusFrameType.Data.value())) || (mode == (CanbusIDMode.STD.value() | CanbusFrameType.Remote.value()))) {
            length += STD_FRAMEID_LENGTH;
        } else {
            length += EXT_FRAMEID_LENGTH;
        }
        
        byte[] result = new byte[length + 1];
        System.arraycopy(temp, 0, result, 0, 3);

        length -= 2;
        if (length <= 0) {
            finishExecute(isFinish);
            return null;
        }
        
        if (_box.readBytes(result, 3, length) <= 0) {
            finishExecute(isFinish);
            throw new IOException();
        }
        finishExecute(isFinish);
        return unpack(result);
    }
}
