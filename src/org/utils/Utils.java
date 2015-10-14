package org.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Utils {
	/**
	 * 
	 * @param cksMap
	 * @return
	 */
	public static HashMap<String, Integer> countCks(HashMap<String, HashMap<String, Integer>> cksMap){
		HashMap<String, Integer> result = new HashMap<>();
		
		for (String userId : cksMap.keySet()){
			HashMap<String, Integer> map = cksMap.get(userId);
			for (String venueId : map.keySet()){
				int numCks = map.get(venueId);
				Integer currentCks = result.get(venueId);
				if (currentCks == null){
					currentCks = 0;
				} 
				result.put(venueId, numCks + currentCks);
			}
		}
		
		return result;
	}

	/**
	 * 
	 * @param cksMap
	 * @return hash map whose key is venue id, value is list of user id who have check-in in this venue
	 */
	public static HashMap<String, ArrayList<String>> collectUsers(HashMap<String, HashMap<String, Integer>> cksMap){
		HashMap<String, ArrayList<String>> result = new HashMap<>();
		
		for (String userId : cksMap.keySet()){
			HashMap<String, Integer> map = cksMap.get(userId);
			
			for (String venueId : map.keySet()){
				Integer numCks = map.get(venueId);
				if (numCks == null){
					System.err.println("Bug here");
					continue;
				}
				
				ArrayList<String> listOfUId = result.get(venueId);
				if (listOfUId == null){
					listOfUId = new ArrayList<>();
					result.put(venueId, listOfUId);
				}
				
				listOfUId.add(userId);
			}
		}
		
		return result;
	}
	
	/**
	 * Round up number up to the places-th behind the point
	 * For example, 11.56 => 11.6
	 * @param value		value we want to round up
	 * @param places	location behind the point that we want to round up
	 * @return			round up value
	 */
	public static double roundUp(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    double factor = Math.pow(10, places);
	    value = value * factor;
	    double tmp = Math.ceil(value);
	    return tmp / factor;
	}
	
	/**
	 * Round down number up to the places-th behind the point
	 * Eg: 11.56 => 11.5
	 * @param value		value we want to round down
	 * @param places	location behind the point that we want to round down
	 * @return
	 */
	public static double roundDown(double value, int places){
		if (places < 0) throw new IllegalArgumentException();

	    double factor = Math.pow(10, places);
	    value = value * factor;
	    double tmp = Math.floor(value);
	    return tmp / factor;
	}
}
