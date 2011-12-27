package jm.device;

import java.io.IOException;

public abstract class KWP2000 extends KLineProtocol {

    public final int KWP8XHeaderLength = 3;
    public final int KWPCXHeaderLength = 3;
    public final int KWP80HeaderLength = 4;
    public final int KWPXXHeaderLength = 1;
    public final int KWP00HeaderLength = 2;
    public final int KWPChecksumLength = 1;
    public final int KWPMaxDataSize = 128;
    protected KWPMode _mode;
    protected KWPMode _msgMode;
    protected KWPMode _linkMode;
    protected int _baud;

    public KWP2000() {
        _mode = KWPMode.KWP8X;
        _msgMode = KWPMode.KWP8X;
        _linkMode = KWPMode.KWP8X;
        _baud = 0;
    }

    void setOptions(KWPMode msgMode, KWPMode linkMode, int baud) {
        _msgMode = msgMode;
        _linkMode = linkMode;
        _mode = msgMode;
        _baud = baud;
    }

    @Override
    public byte[] pack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] target = null;
        if (_mode.compareTo(KWPMode.KWP8X) == 0) {
            target = new byte[KWP8XHeaderLength + KWPChecksumLength + data.length];
            target[0] = (byte) (0x80 | data.length);
            target[1] = (byte) (_targetAddress);
            target[2] = (byte) (_sourceAddress);
            System.arraycopy(data, 0, target, KWP8XHeaderLength, data.length);
        } else if (_mode.compareTo(KWPMode.KWPCX) == 0) {
            target = new byte[KWPCXHeaderLength + KWPChecksumLength + data.length];
            target[0] = (byte) (0xC0 | data.length);
            target[1] = (byte) (_targetAddress);
            target[2] = (byte) (_sourceAddress);
            System.arraycopy(data, 0, target, KWPCXHeaderLength, data.length);
        } else if (_mode.compareTo(KWPMode.KWP80) == 0) {
            target = new byte[KWP80HeaderLength + KWPChecksumLength + data.length];
            target[0] = (byte) (0x80);
            target[1] = (byte) (_targetAddress);
            target[2] = (byte) (_sourceAddress);
            target[3] = (byte) (data.length);
            System.arraycopy(data, 0, target, KWP80HeaderLength, data.length);
        } else if (_mode.compareTo(KWPMode.KWP00) == 0) {
            target = new byte[KWP00HeaderLength + KWPChecksumLength + data.length];
            target[0] = 0x00;
            target[1] = (byte) (data.length);
            System.arraycopy(data, 0, target, KWP00HeaderLength, data.length);
        } else if (_mode.compareTo(KWPMode.KWPXX) == 0) {
            target = new byte[KWPXXHeaderLength + KWPChecksumLength + data.length];
            target[0] = (byte) (data.length);
            System.arraycopy(data, 0, target, KWPXXHeaderLength, data.length);
        } else {
            return target;
        }
        
        int checksum = 0;
        for (int i = 0; i < target.length - 1; ++i) {
            checksum += target[i] & 0xFF;
        }
        target[target.length - 1] = (byte)checksum;
        return target;
    }

    @Override
    public byte[] unpack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }
        byte[] target = null;
        int length = 0;
        if ((data[0] & 0xFF) > 0x80) {
            length = (data[0] & 0xFF) - 0x80;
            if ((data[1] & 0xFF) != _sourceAddress) {
                throw new IllegalArgumentException();
            }
            if (length != (data.length - KWP8XHeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, KWP8XHeaderLength, target, 0, length);
        } else if ((data[0] & 0xFF) == 0x80) {
            length = data[3] & 0xFF;
            if ((data[1] & 0xFF) != _sourceAddress) {
                throw new IllegalArgumentException();
            }
            if (length != (data.length - KWP80HeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, KWP80HeaderLength, target, 0, length);
        } else if ((data[0] & 0xFF) == 0) {
            length = data[0] & 0xFF;
            if (length != (data.length - KWP00HeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, KWP00HeaderLength, target, 0, length);
        } else {
            length = data[0] & 0xFF;
            if (length != (data.length - KWPXXHeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, KWPXXHeaderLength, target, 0, length);
        }
        return target;
    }

    public abstract void fastInit(byte[] data) throws IOException;
}