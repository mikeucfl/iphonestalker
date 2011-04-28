/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.util.io;

import iphonestalker.data.IPhoneData;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 *
 * @author MikeUCFL
 */
public class InfoReader extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(InfoReader.class.getName());
    
    private String tempVal = null;
    private IPhoneData iPhoneData = null;
    private boolean readDisplay = false;
    private boolean readBackupDate = false;

    public InfoReader() {
    }
    
    public IPhoneData parseFile(File file) {
        try {
            //get a factory
            SAXParserFactory spf = SAXParserFactory.newInstance();

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            iPhoneData = new IPhoneData();
            
            //parse the file and also register this class for call backs
            sp.parse(file, this);
            
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InfoReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InfoReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InfoReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return iPhoneData;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName,
            String qName) throws SAXException {

        if (readDisplay) {
            iPhoneData.name = tempVal;
            readDisplay = false;
        } else if (readBackupDate) {
            
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z' z");
            try {
                tempVal += " GMT-00:00";
                cal.setTime(sdf.parse(tempVal));
                iPhoneData.lastBackupDate = cal.getTime();
            } catch (ParseException ex) {
                logger.log(Level.WARNING, "Couldn't set iPhone backup date", ex);
                iPhoneData.lastBackupDate = null;
            }
            readBackupDate = false;
        } else if (tempVal.equals("Display Name")) {
            readDisplay = true;
        } else if (tempVal.equals("Last Backup Date")) {
            readBackupDate = true;
        }
    }
 
}