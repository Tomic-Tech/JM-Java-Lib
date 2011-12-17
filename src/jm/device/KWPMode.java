package jm.device;

public enum KWPMode {

    KWP8X(0),
    KWP80(1),
    KWPXX(2),
    KWP00(3),
    KWPCX(4);
    private int _value;

    private KWPMode(int value) {
        _value = value;
    }

    public int value() {
        return _value;
    }
}