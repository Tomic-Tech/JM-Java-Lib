package jm.device;

public enum ProtocolType {

    J1850VPW(0),
    J1850PWM(1),
    ISO9141_2(2),
    ISO14230(3),
    ISO15765(4),
    ALDL160(5),
    ALDL8192(6),
    KWP1281(7),
    MIKUNI(8);
    private int _value;

    private ProtocolType(int value) {
        this._value = value;
    }

    public int value() {
        return this._value;
    }
}