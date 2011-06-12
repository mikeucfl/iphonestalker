/*
 *  This file is a part of iPhoneStalker.
 * 
 *  iPhoneStalker is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package iphonestalker.util;

import iphonestalker.data.IPhoneRoute;
import iphonestalker.data.IPhoneLocation;
import iphonestalker.util.io.MBDBReader;
import iphonestalker.util.io.MBDBReader.MBDBData;
import iphonestalker.util.io.MBDXReader;
import iphonestalker.util.io.MBDXReader.MBDXData;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MikeUCFL
 */
public class BackupReader { 

    private static final Logger logger =
            Logger.getLogger(BackupReader.class.getName());
    private static final String FILE_SEPARATOR =
            System.getProperty("file.separator");

    public BackupReader() {
        
    }
    
    public String processFolder (String folder, IPhoneRoute iPhoneData) {
        
        String errorReason = null;
        
        MBDBReader mbdbReader = new MBDBReader();
        MBDXReader mbdxReader = new MBDXReader();
        
        String manifestMbdb = folder + FILE_SEPARATOR + "Manifest.mbdb";
        boolean success = mbdbReader.processMbdb(new File(manifestMbdb));
        if (success) {
            
            String manifestMbdx = folder + FILE_SEPARATOR + "Manifest.mbdx";
            success = mbdxReader.processMbdx(new File(manifestMbdx));

            if (success) {
                
                String consolidateFile = "Library/Caches/locationd/consolidated.db";
                MBDBData mbdbData = mbdbReader.getMBDBData(consolidateFile);
                if (mbdbData != null) {
                    MBDXData mbdxData = mbdxReader.getMBDXData(mbdbData.startOffset);

                    if (mbdxData != null) {
                        try {
                            try {
                                Class.forName("org.sqlite.JDBC");
                            } catch (ClassNotFoundException ex) {
                                logger.log(Level.SEVERE, null, ex);
                                errorReason = "Could not load JDBC class";
                                return errorReason;
                            }

                            // Connect to the DB
                            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + folder
                                    + FILE_SEPARATOR + mbdxData.fileId);
                            Statement statement = connection.createStatement();
                            ResultSet rs = null;
                                                        
                            int cdmaCellLocations = 0;
                            int cellLocations = 0;
                            boolean exception = false;
                            try {
                                // Get the list of CellLocations (AT&T)
                                rs = statement.executeQuery("SELECT COUNT(*) FROM CellLocation");
                                if (rs.next()) {
                                    cellLocations = Integer.parseInt(rs.getString("COUNT(*)"));
                                }
                            } catch (SQLException e) { 
                                exception = true;
                            }

                            try {
                                // Get the list of CellLocations (Verizon)
                                rs = statement.executeQuery("SELECT COUNT(*) FROM CdmaCellLocation");
                                if (rs.next()) {
                                    cdmaCellLocations = Integer.parseInt(rs.getString("COUNT(*)"));
                                }
                            } catch (SQLException e) {
                                if (exception) {
                                    // Looks like both values can't be retrieved
                                    throw(e);
                                }
                            }
                            
                            if (cellLocations > cdmaCellLocations) {
                                // Get AT&T data
                                rs = statement.executeQuery("SELECT Timestamp, Latitude, Longitude, "
                                        + "HorizontalAccuracy, Confidence FROM CellLocation");
                            } else {
                                // Get Verizon data
                                rs = statement.executeQuery("SELECT Timestamp, Latitude, Longitude, " +
                                    "HorizontalAccuracy, Confidence FROM CdmaCellLocation");
                            }

                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMM dd, yyyy z");
                            try {
                                cal.setTime(sdf.parse("January 1, 2001 GMT-00:00"));
                            } catch (ParseException ex) {
                                logger.log(Level.SEVERE, null, ex);
                                errorReason = "Could not load JDBC class";
                                return errorReason;
                            }
                            long startTimestamp = cal.getTimeInMillis();

                            while (rs.next()) {

                                int confidence = Integer.parseInt(rs.getString("Confidence"));
                                if (confidence == 0) {
                                    // skip
                                    continue;
                                }
                                double iPhoneTimestamp = Double.parseDouble(rs.getString("Timestamp")) * 1000.0;
                                long timestamp = startTimestamp + (long) iPhoneTimestamp;
                                double latitude = Double.parseDouble(rs.getString("Latitude"));
                                double longitude = Double.parseDouble(rs.getString("Longitude"));
                                double horizontalAccuracy = Double.parseDouble(rs.getString("HorizontalAccuracy"));
                                cal.setTimeInMillis(timestamp);
                                
                                IPhoneLocation iPhoneLocation = new IPhoneLocation(latitude, longitude);
                                iPhoneLocation.setFulldate(cal.getTime());
                                iPhoneLocation.setHorizontalAccuracy(horizontalAccuracy);
                                iPhoneLocation.setConfidence(confidence);
                                iPhoneLocation.setHitCount(1);

                                iPhoneData.addLocation(iPhoneLocation);
                            }
                            rs.close();
                            connection.close();
                        } catch (SQLException ex) {
                            errorReason = "OVERRIDE Could not execute SQL operation: " + ex;
                        }
                    } else {
                        errorReason = "Could not locate MBDXData via offset: " + mbdbData.startOffset;
                    }
                } else {
                    errorReason = "Could not locate " + consolidateFile;
                }
            } else {
                errorReason =  "Unable to parse " + manifestMbdx;
            }
        } else {
            errorReason = "Unable to parse " + manifestMbdb;
        }
        return errorReason;
    }

}
