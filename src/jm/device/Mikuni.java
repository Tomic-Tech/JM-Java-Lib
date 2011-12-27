package jm.device;

public abstract class Mikuni extends KLineProtocol {

    public final int HeadFormat = 0x48;

    @Override
    public byte[] pack(byte[] data) throws IllegalArgumentException {
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] target = new byte[data.length + 3];
        target[0] = (byte)(HeadFormat);
        System.arraycopy(data, 0, target, 1, data.length);
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
        System.arraycopy(data, 1, target, 0, data.length - 2);
        return target;
    }
}