/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.data;

import iphonestalker.data.IPhoneData.IPhoneLocation;
import org.openstreetmap.gui.jmapviewer.Coordinate;

/**
 *
 * @author MikeUCFL
 */
public class MyCoordinate extends Coordinate {
    
    private String label = null;
    private int confidence = 0;
    
    public MyCoordinate(String label, IPhoneLocation iPhoneLocation) {
        super(iPhoneLocation.latitude, iPhoneLocation.longitude);

        this.label = label;
        this.confidence = iPhoneLocation.confidence;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
        
}
