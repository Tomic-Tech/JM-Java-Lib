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
            target = new byte[KWP8XHeaderLength + KWPChecksumLength
                    + data.length];
            target[0] = new Integer(0x80 | data.length).byteValue();
            target[1] = new Integer(_targetAddress).byteValue();
            target[2] = new Integer(_sourceAddress).byteValue();
            for (int i = 0; i < data.length; ++i) {
                target[KWP8XHeaderLength + i] = data[i];
            }
        } else if (_mode.compareTo(KWPMode.KWPCX) == 0) {
            target = new byte[KWPCXHeaderLength + KWPChecksumLength
                    + data.length];
            target[0] = new Integer(0xC0 | data.length).byteValue();
            target[1] = new Integer(_targetAddress).byteValue();
            target[2] = new Integer(_sourceAddress).byteValue();
            for (int i = 0; i < data.length; ++i) {
                target[KWPCXHeaderLength + i] = data[i];
            }
        } else if (_mode.compareTo(KWPMode.KWP80) == 0) {
            target = new byte[KWP80HeaderLength + KWPChecksumLength
                    + data.length];
            target[0] = new Integer(0x80).byteValue();
            target[1] = new Integer(_targetAddress).byteValue();
            target[2] = new Integer(_sourceAddress).byteValue();
            target[3] = new Integer(data.length).byteValue();
            for (int i = 0; i < data.length; ++i) {
                target[KWP80HeaderLength + i] = data[i];
            }
        } else if (_mode.compareTo(KWPMode.KWP00) == 0) {
            target = new byte[KWP00HeaderLength + KWPChecksumLength
                    + data.length];
            target[0] = 0x00;
            target[1] = new Integer(data.length).byteValue();
            for (int i = 0; i < data.length; ++i) {
                target[KWP00HeaderLength + i] = data[i];
            }
        } else if (_mode.compareTo(KWPMode.KWPXX) == 0) {
            target = new byte[KWPXXHeaderLength + KWPChecksumLength
                    + data.length];
            target[0] = new Integer(data.length).byteValue();
            for (int i = 0; i < data.length; ++i) {
                target[KWPXXHeaderLength + i] = data[i];
            }
        } else {
            return target;
        }

        target[target.length - 1] = 0;
        for (int i = 0; i < target.length - 1; ++i) {
            target[target.length - 1] = new Integer(target[target.length - 1]
                    + target[i]).byteValue();
        }
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
            for (int i = 0; i < length; ++i) {
                target[i] = data[KWP8XHeaderLength + i];
            }
        } else if ((data[0] & 0xFF) == 0x80) {
            length = data[3] & 0xFF;
            if ((data[1] & 0xFF) != _sourceAddress) {
                throw new IllegalArgumentException();
            }
            if (length != (data.length - KWP80HeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            for (int i = 0; i < length; ++i) {
                target[i] = data[KWP80HeaderLength + i];
            }
        } else if ((data[0] & 0xFF) == 0) {
            length = data[0] & 0xFF;
            if (length != (data.length - KWP00HeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            for (int i = 0; i < length; ++i) {
                target[i] = data[KWP00HeaderLength + i];
            }
        } else {
            length = data[0] & 0xFF;
            if (length != (data.length - KWPXXHeaderLength - KWPChecksumLength)) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            for (int i = 0; i < length; ++i) {
                target[i] = data[KWPXXHeaderLength + i];
            }
        }
        return target;
    }

    public abstract void fastInit(byte[] data) throws IOException;
}