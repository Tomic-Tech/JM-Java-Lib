package jm.device;

public interface IProtocol {

    byte[] pack(byte[] data) throws IllegalArgumentException;

    byte[] unpack(byte[] data) throws IllegalArgumentException;
}
