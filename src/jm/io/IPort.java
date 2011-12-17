package jm.io;

import java.io.IOException;

public interface IPort {

    public int bytesAvailable() throws IOException;

    public void setReadTimeout(long millicSeconds) throws IOException;

    public void setWriteTimeout(long millicSeconds) throws IOException;

    public int read(byte[] buff, int offset, int count) throws IOException;

    public int read(byte[] buff, int offset, int count, long millicSeconds) throws IOException;

    public int readByte() throws IOException;

    public int readByte(long millicSeconds) throws IOException;

    public int write(byte[] data, int offset, int count) throws IOException;

    public void discardInBuffer() throws IOException;

    public void discardOutBuffer() throws IOException;
}
