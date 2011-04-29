/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package iphonestalker.gui;

import iphonestalker.data.IPhoneData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author baranmi
 */
public class TrackerTableModel extends AbstractTableModel {

    private String columns[] = {"#", "Date", "Locations", 
                                "Time Period"};
    private IPhoneData iPhoneData = null;
    private ArrayList<String> days = null;
    private static SimpleDateFormat sdfTime = new SimpleDateFormat("h:mm a");
    
    public TrackerTableModel() {
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    public synchronized void setData(final IPhoneData iPhoneData) {

        this.iPhoneData = iPhoneData;
        updateInternalData();
        fireTableDataChanged();
    }

    public synchronized boolean isEmpty() {
        return (iPhoneData == null);
    }
    
    @Override
    public synchronized Object getValueAt(int row, int column) {
        Object returnVal = null;

        String day = days.get(row);
        
        if (column == 0) {
            returnVal = row + 1;
        } else if (column == 1) {
            returnVal = day;
        } else if (column == 2) {
            returnVal = iPhoneData.getNumberOfLocations(day);
        } else if (column == 3) {
            
            String startTime = sdfTime.format(iPhoneData.getStartDate(day));
            String endTime = sdfTime.format(iPhoneData.getEndDate(day));
            
            returnVal = (startTime.equals(endTime)?
                    startTime:
                    startTime + " to " + endTime);
            
        } else {
            returnVal = "Error";
        }

        return returnVal;
    }
    
    private synchronized void updateInternalData() {
        if (iPhoneData != null) {
            days = (ArrayList<String>)iPhoneData.getListOfDays();
        }
    }
    
    public synchronized String getDay(int row) {
        return days.get(row);
    }
    
    @Override
    public synchronized int getRowCount() {
        int size = 0;

        if (days != null) {
            size = days.size();
        }

        return size;
    }

}
