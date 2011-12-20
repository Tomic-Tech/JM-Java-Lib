/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1;

import java.io.IOException;
import jm.device.IComm;

/**
 *
 * @author Ogilvy
 */
class Comm<P extends IProtocol & jm.device.IProtocol> implements IComm<P> {

    private P _protocol;
    private Box _box;

    public Comm(Box box, P protocol) {
        _protocol = protocol;
        _box = box;
    }

    @Override
    public void sendOneFrame(byte[] data) throws IOException {
        _protocol.sendOneFrame(data);
    }

    @Override
    public void sendFrames(byte[] data) throws IOException {
        _protocol.sendFrames(data);
    }

    @Override
    public byte[] readOneFrame() throws IOException {
        return _protocol.readOneFrame();
    }

    @Override
    public byte[] readFrames() throws IOException {
        return _protocol.readFrames();
    }

    @Override
    public byte[] sendAndRecv(byte[] data) throws IOException {
        int times = D.REPLAYTIMES;
        while (times-- != 0) {
            try {
                sendFrames(data);
                return readFrames();
            } catch (IOException e) {
                if (times != 0) {
                    continue;
                }
                _protocol.finishExecute(true);
                throw e;
            }
        }
        return null;
    }

    @Override
    public void setTimeouts(int txB2B, int rxB2B, int txF2F, int rxF2F, int total)
            throws IOException {
    }

    @Override
    public void startKeepLink(boolean run) throws IOException {
        if (!_box.keepLink(run)) {
            throw new IOException();
        }
    }
    
    @Override
    public void setKeepLink(byte[] data) throws IOException {
        _protocol.setKeepLink(data);
    }

    @Override
    public P getProtocol() {
        return _protocol;
    }
}
