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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.openstreetmap.gui.jmapviewer.Coordinate;

/**
 *
 * @author Mike
 */
public class IPhoneLocation extends Coordinate {

    private Date fulldate = null;
    private String day = null;
    private String time = null;
    private double horizontalAccuracy = -1.0;
    private int confidence = -1;
    private int hitCount = 0;
    
    private static SimpleDateFormat fullDateFormat = 
            new SimpleDateFormat("EEE MMM dd yyyy h:mm:ss a");
    private static SimpleDateFormat sdfDay =
            new SimpleDateFormat("EEE MMM d yyyy");
    private static SimpleDateFormat sdfTime =
            new SimpleDateFormat("h:mm a");
    
    public IPhoneLocation(double lat, double lon) {
        super(lat, lon);
    }
    
    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public String getDay() {
        return day;
    }

    public Date getFulldate() {
        return fulldate;
    }
    
    public void setFulldate(Date fulldate) {
        this.fulldate = fulldate;
        day = sdfDay.format(fulldate);
        time = sdfTime.format(fulldate);
    }

    public String getFullDateString() {
        return fullDateFormat.format(fulldate);
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public void incrementHitCount() {
        hitCount++;
    }
    
    public double getHorizontalAccuracy() {
        return horizontalAccuracy;
    }

    public void setHorizontalAccuracy(double horizontalAccuracy) {
        this.horizontalAccuracy = horizontalAccuracy;
    }

    public String getLabel(int index, int size) {
        return getFullDateString()
            + " [route: " + index + "/" + size
            + ", confidence: " + confidence
            + "]";
    }
    
    @Override
    public String toString() {
        return getLat() + "," + getLon() + ",0.0";
    }
}
