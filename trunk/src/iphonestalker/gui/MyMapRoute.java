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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author MikeUCFL
 */
public class MyMapRoute implements MapRoute {

    private Color locationColor = null;
    private Color routeColor = null;
    private List<IPhoneLocation> coordinates = null;
    
    public MyMapRoute(Color locationColor, Color routeColor, List<IPhoneLocation> coordinates) {
        this.locationColor = locationColor;
        this.routeColor = routeColor;
        this.coordinates = coordinates;
    }
    @Override
    public void setCoordinates(List<IPhoneLocation> coordinates) {
        this.coordinates = coordinates;
    }
    
    @Override
    public List<IPhoneLocation> getCoordinates() {
        return new ArrayList<IPhoneLocation>(coordinates);
    }

    public IPhoneLocation getLastCoordinate() {
        return coordinates.get(coordinates.size()-1);
    }
    
    private void paintArrow(Graphics g, int x0, int y0, int x1, int y1){
	int deltaX = x1 - x0;
	int deltaY = y1 - y0;
	double frac = 1.0/100.0;
        int x2 = x0 + (int)((1-frac)*deltaX + frac*deltaY);
        int y2 = y0 + (int)((1-frac)*deltaY - frac*deltaX);
        int x3 = x0 + (int)((1-frac)*deltaX - frac*deltaY);
        int y3 = y0 + (int)((1-frac)*deltaY + frac*deltaX);

        Graphics2D g2 = (Graphics2D) g;
        Stroke drawingStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2.setStroke(drawingStroke);
        g2.drawLine(x0, y0, x1, y1);
        g2.setStroke(new BasicStroke(0));
        
        Polygon p = new Polygon();
        p.addPoint(x1, y1);
        p.addPoint(x2, y2);
        p.addPoint(x3, y3);
        g.fillPolygon(p);
        g.drawPolygon(p);
        
	//g.drawLine(x2, y2, x1, y1);
	//g.drawLine(x3, y3, x1, y1);

    }
    
     private void drawLine(Graphics g, Color color, int x0, int y0, int x1, int y1){
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(4));
        g2.drawLine(x0, y0, x1, y1);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x0, y0, x1, y1);
    }
    
    @Override
    public void paint(Graphics g) {
        
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        MyJMapViewer mapViewer = MyJMapViewer.getInstance();
        
        // Draw the routes first
        if (coordinates != null) {
            IPhoneLocation lastCoordinate = null;
            for (IPhoneLocation coordinate : coordinates) {
                
                // Draw the route if previous exists
                if (lastCoordinate != null) {
                    Point lastPosition = mapViewer.getMapPosition(
                        lastCoordinate.getLat(),lastCoordinate.getLon(), false);
                    Point currentPosition = mapViewer.getMapPosition(coordinate.getLat(),
                        coordinate.getLon(), false);
                    if (lastPosition != null) {
                        //g.setColor(routeColor);
                        drawLine(g, routeColor, lastPosition.x, lastPosition.y, 
                                currentPosition.x, currentPosition.y);
                        //g2.drawLine(lastPosition.x, lastPosition.y, 
                        //        currentPosition.x, currentPosition.y);
                        
                        //paintArrow(g, lastPosition.x, lastPosition.y, 
                        //        currentPosition.x, currentPosition.y);
                    }
                }
                
                lastCoordinate = coordinate;
            }
            
            // Now draw the points and info text
            for (int i = 0;i < coordinates.size();i++) {
                IPhoneLocation coordinate = coordinates.get(i);

                // Draw the point
                int size = 14;
                Point position = mapViewer.getMapPosition(coordinate.getLat(),
                        coordinate.getLon(), true);
                if (position != null) {
                    Color color = locationColor;
                    if (color == null) {
                        // Set this based on confidence
                        int confidence = coordinate.getConfidence();
                        int colorR = (confidence > 70?
                                255:
                                (int)(255.0/70.0*(70-confidence)));
                        color = new Color(colorR, 255, 0);
                    }
                    g.setColor(color);
                    g.fillOval(position.x - (int)(size/2), 
                            position.y - (int)(size/2), size, size);
                    g.setColor(Color.BLACK);
                    g.drawOval(position.x - (int)(size/2),
                            position.y - (int)(size/2), size, size);
                }
                
                String label = coordinate.getLabel((i+1),coordinates.size());
                if (label != null && position != null) {
                    g.setColor(new Color(0,0,0,150));
                    FontMetrics fm = mapViewer.getFontMetrics(g.getFont());
                    int width_offset = 8;
                    int width = fm.stringWidth(label);
                    g.fillRect(position.x - 7 - width_offset/2, position.y - 22, 
                            width + width_offset, 18);
                    g.setColor(Color.WHITE);
                    g.drawString(label, position.x - 7, position.y - 7);
                }
            }
        }
    }
    
}
