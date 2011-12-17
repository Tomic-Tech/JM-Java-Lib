/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device.v1;

import java.io.IOException;

/**
 *
 * @author Ogilvy
 */
public interface IProtocol {
    void sendOneFrame(byte[] data) throws IOException;
    void sendFrames(byte[] data) throws IOException;
    byte[] readOneFrame() throws IOException;
    byte[] readFrames() throws IOException;
    void finishExecute(boolean isFinish);
    void setKeepLink(byte[] data) throws IOException;
}
