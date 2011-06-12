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
package iphonestalker.data;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author MikeUCFL
 */
public class IPhoneRoute {

    private static final Logger logger =
            Logger.getLogger(IPhoneRoute.class.getName());
    
    public String name = null;
    public Date lastBackupDate = null;
    private Map<String, ArrayList<IPhoneLocation>> locationMap = null;
    private List<String> days = null;
    private boolean fmi = false;
    
    private static SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEE MMM dd yyyy h:mm:ss a");

    public IPhoneRoute() {
        locationMap = new TreeMap<String, ArrayList<IPhoneLocation>>();
        days = new ArrayList<String>();
    }

    public void addLocation(IPhoneLocation newLocation) {

        // Override exact locations with better confidences
        boolean isUpdate = false;
        ArrayList<IPhoneLocation> locations = locationMap.get(newLocation.getDay());
        
        //TODO add a list of dates so we can do a start/end date? This would fix the
        // numerous same location but diff date craps for findmyiphone. Also we'd have to
        // display this properly on the label.. or wait what about looking at the total route for
        // this info? that has everything anyways?
        if (locations != null) {
            for (IPhoneLocation location : locations) {
                if (location.getFulldate().equals(newLocation.getFulldate())) {
                    if (newLocation.getConfidence() > location.getConfidence()) {
                        location.setConfidence(newLocation.getConfidence());
                        location.setLat(newLocation.getLat());
                        location.setLon(newLocation.getLon());
                        location.incrementHitCount();
                    }
                    isUpdate = true;
                }
            }
        } else {
            locations = new ArrayList<IPhoneLocation>();
            locationMap.put(newLocation.getDay(), locations);
            days.add(newLocation.getDay());
        }

        if (!isUpdate) {
            locations.add(newLocation);
        }
    }

    public List<String> getListOfDays() {
        ArrayList<String> daysClone = new ArrayList<String>(days);
        
        Collections.sort(daysClone, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {

                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d yyyy");
                try {
                    cal1.setTime(sdf.parse((String)o1));
                    cal2.setTime(sdf.parse((String)o2));
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                boolean equals = cal1.getTime().equals(cal2.getTime());
                boolean before = cal1.getTime().before(cal2.getTime());
                return (equals?0:(before?1:-1));
            }
        
        });
        
        return daysClone;
    }

    public int getNumberOfLocations(String day) {
        ArrayList<IPhoneLocation> locations = locationMap.get(day);
        return (locations != null ? locations.size() : 0);
    }

    public ArrayList<IPhoneLocation> getIphoneLocations(ArrayList<String> days) {
        ArrayList<IPhoneLocation> iPhoneLocations = new ArrayList<IPhoneLocation>();
        for (String day : days) {
            iPhoneLocations.addAll(locationMap.get(day));
        }
        return iPhoneLocations;
    }
    
    public ArrayList<IPhoneLocation> getIPhoneLocations(String day) {
        return locationMap.get(day);
    }
    
    public Date getStartDate(String day) {
        ArrayList<IPhoneLocation> locations = locationMap.get(day);
        return (locations != null ? locations.get(0).getFulldate() : null);
    }

    public Date getEndDate(String day) {
        ArrayList<IPhoneLocation> locations = locationMap.get(day);
        return (locations != null ? locations.get(locations.size() - 1).getFulldate() : null);
    }

    public String getLastBackupDate() {
        return fullDateFormat.format(lastBackupDate);
    }

    public boolean isFMI() {
        return fmi;
    }

    public void setFMI(boolean fmi) {
        this.fmi = fmi;
    }
    
