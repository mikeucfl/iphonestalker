/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.util.io;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author MikeUCFL
 */
public class MBUtils {
    
    public static int offset = 0;
    
    public static String getString(DataInputStream dis) throws IOException {
        String value = null;
            
        int length = dis.readUnsignedShort();
        offset += 2;
        if (length > 0 && length < 65535) {
            byte[] data = new byte[length];

            dis.read(data);
            value = new String(data);
            offset += length;
        }
            
        return value;
    }
}
