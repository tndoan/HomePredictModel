package org.object;

import java.util.HashMap;

public class UserObject {
	/**
	 * home location of user
	 */
	private PointObject location;
	
	/**
	 * true if the location is the actual home of user
	 * false if the location is needed to be inferred from data
	 */
	private boolean isKnownLocation;
	
	/**
	 * checkin map whose key is venue id, value is number of check-in that user has made in this venue
	 */
	private HashMap<String, Integer> checkinMap;
	
	/**
	 * id of user
	 */
	private String id;
	
	/**
	 * get how many check-in user has done in this venue
	 * @param vIds	venue id
	 * @return		number of check-in
	 */
	public int retrieveNumCks(String vIds){
		Integer num = checkinMap.get(vIds);
		if (num == null)
			return 0;
		else
			return num;
	}
	
	/**
	 * if {@link UserObject#isKnownLocation} is false, we update the home location of user to new one; otherwise, do nothing
	 * @param point	new home location of user
	 */
	public void updateLocation(PointObject point){
		if (!isKnownLocation){
			this.location = point;
		}
	}

	public PointObject getLocation() {
		return location;
	}

	public boolean isKnownLocation() {
		return isKnownLocation;
	}

	public String getId() {
		return id;
	}
	
	public UserObject(String id, PointObject location, boolean isKnownLocation, HashMap<String, Integer> checkinMap){
		this.id = id;
		this.location = location;
		this.isKnownLocation = isKnownLocation;
		this.checkinMap = checkinMap;
	}
}
