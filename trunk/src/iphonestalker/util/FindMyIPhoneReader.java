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
package iphonestalker.util;

import org.apache.commons.codec.binary.Base64;
import iphonestalker.data.IPhoneRoute;
import iphonestalker.data.IPhoneLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author Mike
 */
public class FindMyIPhoneReader {

    private static final Logger logger = Logger.getLogger(FindMyIPhoneReader.class.getName());
    private static FindMyIPhoneReader instance = null;
    private HttpClient client = null;
    private HttpPost post = null;
    private int numDevices = 0;
    List<IPhoneRoute> iPhoneRouteList = null;

    private FindMyIPhoneReader() {
    }

    public static synchronized FindMyIPhoneReader getInstance() {
        if (instance == null) {
            instance = new FindMyIPhoneReader();
        }
        return instance;
    }

    public String connect(String username, String password) {

        numDevices = 0;

        // Setup HTTP parameters
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Find iPhone/1.1 MeKit (iPhone: iPhone OS/4.2.1)");
        client = new DefaultHttpClient(params);

        // Construct the post
        post = new HttpPost("https://fmipmobile.me.com/fmipservice/device/" + username);
        post.addHeader("X-Client-Uuid", "0000000000000000000000000000000000000000");
        post.addHeader("X-Client-Name", "My iPhone");
        Header[] headers = {
            new BasicHeader("X-Apple-Find-Api-Ver", "2.0"),
            new BasicHeader("X-Apple-Authscheme", "UserIdGuest"),
            new BasicHeader("X-Apple-Realm-Support", "1.0"),
            new BasicHeader("Content-Type", "application/json; charset=utf-8"),
            new BasicHeader("Accept-Language", "en-us"),
            new BasicHeader("Pragma", "no-cache"),
            new BasicHeader("Connection", "keep-alive"),
            new BasicHeader("Authorization", "Basic "
            + new String(Base64.encodeBase64((username + ":" + password).getBytes())))
        };
        post.setHeaders(headers);

        HttpResponse response = null;
        try {
            // Execute
            response = client.execute(post);

            // We should get a redirect
            if (response.getStatusLine().getStatusCode() == 330) {
                String newHost = (((Header[]) response.getHeaders("X-Apple-MME-Host"))[0]).getValue();
                try {
                    post.setURI(new URI("https://" + newHost + "/fmipservice/device/" + username + "/initClient"));
                } catch (URISyntaxException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return "Couldn't post URI: " + ex;
                }

                // Dump the data so we can execute a new post
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                JSONObject object = (JSONObject) JSONValue.parse(reader);

                // This post should get us our data
                response = client.execute(post);
                reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

                // Read the data
                object = (JSONObject) JSONValue.parse(reader);
                JSONArray array = ((JSONArray) object.get("content"));
                numDevices = array.size();

                // Setup the route data
                iPhoneRouteList = new ArrayList<IPhoneRoute>();

                for (int i = 0; i < numDevices; i++) {
                    // Get the device data
                    object = (JSONObject) array.get(i);
                    IPhoneLocation iPhoneLocation = getLocation(object);
                    
                    // Add route data
                    IPhoneRoute iPhoneRoute = new IPhoneRoute();
                    String modelDisplayName = (String) object.get("modelDisplayName");
                    String deviceModelName = (String) object.get("deviceModel");
                    String name = (String) object.get("name");

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    iPhoneRoute.name = "[FMI #" + (i + 1) + "]" + name + ":" + 
                            modelDisplayName + " " + deviceModelName;
                    iPhoneRoute.lastBackupDate = cal.getTime();
                    iPhoneRoute.setFMI(true);
                    
                    if (iPhoneLocation != null) {
                        iPhoneRoute.addLocation(iPhoneLocation);
                    }
                    iPhoneRouteList.add(iPhoneRoute);
                }

            } else {
                logger.log(Level.WARNING, "Couldn\'t retrieve iPhone data: " + 
                        response.getStatusLine().toString());
                return "Couldn't retrieve iPhone data: " + response.getStatusLine().toString();
            }
        } catch (ClientProtocolException ex) {
            logger.log(Level.SEVERE, null, ex);
            return "Couldn't retrieve iPhone data: " + ex;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return "Couldn't retrieve iPhone data: " + ex;
        }

        return null;
    }

    public boolean pollLocation(int device) {

        if (device < numDevices) {
            try {
                // Poll for data
                HttpResponse response = client.execute(post);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

                // Read the data
                JSONObject object = (JSONObject) JSONValue.parse(reader);
                JSONArray array = ((JSONArray) object.get("content"));
                //int devices = array.size();
                //System.out.println("Found " + devices + " devices");

                // Get the device data
                object = (JSONObject) array.get(device);
                
                // Update the route information
                IPhoneLocation iPhoneLocation = getLocation(object);
                
                if (iPhoneLocation != null) {
                    iPhoneRouteList.get(device).addLocation(iPhoneLocation);
                }
            } catch (ClientProtocolException ex) {
                logger.log(Level.SEVERE, null, ex);
                return false;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            logger.log(Level.WARNING, "Device {0} is out of range ({1} max)",
                    new Object[]{(device + 1), numDevices});
            return false;
        }

        return true;
    }

    public IPhoneRoute getIPhoneRoute(int device) {
        return iPhoneRouteList.get(device);
    }
    
    public List<IPhoneRoute> getIPhoneRouteList() {
        return iPhoneRouteList;
    }
    
    private IPhoneLocation getLocation(JSONObject object) {
        IPhoneLocation iPhoneLocation = null;

        // Get location information
        JSONObject location = (JSONObject) object.get("location");
        if (location != null) {

            boolean locationFinished = (Boolean)location.get("locationFinished");
            if (locationFinished) {
                long timestamp = (Long) location.get("timeStamp");
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timestamp);
                //System.out.println("positionType: " + location.get("positionType"));
                double horizontalAccuracy = (Double) location.get("horizontalAccuracy");
                //System.out.println("locationFinished: " + location.get("locationFinished"));
                double latitude = (Double) location.get("latitude");
                double longitude = (Double) location.get("longitude");
                //System.out.println("isOld: " + location.get("isOld"));

                DecimalFormat df = new DecimalFormat("#.0000");
                iPhoneLocation = new IPhoneLocation(
                        Double.parseDouble(df.format(latitude)),
                        Double.parseDouble(df.format(longitude)));
                iPhoneLocation.setFulldate(cal.getTime());
                iPhoneLocation.setHorizontalAccuracy(horizontalAccuracy);
                iPhoneLocation.setConfidence(70-(int)horizontalAccuracy);
                iPhoneLocation.setHitCount(1);
            }

        }

        return iPhoneLocation;
    }

    public int getNumDevices() {
        return numDevices;
    }
}
