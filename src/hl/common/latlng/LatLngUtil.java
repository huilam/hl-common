package hl.common.latlng;

import java.util.logging.Logger;

public class LatLngUtil {
	
	private static Logger logger 	= Logger.getLogger(LatLngUtil.class.getName());
	/**
	EARTH Radius in KM 		= 6371
	Latitude Degree Range 	= (S/-) 90 to (N/+) 90
	Longitude Degree Range 	= (W/-) 180 to (E/+) 180
	**/
    private static double EARTH_RADIUS 	= 6378.16;
    private static double PIx = Math.PI / 180;

    private static double deg2radius(double aDeg)
    {
    	return aDeg * PIx;
    }
	
    public static double getDistanceInKm(
            double lat1, double lng1,
            double lat2, double lng2)
    {
        double dlng = deg2radius(lng2 - lng1);
        double dlat = deg2radius(lat2 - lat1);

        double a = (Math.sin(dlat / 2) * Math.sin(dlat / 2)) 
        		+ Math.cos(deg2radius(lat1)) * Math.cos(deg2radius(lat2)) * (Math.sin(dlng / 2) * Math.sin(dlng / 2));
        double angle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return angle * EARTH_RADIUS;
    }

	public static boolean isValidLatLng(double aLat, double aLng)
	{
		return (aLat>=-90 && aLat<=90) && (aLng>=-180 && aLng<=180);
	}
	
	public static boolean isWithinDistanceInKM(double aLat1, double aLng1, double aLat2, double aLng2, double aDistanceInKM)
	{
		return aDistanceInKM <= getDistanceInKm(aLat1, aLng1, aLat2, aLng2);
	}
	
}
