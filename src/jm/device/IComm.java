package jm.device;

import java.io.IOException;

public interface IComm<P> {

    P getProtocol();

    void sendOneFrame(byte[] data) throws IOException;

    void sendFrames(byte[] data) throws IOException;

    byte[] readOneFrame() throws IOException;

    byte[] readFrames() throws IOException;

    byte[] sendAndRecv(byte[] data) throws IOException;

    void startKeepLink(boolean run) throws IOException;

    void setKeepLink(byte[] data) throws IOException;

    void setTimeouts(int txB2B, int rxB2B, int txF2F, int rxF2F, int total)
            throws IOException;
}