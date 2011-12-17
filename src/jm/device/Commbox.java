package jm.device;

import java.io.IOException;

import jm.io.IPort;

public abstract class Commbox {

    private IPort _port = null;

    public void setPort(IPort port) {
        _port = port;
    }

    public IPort getPort() {
        return _port;
    }

    abstract public void open() throws IOException;

    abstract public void close() throws IOException;

    abstract public IComm<?> configure(ProtocolType type) throws IOException;

    abstract public void setConnector(ConnectorType type) throws IOException;
}