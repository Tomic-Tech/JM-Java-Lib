package jm.io;

import java.io.IOException;

public final class SerialPort implements IPort {

    static {
        System.loadLibrary("jmlib");
    }

    public enum Parity {

        None(0), Odd(1), Even(2), Mark(3), Space(4);
        private int _value;

        private Parity(int value) {
            _value = value;
        }

        public int value() {
            return _value;
        }
    }

    public enum Stopbits {

        One(0), OnePointFive(1), Two(2);
        private int _value;

        private Stopbits(int value) {
            _value = value;
        }

        public int value() {
            return _value;
        }
    }

    public enum FlowControl {

        None(0), Hardware(1), Software(2);
        private int _value;

        private FlowControl(int value) {
            _value = value;
        }

        public int value() {
            return _value;
        }
    }
    private long _handle;

    private native long nativeNewSerialPort();

    private native void nativeFree(long handle);

    public SerialPort() {
        _handle = nativeNewSerialPort();
    }

    public SerialPort(String portName) throws IOException {
        _handle = nativeNewSerialPort();
        setPortName(portName);
    }

    public SerialPort(String portName, int baudrate, byte databits,
            Parity parity, Stopbits stopbits, FlowControl flowControl)
            throws IOException {
        _handle = nativeNewSerialPort();
        setPortName(portName);
        setBaudrate(baudrate);
        setDatabits(databits);
        setParity(parity);
        setStopbits(stopbits);
        setFlowControl(flowControl);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeFree(_handle);
    }

    private static native int nativeSetPortName(long handle, String name);

