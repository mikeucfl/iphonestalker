/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.gui.interfaces;

import iphonestalker.data.MyCoordinate;
import java.awt.Graphics;
import java.util.List;

/**
 *
 * @author MikeUCFL
 */
public interface MapRoute {

    
    /**
     * Sets the route coordinates
     * 
     * @param coordinates 
     */
    public void setCoordinates(List<MyCoordinate> coordinates);
    
    /**
     * @return list of the route coordinates.
     */
    public List<MyCoordinate> getCoordinates();

    /**
     * Paints the route on the map.
     *
     * @param g
     * @param position
     */
    public void paint(Graphics g);
}

