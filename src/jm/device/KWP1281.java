package jm.device;

public abstract class KWP1281 extends KLineProtocol {

    public final byte FrameEnd = 0x03;
    protected int _frameCounter = 0;

    @Override
    public byte[] pack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] ret = new byte[3 + data.length];
        ret[0] = new Integer(data.length + 20).byteValue();
        ret[1] = frameCounterIncrement();
        for (int i = 0; i < data.length; ++i) {
            ret[i + 2] = data[i];
        }
        ret[data.length + 2] = FrameEnd;
        return ret;
    }

    @Override
    public byte[] unpack(byte[] data) throws IllegalArgumentException {
        if (data == null || (data.length - 2 <= 0)) {
            throw new IllegalArgumentException();
        }

        byte[] ret = new byte[data.length - 2];
        for (int i = 0; i < data.length - 2; ++i) {
            ret[i] = data[i + 1];
        }
        return ret;
    }

    protected byte frameCounterIncrement() {
        _frameCounter = (_frameCounter + 1) & 0xFF;
        return new Integer(_frameCounter).byteValue();
    }
}
