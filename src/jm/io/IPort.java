package jm.io;

import java.io.IOException;

public interface IPort {

    public long bytesAvailable() throws IOException;

    public void setReadTimeout(long millicSeconds) throws IOException;

    public void setWriteTimeout(long millicSeconds) throws IOException;

    public long read(byte[] buff, long offset, long count) throws IOException;

    public long read(byte[] buff, long offset, long count, long millicSeconds) throws IOException;

    public int readByte() throws IOException;

    public int readByte(long millicSeconds) throws IOException;

    public long write(byte[] data, long offset, long count) throws IOException;

    public void discardInBuffer() throws IOException;

    public void discardOutBuffer() throws IOException;
}
