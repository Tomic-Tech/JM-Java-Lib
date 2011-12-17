package jm.device;

public enum CanbusFrameType {

    Data(0x00),
    Remote(0x40);
    private int _value;

    private CanbusFrameType(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }
}