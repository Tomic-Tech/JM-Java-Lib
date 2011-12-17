package jm.device;

public enum CanbusIDMode {

    STD(0x00),
    EXT(0x80);
    private int _value;

    private CanbusIDMode(int value) {
        this._value = value;
    }

    public int value() {
        return this._value;
    }
}