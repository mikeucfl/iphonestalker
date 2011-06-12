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
package iphonestalker.gui.interfaces;

import iphonestalker.data.IPhoneLocation;
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
    public void setCoordinates(List<IPhoneLocation> coordinates);
    
    /**
     * @return list of the route coordinates.
     */
    public List<IPhoneLocation> getCoordinates();

    /**
     * Paints the route on the map.
     *
     * @param g
     * @param position
     */
    public void paint(Graphics g);
}