    public void exportToFile(ArrayList<String> days) {
        BufferedWriter bufferedWriter = null;

        boolean exported = false;
        String filename = name + " on " + getLastBackupDate().replaceAll(":", ".") + ".kml";
        try {
            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(
                    new FileWriter(filename));

            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            bufferedWriter.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\""
                    + " xmlns:gx=\"http://www.google.com/kml/ext/2.2\""
                    + " xmlns:kml=\"http://www.opengis.net/kml/2.2\""
                    + " xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            bufferedWriter.write("<Document>\n");
            bufferedWriter.write("  <name>" + this + "</name>\n");
            bufferedWriter.write("  <Style id=\"route\">\n");
            bufferedWriter.write("    <LineStyle>\n");
            bufferedWriter.write("      <color>c8ff00ff</color>\n");
            bufferedWriter.write("      <gx:physicalWidth>12</gx:physicalWidth>\n");
            bufferedWriter.write("      <gx:outerColor>55ff00ff</gx:outerColor>\n");
            bufferedWriter.write("      <gx:outerWidth>0.25</gx:outerWidth>\n");
            bufferedWriter.write("    </LineStyle>\n");
            bufferedWriter.write("  </Style>\n");
            
            bufferedWriter.write("  <Style id=\"startPoint\">\n");
            bufferedWriter.write("    <IconStyle>\n");
            bufferedWriter.write("      <scale>1.1</scale>\n");
            bufferedWriter.write("      <Icon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars.png</href>\n");
            bufferedWriter.write("      </Icon>\n");
            bufferedWriter.write("      <hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            bufferedWriter.write("    </IconStyle>\n");
            bufferedWriter.write("    <ListStyle>\n");
            bufferedWriter.write("      <ItemIcon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-stars-lv.png</href>\n");
            bufferedWriter.write("      </ItemIcon>\n");
            bufferedWriter.write("     </ListStyle>\n");
            bufferedWriter.write("  </Style>\n");
            
            bufferedWriter.write("  <Style id=\"routePoint\">\n");
            bufferedWriter.write("    <IconStyle>\n");
            bufferedWriter.write("      <scale>1.1</scale>\n");
            bufferedWriter.write("      <Icon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-circle.png</href>\n");
            bufferedWriter.write("      </Icon>\n");
            bufferedWriter.write("      <hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            bufferedWriter.write("    </IconStyle>\n");
            bufferedWriter.write("    <ListStyle>\n");
            bufferedWriter.write("      <ItemIcon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-circle-lv.png</href>\n");
            bufferedWriter.write("      </ItemIcon>\n");
            bufferedWriter.write("     </ListStyle>\n");
            bufferedWriter.write("  </Style>\n");
            
            bufferedWriter.write("  <Style id=\"stopPoint\">\n");
            bufferedWriter.write("    <IconStyle>\n");
            bufferedWriter.write("      <scale>1.1</scale>\n");
            bufferedWriter.write("      <Icon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-square.png</href>\n");
            bufferedWriter.write("      </Icon>\n");
            bufferedWriter.write("      <hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
            bufferedWriter.write("    </IconStyle>\n");
            bufferedWriter.write("    <ListStyle>\n");
            bufferedWriter.write("      <ItemIcon>\n");
            bufferedWriter.write("        <href>http://maps.google.com/mapfiles/kml/paddle/ylw-square-lv.png</href>\n");
            bufferedWriter.write("      </ItemIcon>\n");
            bufferedWriter.write("     </ListStyle>\n");
            bufferedWriter.write("  </Style>\n");
            
            for (String day : days) {
                ArrayList<IPhoneLocation> locations = locationMap.get(day);
                for (int i = 0;i < locations.size();i++) {

                    String style = null;
                    if (i == 0) {
                        style = "startPoint";
                    } else if (i == (locations.size()-1)) {
                        style = "endPoint";
                    } else {
                        style = "routePoint";
                    }
                    //TODO endPoint is not showing a square
                    IPhoneLocation location = locations.get(i);
                    
                    bufferedWriter.write("  <Placemark>\n");
                    bufferedWriter.write("    <name>" + location.getFullDateString() + " [" 
                            + (i+1) + "/" + locations.size() + "]</name>\n");
                    bufferedWriter.write("    <styleUrl>#" + style + "</styleUrl>\n");
                    bufferedWriter.write("    <Point>\n");
                    bufferedWriter.write("      <coordinates>" + location.toString() + "</coordinates>\n");
                    bufferedWriter.write("    </Point>\n");
                    bufferedWriter.write("    <LookAt>\n");
                    bufferedWriter.write("      <longitude>" + location.getLon() + "</longitude>\n");
                    bufferedWriter.write("      <latitude>" + location.getLat() + "</latitude>\n");
                    bufferedWriter.write("      <range>100.000000</range>\n");
                    bufferedWriter.write("      <tilt>45.000000</tilt>\n");
                    bufferedWriter.write("    </LookAt>\n");
                    bufferedWriter.write("  </Placemark>\n");
                }
            }

            for (String day : days) {
                ArrayList<IPhoneLocation> locations = locationMap.get(day);

                bufferedWriter.write("  <Placemark>\n");
                bufferedWriter.write("    <name>" + this + "</name>\n");
                bufferedWriter.write("    <styleUrl>#route</styleUrl>\n");
                bufferedWriter.write("    <description>\n");
                bufferedWriter.write("      <![CDATA[" + day + " ("
                        + locations.size() + "points)]]>\n");
                bufferedWriter.write("    </description>\n");
                bufferedWriter.write("    <GeometryCollection>\n");
                bufferedWriter.write("      <LineString>\n");
                bufferedWriter.write("        <coordinates>");

                for (IPhoneLocation location : locations) {
                    bufferedWriter.write(location.toString() + " ");
                }

                bufferedWriter.write("</coordinates>\n");
                bufferedWriter.write("      </LineString>\n");
                bufferedWriter.write("    </GeometryCollection>\n");
                bufferedWriter.write("  </Placemark>\n");
            }
            bufferedWriter.write("  </Document>\n");
            bufferedWriter.write("</kml>\n");
            exported = true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        JOptionPane.showMessageDialog(null, "Exporting " + days.size() + " day(s)" 
                                            + " of data to '" + filename +
                                            "' was " + (exported?"successful!":
                                            "unsuccessful!"));
    }


    @Override
    public String toString() {
        return name + " @ " + getLastBackupDate();
    }

}
