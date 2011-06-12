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

import iphonestalker.data.IPhoneRoute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author MikeUCFL
 */
public class TrackerTableModel extends AbstractTableModel {

    private String columns[] = {"#", "Date", "Locations", 
                                "Time Period"};
    private IPhoneRoute iPhoneData = null;
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

    public synchronized void setData(final IPhoneRoute iPhoneData) {

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
