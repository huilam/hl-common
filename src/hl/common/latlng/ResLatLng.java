package hl.common.latlng;

public class ResLatLng {

	private String resource_name 	= null;
	private long epoch_timestamp 	= 0;
	private double latitude 		= 0;
	private double longitude 		= 0;
	
	public ResLatLng(String aResName)
	{
		setResource_name(aResName);
	}
	
	public String getResource_name() {
		return resource_name;
	}
	public void setResource_name(String resource_name) {
		this.resource_name = resource_name;
	}
	////
	public long getEpoch_timestamp() {
		return epoch_timestamp;
	}
	public void setEpoch_timestamp(long epoch_timestamp) {
		this.epoch_timestamp = epoch_timestamp;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	//
	public void setTimestampNLatLng(long aEpochTime, double aLat, double aLng) {
		setEpoch_timestamp(aEpochTime);
		setLatitude(aLat);
		setLongitude(aLng);
	}
	
	public double[] getLatLng()
	{
		return new double[] {getLatitude(), getLongitude()};
	}
}
