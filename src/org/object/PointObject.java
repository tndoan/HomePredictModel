package org.object;

/**
 * all of point object
 * @author tndoan
 *
 */
public class PointObject {
	/**
	 * latitude of point object
	 */
	private double lat;
	
	/**
	 * longitude of point object
	 */
	private double lng;
	
	/**
	 * constructor
	 * @param lat	latitude
	 * @param lng	longitude
	 */
	public PointObject(double lat, double lng){
		this.lat = lat;
		this.lng = lng;
	}

	/**
	 * get latitude of point
	 * @return	latitude
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * get longitude of point
	 * @return	longitude
	 */
	public double getLng() {
		return lng;
	}
	
	/**
	 * to String
	 */
	public String toString(){
		return "latitude:" + lat + "\t longitude:" + lng;
	}
	
}
