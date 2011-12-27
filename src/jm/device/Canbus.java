package jm.device;

public abstract class Canbus implements IProtocol {

    protected int _targetID = 0;
    protected CanbusBaud _baud = CanbusBaud.B500K;
    protected CanbusIDMode _idMode = CanbusIDMode.STD;
    protected CanbusFilterMask _mask = CanbusFilterMask.Disable;
    protected CanbusFrameType _frameType = CanbusFrameType.Data;
    protected int _high = 0;
    protected int _low = 0;
    protected int[] _idArray = null;
    protected byte[] _flowControl = new byte[8];

    public Canbus() {
        _flowControl[0] = 0x30;
        _flowControl[1] = 0x00;
        _flowControl[2] = 0x00;
        _flowControl[3] = 0x00;
        _flowControl[4] = 0x00;
        _flowControl[5] = 0x00;
        _flowControl[6] = 0x00;
        _flowControl[7] = 0x00;
    }

    @Override
    public byte[] pack(byte[] data) throws IllegalArgumentException {
        byte[] target = null;
        if (data == null || data.length > 8 || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        if (_idMode.value() == CanbusIDMode.STD.value()) {
            target = new byte[3 + data.length];
            target[1] = (byte)(_targetID >> 8);
            target[2] = (byte)(_targetID);
            if (_frameType.value() == CanbusFrameType.Data.value()) {
                target[0] = (byte)(data.length | CanbusIDMode.STD.value() | CanbusFrameType.Data.value());
            } else {
                target[0] = (byte)(data.length | CanbusIDMode.STD.value() | CanbusFrameType.Remote.value());
            }
            System.arraycopy(data, 0, target, 3, data.length);
            return target;
        } else if (_idMode.value() == CanbusIDMode.EXT.value()) {
            target = new byte[5 + data.length];
            target[1] = (byte)(_targetID >> 24);
            target[2] = (byte)(_targetID >> 16);
            target[3] = (byte)(_targetID >> 8);
            target[4] = (byte)(_targetID);
            if (_frameType.value() == CanbusFrameType.Data.value()) {
                target[0] = (byte)(data.length | CanbusIDMode.EXT.value() | CanbusFrameType.Data.value());
            } else {
                target[0] = (byte)(data.length | CanbusIDMode.EXT.value() | CanbusFrameType.Remote.value());
            }
            System.arraycopy(data, 0, target, 5, data.length);
            return target;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public byte[] unpack(byte[] data) throws IllegalArgumentException {
        byte[] target = null;
        int length = 0;
        if (data == null || data.length <= 0) {
            throw new IllegalArgumentException();
        }

        int mode = (data[0] & 0xFF) & (CanbusIDMode.EXT.value() | CanbusFrameType.Remote.value());
        if (mode == (CanbusIDMode.STD.value() | CanbusFrameType.Data.value())) {
            length = data[0] & 0x0F;
            if (length != data.length - 3) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, 3, target, 0, length);
        } else if (mode == (CanbusIDMode.STD.value() | CanbusFrameType.Remote.value())) {
            return target;
        } else if (mode == (CanbusIDMode.EXT.value() | CanbusFrameType.Data.value())) {
            length = data[0] & 0x0F;
            if (length != data.length - 5) {
                throw new IllegalArgumentException();
            }
            target = new byte[length];
            System.arraycopy(data, 5, target, 0, length);
        } else {
            return target;
        }
        return target;
    }

    abstract public void setLines(int high, int low) throws IllegalArgumentException;

    abstract public void setFilter(int... idArray) throws IllegalArgumentException;

    abstract public void setOptions(int id, CanbusBaud baud, CanbusIDMode idMode,
            CanbusFilterMask mask, CanbusFrameType frame)
            throws IllegalArgumentException;
}
