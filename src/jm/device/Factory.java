/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.device;

import java.util.EnumMap;
import jm.io.IPort;

/**
 *
 * @author Ogilvy
 */
public class Factory {
    static private EnumMap<CommboxVersion, Commbox> _objMap = null;
    static {
        _objMap = new EnumMap<>(CommboxVersion.class);
    }
    
    public static Commbox create(CommboxVersion ver, IPort port) {
        Commbox box = null;
        if (ver.equals(CommboxVersion.V1)) {
            if (_objMap.containsKey(ver)) {
                box = _objMap.get(ver);
            } else {
                box = new jm.device.v1.Commbox();
                _objMap.put(ver, box);
            }
            box.setPort(port);
        }
        return box;
    }
}
