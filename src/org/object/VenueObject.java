package org.object;

import java.util.ArrayList;

/**
 * 
 * @author tndoan
 *
 */
public class VenueObject {
	
	public VenueObject(String id, int totalCks,	PointObject location, ArrayList<String> neighbors, ArrayList<String> userIds){
		this.id = id;
		this.influenceScope = 5.0;
		this.location = location;
		this.neighbors = neighbors;
		this.userIds = userIds;
		this.totalCks = totalCks;
	}
	
	/**
	 * total number of check-in that it has
	 */
	private int totalCks;
	
	/**
	 * influence scope
	 */
	private double influenceScope;
	
	/**
	 * location of venue
	 */
	private PointObject location;
	
	/**
	 * id of venue
	 */
	private String id;
	
	/**
	 * list of id of neighbors
	 */
	private ArrayList<String> neighbors;
	
	/**
	 * list of user ids who have check-in in this venue
	 */
	private ArrayList<String> userIds;

	public double getInfluenceScope() {
		return influenceScope;
	}

	public PointObject getLocation() {
		return location;
	}

	public String getId() {
		return id;
	}

	public ArrayList<String> getNeighbors() {
		return neighbors;
	}

	public ArrayList<String> getUserIds() {
		return userIds;
	}

	public void updateInfluenceScope(double scope){
		this.influenceScope = scope;
	}

	public int getTotalCks() {
		return totalCks;
	}
	
}