    public void setPortName(String name) throws IOException {
        int ret = nativeSetPortName(_handle, name);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private static native String nativePortName(long handle);

    public String portName() throws IOException {
        String name = nativePortName(_handle);
        if (name == null) {
            throw new IOException();
        }
        return name;
    }

    private static native int nativeSetBaudrate(long handle, int baudrate);

    public void setBaudrate(int baudrate) throws IOException {
        int ret = nativeSetBaudrate(_handle, baudrate);
        if (ret == 0) {
            throw new IOException();
        }
    }

    private static native int nativeBaudrate(long handle);

    public int baudrate() throws IOException {
        int ret = nativeBaudrate(_handle);
        if (ret <= 0) {
            throw new IOException();
        }
        return ret;
    }

    private static native int nativeSetDatabits(long handle, byte bits);

    public void setDatabits(byte bits) throws IOException {
        int ret = nativeSetDatabits(_handle, bits);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private static native int nativeDatabits(long handle);

    public int databits() throws IOException {
        int ret = nativeDatabits(_handle);
        if (ret <= 0) {
            throw new IOException();
        }
        return ret;
    }

    private static native int nativeSetParity(long handle, int parity);

    public void setParity(Parity parity) throws IOException {
        int ret = nativeSetParity(_handle, parity.value());
        if (ret != 0) {
            throw new IOException();
        }
    }

    private static native int nativeParity(long handle);

    public Parity parity() throws IOException {
        int ret = nativeParity(_handle);
        if (ret == Parity.Even.value()) {
            return Parity.Even;
        } else if (ret == Parity.Mark.value()) {
            return Parity.Mark;
        } else if (ret == Parity.None.value()) {
            return Parity.None;
        } else if (ret == Parity.Odd.value()) {
            return Parity.Odd;
        } else if (ret == Parity.Space.value()) {
            return Parity.Space;
        } else {
            throw new IOException();
        }
    }

    private native int nativeSetStopbits(long handle, int stopbits);

    public void setStopbits(Stopbits stopbits) throws IOException {
        int ret = nativeSetStopbits(_handle, stopbits.value());
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeStopbits(long handle);

    public Stopbits stopbits() throws IOException {
        int ret = nativeStopbits(_handle);
        if (ret == Stopbits.One.value()) {
            return Stopbits.One;
        } else if (ret == Stopbits.OnePointFive.value()) {
            return Stopbits.OnePointFive;
        } else if (ret == Stopbits.Two.value()) {
            return Stopbits.Two;
        } else {
            throw new IOException();
        }
    }

    private native static int nativeSetFlowControl(long handle, int flow);

    public void setFlowControl(FlowControl flowControl) throws IOException {
        int ret = nativeSetFlowControl(_handle, flowControl.value());
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native static int nativeFlowControl(long handle);

    public FlowControl flowControl() throws IOException {
        int ret = nativeFlowControl(_handle);
        if (ret == FlowControl.Hardware.value()) {
            return FlowControl.Hardware;
        } else if (ret == FlowControl.None.value()) {
            return FlowControl.None;
        } else if (ret == FlowControl.Software.value()) {
            return FlowControl.Software;
        } else {
            throw new IOException();
        }
    }

    private native int nativeSetReadTimeout(long handle, long millicSeconds);

    @Override
    public void setReadTimeout(long millicSeconds) throws IOException {
        int ret = nativeSetReadTimeout(_handle, millicSeconds);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeSetWriteTimeout(long handle, long millicSeconds);

    @Override
    public void setWriteTimeout(long millicSeconds) throws IOException {
        int ret = nativeSetWriteTimeout(_handle, millicSeconds);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native boolean nativeIsOpen(long handle);

    public boolean isOpen() throws IOException {
        return nativeIsOpen(_handle);
    }

    private native int nativeOpen(long handle);

    public void open() throws IOException {
        int ret = nativeOpen(_handle);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeClose(long handle);

    public void close() throws IOException {
        int ret = nativeClose(_handle);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeFlush(long handle);

    public void flush() throws IOException {
        int ret = nativeFlush(_handle);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeDiscardInBuffer(long handle);

    @Override
    public void discardInBuffer() throws IOException {
        int ret = nativeDiscardInBuffer(_handle);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeDiscardOutBuffer(long handle);

    @Override
    public void discardOutBuffer() throws IOException {
        int ret = nativeDiscardOutBuffer(_handle);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeBytesAvailable(long handle);

    @Override
    public int bytesAvailable() throws IOException {
        return nativeBytesAvailable(_handle);
    }

    private native int nativeSetDtr(long handle, boolean set);

    public void setDtr(boolean set) throws IOException {
        int ret = nativeSetDtr(_handle, set);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeSetRts(long handle, boolean set);

    public void setRts(boolean set) throws IOException {
        int ret = nativeSetRts(_handle, set);
        if (ret != 0) {
            throw new IOException();
        }
    }

    private native int nativeRead(long handle, byte[] buff, int offset, int count);

    @Override
    public int read(byte[] buff, int offset, int count) throws IOException {
        int ret = nativeRead(_handle, buff, offset, count);
        if (ret != count) {
            throw new IOException();
        }
        return ret;
    }

    @Override
    public int read(byte[] buff, int offset, int count, long millicSeconds) throws IOException {
        setReadTimeout(millicSeconds);
        return read(buff, offset, count);
    }

    public byte[] read() throws IOException {
        byte[] buff = null;
        long bytesToRead = bytesAvailable();
        if (bytesToRead > 0) {
            buff = new byte[(int) bytesToRead];
            read(buff, 0, (int)bytesToRead);
        }
        return buff;
    }

    @Override
    public int readByte() throws IOException {
        byte[] buff = new byte[1];
        read(buff, 0, 1);
        return (int) buff[0];
    }

    @Override
    public int readByte(long millicSeconds) throws IOException {
        byte[] buff = new byte[1];
        read(buff, 0, 1, millicSeconds);
        return (int) buff[0];
    }

    private native int nativeWrite(long handle, byte[] data, int offset, int count);

    @Override
    public int write(byte[] data, int offset, int count) throws IOException {
        int ret = nativeWrite(_handle, data, offset, count);
        if (ret != data.length) {
            throw new IOException();
        }
        return ret;
    }

    public native static String[] getSystemPorts();
}
