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
package iphonestalker.gui;

import iphonestalker.data.IPhoneLocation;
import iphonestalker.gui.interfaces.MapRoute;
import java.awt.Graphics;
import java.awt.Point;
import java.util.List;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;

/**
 *
 * @author MikeUCFL
 */
public class MyJMapViewer extends JMapViewer {
    
    private static MyJMapViewer instance = null;
    protected List<MapRoute> mapRouteList;

    protected boolean mapRoutesVisible;
    
    private MyJMapViewer() {
        super();
        mapRoutesVisible = true;
    }
    
    public static synchronized MyJMapViewer getInstance() {
        if (instance == null) {
            instance = new MyJMapViewer();
        }
        return instance;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int iMove = 0;

        int tilesize = tileSource.getTileSize();
        int tilex = center.x / tilesize;
        int tiley = center.y / tilesize;
        int off_x = (center.x % tilesize);
        int off_y = (center.y % tilesize);

        int w2 = getWidth() / 2;
        int h2 = getHeight() / 2;
        int posx = w2 - off_x;
        int posy = h2 - off_y;

        int diff_left = off_x;
        int diff_right = tilesize - off_x;
        int diff_top = off_y;
        int diff_bottom = tilesize - off_y;

        boolean start_left = diff_left < diff_right;
        boolean start_top = diff_top < diff_bottom;

        if (start_top) {
            if (start_left) {
                iMove = 2;
            } else {
                iMove = 3;
            }
        } else {
            if (start_left) {
                iMove = 1;
            } else {
                iMove = 0;
            }
        } // calculate the visibility borders
        int x_min = -tilesize;
        int y_min = -tilesize;
        int x_max = getWidth();
        int y_max = getHeight();

        // paint the tiles in a spiral, starting from center of the map
        boolean painted = true;
        int x = 0;
        while (painted) {
            painted = false;
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) {
                    x++;
                }
                for (int j = 0; j < x; j++) {
                    if (x_min <= posx && posx <= x_max && y_min <= posy && posy <= y_max) {
                        // tile is visible
                        Tile tile = tileController.getTile(tilex, tiley, zoom);
                        if (tile != null) {
                            painted = true;
                            tile.paint(g, posx, posy);
                            if (tileGridVisible) {
                                g.drawRect(posx, posy, tilesize, tilesize);
                            }
                        }
                    }
                    Point p = move[iMove];
                    posx += p.x * tilesize;
                    posy += p.y * tilesize;
                    tilex += p.x;
                    tiley += p.y;
                }
                iMove = (iMove + 1) % move.length;
            }
        }
        // outer border of the map
        int mapSize = tilesize << zoom;
        g.drawRect(w2 - center.x, h2 - center.y, mapSize, mapSize);

        // g.drawString("Tiles in cache: " + tileCache.getTileCount(), 50, 20);

        if (mapRectanglesVisible && mapRectangleList != null) {
            for (MapRectangle rectangle : mapRectangleList) {
                Coordinate topLeft = rectangle.getTopLeft();
                Coordinate bottomRight = rectangle.getBottomRight();
                if (topLeft != null && bottomRight != null) {
                    Point pTopLeft = getMapPosition(topLeft.getLat(), topLeft.getLon(), false);
                    Point pBottomRight = getMapPosition(bottomRight.getLat(), bottomRight.getLon(), false);
                    if (pTopLeft != null && pBottomRight != null) {
                        rectangle.paint(g, pTopLeft, pBottomRight);
                    }
                }
            }
        }

        if (mapMarkersVisible && mapMarkerList != null) {
            for (MapMarker marker : mapMarkerList) {
                paintMarker(g, marker);
            }
        }
        
        if (mapRoutesVisible && mapRouteList != null) {
            for (MapRoute mapRoute : mapRouteList) {
                mapRoute.paint(g);
            }
        }
        paintAttribution(g);
    }

    /**
     * Sets the displayed map pane and zoom level so that all routes are 
     * visible.
     */
    public void setDisplayToFitMapRoutes(List<MapRoute> displayMapRouteList) {
        if (displayMapRouteList == null) {
            displayMapRouteList = mapRouteList;
        }
        if (displayMapRouteList == null || displayMapRouteList.isEmpty())
            return;
        int x_min = Integer.MAX_VALUE;
        int y_min = Integer.MAX_VALUE;
        int x_max = Integer.MIN_VALUE;
        int y_max = Integer.MIN_VALUE;
        int mapZoomMax = tileController.getTileSource().getMaxZoom();
        for (MapRoute mapRoute : displayMapRouteList) {
            List<IPhoneLocation> coordinates = mapRoute.getCoordinates();
            for (IPhoneLocation coordinate : coordinates) {
                int x = OsmMercator.LonToX(coordinate.getLon(), mapZoomMax);
                int y = OsmMercator.LatToY(coordinate.getLat(), mapZoomMax);
                x_max = Math.max(x_max, x);
                y_max = Math.max(y_max, y);
                x_min = Math.min(x_min, x);
                y_min = Math.min(y_min, y);
            }
        }
        int height = Math.max(0, getHeight());
        int width = Math.max(0, getWidth());
        int newZoom = mapZoomMax;
        int x = x_max - x_min;
        int y = y_max - y_min;
        while (x > width || y > height) {
            newZoom--;
            x >>= 1;
            y >>= 1;
        }
        x = x_min + (x_max - x_min) / 2;
        y = y_min + (y_max - y_min) / 2;
        int z = 1 << (mapZoomMax - newZoom);
        x /= z;
        y /= z;
        setDisplayPosition(x, y, newZoom);
    }
    
    public List<MapRoute> getMapRouteList() {
        return mapRouteList;
    }

    public void setMapRouteList(List<MapRoute> mapRouteList) {
        this.mapRouteList = mapRouteList;
    }
    
    public void addMapRoute(MapRoute mapRoute) {
        mapRouteList.add(mapRoute);
    }
    
    public void clearMapRoutes() {
        mapRouteList.clear();
    }

    public boolean isMapRoutesVisible() {
        return mapRoutesVisible;
    }

    public void setMapRoutesVisible(boolean mapRoutesVisible) {
        this.mapRoutesVisible = mapRoutesVisible;
    }
    
}
