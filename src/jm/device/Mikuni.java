package jm.device;

public abstract class Mikuni extends KLineProtocol {

    public final int HeadFormat = 0x48;

    @Override
    public byte[] pack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] target = new byte[data.length + 3];
        target[0] = new Integer(HeadFormat).byteValue();
        for (int i = 0; i < data.length; ++i) {
            target[i + 1] = data[i];
        }
        target[target.length - 2] = 0x0D;
        target[target.length - 1] = 0x0A;
        return target;
    }

    @Override
    public byte[] unpack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }
        byte[] target = new byte[data.length - 3];
        for (int i = 1; i < data.length - 2; ++i) {
            target[i - 1] = data[i];
        }
        return target;
    }
}