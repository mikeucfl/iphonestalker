/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.data;

import org.openstreetmap.gui.jmapviewer.Coordinate;

/**
 *
 * @author MikeUCFL
 */
public class MyCoordinate extends Coordinate {
    
    private String label = null;
    
    public MyCoordinate(String label, double lat, double lon) {
        super(lat, lon);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
}
