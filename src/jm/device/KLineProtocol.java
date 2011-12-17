package jm.device;

import java.io.IOException;

public abstract class KLineProtocol implements IProtocol {

    protected int _targetAddress = 0;
    protected int _sourceAddress = 0;

    public void setAddress(int targetAddress, int sourceAddress) {
        _targetAddress = targetAddress;
        _sourceAddress = sourceAddress;
    }

    public abstract void addrInit(int addrCode) throws IOException;

    public abstract void setLines(int comLine, boolean lLine)
            throws IllegalArgumentException;
}