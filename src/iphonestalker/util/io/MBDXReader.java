/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.util.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MikeUCFL
 */
public class MBDXReader {
    private static final Logger logger = 
            Logger.getLogger(MBDXReader.class.getName());
    private Map<Integer, MBDXData> mbdxMap = null;
    
    public MBDXReader() {
        mbdxMap = new HashMap<Integer, MBDXData>();
    }
        
    public boolean processMbdx (File file) {
        // Get the file input stream
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            //logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        // Set up the data input stream for reading
        DataInputStream dis = new DataInputStream(fis);

        try {
            
            // Check the tag
            byte[] tag = new byte[4];
            dis.read(tag);
            
            if (new String(tag).equals("mbdx")) {
                dis.skipBytes(2); // 0x02 0x00, not sure what this is
                
                int fileCount = dis.readInt();
                
                while (dis.available() > 0) {
                    
                    MBDXData mbdxData = new MBDXData(dis);
                    mbdxMap.put(mbdxData.mbdbOffset, mbdxData);
                    
                    //if (mbdxData.fileId.equals("4096c9ec676f2847dc283405900e284a7c815836")) {
                    //    System.out.println(mbdxData);
                   // }
                }
            } else {
                logger.log(Level.ALL, "{0} is not a MBDX file!", file);
                return false;
            }
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
    
    public MBDXData getMBDXData (Integer offset) {
        return mbdxMap.get(offset);
    }
    
    public class MBDXData {
        public String fileId = null;
        public int mbdbOffset = -1;
        public int mode = -1;

        public MBDXData (DataInputStream dis) throws IOException {
            
            byte[] data = new byte[20];
            dis.read(data);
            if (data.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (byte b : data) {
                    String hex = Integer.toHexString(b & 0xFF);
                    if (hex.length() == 1) {
                        hex = "0" + hex;
                    }
                    sb.append(hex);
                }
                fileId = sb.toString();
            }
            
            mbdbOffset = dis.readInt();
            mbdbOffset += 6;// Add 6 to get past prolog
            mode = dis.readUnsignedShort();
            
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("fileID: " );
            sb.append(fileId);
            sb.append("\n");
            
            sb.append("mbdbOffset: " );
            sb.append(mbdbOffset);
            sb.append("\n");
            
            sb.append("mode: " );
            sb.append(mode);
            sb.append("\n");
            
            return sb.toString();
        }
    }
}
