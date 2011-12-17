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
class KWP1281Comm extends Comm<KWP1281>{
    public KWP1281Comm(Box box, KWP1281 protocol) {
        super(box, protocol);
    }
    @Override
    public byte[] sendAndRecv(byte[] data) throws IOException {
        getProtocol().sendOneFrame(data);
        return getProtocol().readOneFrame(true);
    }
}
