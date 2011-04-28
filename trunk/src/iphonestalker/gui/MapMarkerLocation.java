/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

/**
 *
 * @author MikeUCFL
 */
public class MapMarkerLocation implements MapMarker {

    private double lat = 0.0;
    private double lon = 0.0;
    private Color color = null;
    private String label = null;

    public MapMarkerLocation(double lat, double lon) {
        this(Color.YELLOW, lat, lon);
    }

    public MapMarkerLocation(Color color, double lat, double lon) {
        this(null, color, lat, lon);
    }
    
    public MapMarkerLocation(String label, Color color, double lat, double lon) {
        super();
        this.label = label;
        this.color = color;
        this.lat = lat;
        this.lon = lon;
    }
    
    @Override
    public double getLat() {
        return lat;
    }

    @Override
    public double getLon() {
        return lon;
    }

    @Override
    public void paint(Graphics g, Point position) {
        int size_h = 5;
        int size = size_h * 2;
        g.setColor(color);
        g.fillOval(position.x - size_h, position.y - size_h, size, size);
        g.setColor(Color.BLACK);
        g.drawOval(position.x - size_h, position.y - size_h, size, size);
        
        if (label != null) {
            g.setColor(new Color(0,0,0,150));
            g.fillRect(position.x - size_h - 5, position.y - size_h*3, 
                    (int)(size*label.length()/1.5), size + 2);
            g.setColor(Color.WHITE);
            g.drawString(label, position.x - size_h, position.y - size_h);
        }
        
    }

    @Override
    public String toString() {
        return "MapMarkerLocation at " + lat + " " + lon;
    }

}