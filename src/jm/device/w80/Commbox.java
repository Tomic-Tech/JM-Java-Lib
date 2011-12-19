/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.w80;

import java.io.IOException;
import jm.device.ConnectorType;
import jm.device.IComm;
import jm.device.ProtocolType;

/**
 *
 * @author Ogilvy
 */
public class Commbox extends jm.device.Commbox {
    private Shared _shared;
    private Box _box;
    
    public Commbox() {
    }
    
    @Override
    public void open() throws IOException {
        _shared = new Shared();
        _box = new Box(getPort(), _shared);
        if (!_box.openComm())
            throw new IOException();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IComm<?> configure(ProtocolType type) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setConnector(ConnectorType type) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
