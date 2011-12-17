/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.core;

/**
 *
 * @author Ogilvy
 */
public class utils {
    public static byte highByte(short value) {
        return new Integer((value >> 8)).byteValue();
    }
    
    public static byte lowByte(short value) {
        return new Integer(value).byteValue();
    }
    
    public static short highWord(int value) {
        return new Integer(value >> 16).shortValue();
    }
    
    public static short lowWord(int value) {
        return new Integer(value).shortValue();
    }
}
