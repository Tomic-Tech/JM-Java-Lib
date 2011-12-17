package jm.device;

public enum CanbusFilterMask {

    Enable(0x0F),
    Disable(0x00);
    private int _value;

    private CanbusFilterMask(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }
}