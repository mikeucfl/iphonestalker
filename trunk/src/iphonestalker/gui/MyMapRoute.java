/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iphonestalker.gui;

import iphonestalker.data.MyCoordinate;
import iphonestalker.gui.interfaces.MapRoute;
import java.awt.BasicStroke;
import java.awt.Color;
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
    private List<MyCoordinate> coordinates = null;
    
    public MyMapRoute(Color locationColor, Color routeColor, List<MyCoordinate> coordinates) {
        this.locationColor = locationColor;
        this.routeColor = routeColor;
        this.coordinates = coordinates;
    }
    @Override
    public void setCoordinates(List<MyCoordinate> coordinates) {
        this.coordinates = coordinates;
    }
    
    @Override
    public List<MyCoordinate> getCoordinates() {
        return new ArrayList<MyCoordinate>(coordinates);
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
        g2.setStroke(new BasicStroke(5));
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
            MyCoordinate lastCoordinate = null;
            for (MyCoordinate coordinate : coordinates) {
                
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
            for (MyCoordinate coordinate : coordinates) {
                // Draw the point
                int size_h = 7;
                int size = size_h * 2;
                Point position = mapViewer.getMapPosition(coordinate.getLat(),
                        coordinate.getLon(), true);
                if (position != null) {
                    g.setColor(locationColor);
                    //g2.draw(null)
                    g.fillOval(position.x - size_h, position.y - size_h, size, size);
                    g.setColor(Color.BLACK);
                    g.drawOval(position.x - size_h, position.y - size_h, size, size);
                }
                
                String label = coordinate.getLabel();
                if (label != null && position != null) {
                    g.setColor(new Color(0,0,0,150));
                    g.fillRect(position.x - size_h - 5, position.y - size_h*3 + 2, 
                            (int)(size*label.length()/2.65), size + 2);
                    g.setColor(Color.WHITE);
                    g.drawString(label, position.x - size_h, position.y - size_h);
                }
            }
        }
    }
    
}
