package jm.device;

public enum ConnectorType {

    OBDII_16(0),
    UNIVERSAL_3(1),
    BENZ_38(2),
    BMW_20(3),
    AUDI_4(4),
    FIAT_3(5),
    CITROEN_2(6),
    CHRYSLER_6(7),
    TOYOTA_17R(8),
    TOYOTA_17F(9),
    HONDA_3(10),
    MITSUBISHI(11),
    HYUNDAI(12),
    NISSAN(13),
    SUZUKI_3(14),
    DAIHATSU_4(15),
    ISUZU_3(16),
    CANBUS_16(17),
    GM_12(18),
    KIA_20(19),
    NISSAN_14(20);
    private int _value;

    private ConnectorType(int value) {
        this._value = value;
    }

    public int value() {
        return this._value;
    }
}