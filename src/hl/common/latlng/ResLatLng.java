package hl.common.latlng;

public class ResLatLng {

	private String resource_uid		= null;
	private long epoch_timestamp 	= 0;
	private double latitude 		= 0;
	private double longitude 		= 0;
	private int level_no  			= 0;
	private double altitude  		= 0;
	private String altitude_unit	= "m";
	
	public ResLatLng(String aResUID)
	{
		setResource_uid(aResUID);
	}
	
	public String getResource_uid() {
		return this.resource_uid;
	}
	public void setResource_uid(String resource_uid) {
		this.resource_uid = resource_uid;
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
	
	public double getAltitude() {
		return altitude;
	}
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	
	public int getLevelNo() {
		return level_no;
	}
	public void setLevelNo(int aLevelNo) {
		this.level_no = aLevelNo;
	}
	
	public String getAltitudeUnit() {
		return altitude_unit;
	}
	public void setAltitudeUnit(String altitude_unit) {
		this.altitude_unit = altitude_unit;
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
