/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jm.device.ConnectorType;

/**
 *
 * @author Ogilvy
 */
class KWP1281 extends jm.device.KWP1281 implements IProtocol {

    private byte[] _buffIdAddr = new byte[2]; // 0 == buffID, 1 == buffAddr
    private int _kLine;
    private int _lLine;
    private Box _box;
    private Shared _shared;

    public KWP1281(Box box, Shared shared) {
        _kLine = D.SK_NO;
        _lLine = D.RK_NO;
        _box = box;
        _shared = shared;
    }

    @Override
    public void finishExecute(boolean isFinish) {
        if (isFinish) {
            _box.delBatch(_shared.buffID);
            _box.checkResult(500000);
        }
    }

    private void sendOneFrame(byte[] data, boolean needRecv) throws IOException {
        _shared.buffID = 0;
        byte[] sendBuff = pack(data);
        if (sendBuff == null) {
            throw new IOException();
        }
        if (!_box.newBatch(_shared.buffID)) {
            throw new IOException();
        }
        if (needRecv) {
            if (!_box.turnOverOneByOne()
                    || !_box.sendOutData(0, sendBuff.length, sendBuff)
                    || !_box.updateBuff(D.INC_DATA, _buffIdAddr)
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_FR)
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_LEN_1)
                    || !_box.updateBuff(D.INC_DATA, _buffIdAddr) // Ending send data, begin receive data
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_FR)
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_LEN_1)
                    || !_box.endBatch()
                    || !_box.runBatch(false, _shared.buffID)) {
                throw new IOException();
            }
            try {
                Thread.sleep((sendBuff.length * _shared.reqByteToByte + _shared.reqWaitTime) / 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(KWP1281.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            if (!_box.turnOverOneByOne()
                    || !_box.sendOutData(0, sendBuff.length, sendBuff)
                    || !_box.updateBuff(D.INC_DATA, _buffIdAddr)
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_FR)
                    || !_box.turnOverOneByOne()
                    || !_box.runReceive(D.REC_LEN_1)
                    || !_box.updateBuff(D.INC_DATA, _buffIdAddr)
                    || !_box.endBatch()
                    || !_box.runBatch(false, _shared.buffID)) {
                throw new IOException();
            }
        }
        byte[] result = new byte[sendBuff.length - 1];
        if (_box.readBytes(result, 0, sendBuff.length - 1) <= 0) {
            throw new IOException();
        }
    }

    public byte[] readOneFrame(boolean isFinish) throws IOException {
        byte[] temp = new byte[1];
        if (_box.readBytes(temp, 0, 1) <= 0) {
            throw new IOException();
        }
        int length = temp[0] & 0xFF;
        byte[] buff = new byte[length + 1];
        System.arraycopy(buff, 0, temp, 0, 1);
        if (_box.readBytes(buff, 1, length) <= 0) {
            throw new IOException();
        }
        finishExecute(isFinish);
        return unpack(buff);
    }

    @Override
    public void sendOneFrame(byte[] data) throws IOException {
        sendOneFrame(data, false);
    }

    @Override
    public void sendFrames(byte[] data) throws IOException {
        sendOneFrame(data);
    }

    @Override
    public byte[] readOneFrame() throws IOException {
        return readOneFrame(true);
    }

    @Override
    public byte[] readFrames() throws IOException {
        return readOneFrame();
    }

    @Override
    public void addrInit(int addrCode) throws IOException {
        boolean dl0;
        byte[] temp = new byte[2];
        _shared.buffID = 0;
        _box.stopNow(true);
        _box.checkResult(50000);

        dl0 = (_lLine == D.SK_NO && _kLine == D.RK_NO) ? true : false;

        if (!_box.setCommCtrl(D.PWC | D.REFC | D.RZFC | (dl0 ? D.DLC0 : D.CK), D.SET_NULL)
                || !_box.setCommLine(_lLine, _kLine)
                || !_box.setCommLink(D.RS_232 | D.BIT9_MARK | (dl0 ? D.SEL_DL0 : D.SEL_SL) | D.SET_DB20, 0xFF, D.INVERTBYTE | 1)
                || !_box.setCommBaud(5)
                || !_box.setCommTime(D.SETBYTETIME, 0)
                || !_box.setCommTime(D.SETWAITTIME, 25000)
                || !_box.setCommTime(D.SETRECBBOUT, 610000)
                || !_box.setCommTime(D.SETRECFROUT, 610000)
                || !_box.setCommTime(D.SETLINKTIME, 710000)) {
            throw new IOException();
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            Logger.getLogger(KWP1281.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (_box.newBatch(_shared.buffID)) {
            throw new IOException();
        }

        if (!_box.sendOutData(0, 1, (byte)addrCode)
                || !_box.setCommLine(_kLine == D.RK1 ? D.SK_NO : _lLine, _kLine)
                || !_box.runReceive(_box.getD().SET55_BAUD)
                || !_box.runReceive(D.REC_LEN_1)
                || !_box.turnOverOneByOne()
                || !_box.runReceive(D.REC_LEN_1)
                || !_box.setCommTime(D.SETBYTETIME, 2000)
                || !_box.setCommTime(D.SETWAITTIME, 2000)
                || !_box.runReceive(D.REC_FR)
                || !_box.turnOverOneByOne()
                || !_box.runReceive(D.REC_LEN_1)
                || !_box.endBatch()) {
            _box.delBatch(_shared.buffID);
            throw new IOException();
        }
        if (!_box.runBatch(false, _shared.buffID)) {
            _box.delBatch(_shared.buffID);
            throw new IOException();
        }
        
        temp = new byte[2];
        if (_box.readData(temp, 0, 2, 3500000) <= 0) {
            _box.delBatch(_shared.buffID);
            throw new IOException();
        }
        
        temp = readOneFrame();
        if (!_box.checkResult(500000)) {
            _box.delBatch(_shared.buffID);
            throw new IOException();
        }
        
        if (!_box.delBatch(_shared.buffID)) {
            throw new IOException();
        }
        
        _frameCounter = temp[0];
        
    }
    
    @Override
    public void setKeepLink(byte[] data) throws IOException {
        int add1 = 0;
        int add2 = 0;
        byte[] tempBuff = new byte[3];
        
        _buffIdAddr[0] = new Integer(D.LINKBLOCK).byteValue();
        if (!_box.newBatch(D.LINKBLOCK)) {
            throw new IOException();
        }
        if (!_box.turnOverOneByOne()
                || !_box.sendOutData(0, data.length, data)) {
            throw new IOException();
        }
        _buffIdAddr[1] = new Integer((data[0] & 0xFF) - 2).byteValue();
        if (!_box.turnOverOneByOne()
                || !_box.sendOutData(0, 1, data[0])
                || !_box.updateBuff(D.INC_DATA, _buffIdAddr)
                || ((add1 = _shared.nextAddress) == 0)
                || !_box.turnOverOneByOne()
                || !_box.runReceive(D.REC_FR)
                || !_box.turnOverOneByOne()
                || !_box.runReceive(D.REC_LEN_1)
                || !_box.updateBuff(D.INC_DATA, _buffIdAddr)
                || ((add2 = _shared.nextAddress) == 0)
                || !_box.endBatch()) {
            throw new IOException();
        }
        
        tempBuff[0] = new Integer(D.LINKBLOCK).byteValue();
        tempBuff[1] = new Integer(add1).byteValue();
        tempBuff[2] = _buffIdAddr[1];
        if (_box.getAbsAdd(D.LINKBLOCK, tempBuff[2]) == 0) {
            throw new IOException();
        }
        if (!_box.updateBuff(D.UPDATE_1BYTE, tempBuff)) {
            throw new IOException();
        }
        tempBuff[1] = new Integer(add2).byteValue();
        if (!_box.updateBuff(D.UPDATE_1BYTE, tempBuff)) {
            throw new IOException();
        }
    }

    @Override
    public void setLines(int comLine, boolean lLine) throws IllegalArgumentException {
        if (_shared.connector.equals(ConnectorType.AUDI_4)
                || _shared.connector.equals(ConnectorType.OBDII_16)) {
            if (comLine != 0) {
                _kLine = D.RK1;
            } else {
                _kLine = D.RK_NO;
            }
            if (lLine) {
                _lLine = D.SK2;
            } else {
                _lLine = D.SK_NO;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
