package jm.device;

public enum CommboxVersion {

    V1(0),
    ELM327(1),
    TL718(2);
    private int _value;

    private CommboxVersion(int value) {
        this._value = value;
    }

    public int value() {
        return this._value;
    }
}