package org.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.model.Model;
import org.object.AreaObject;
import org.object.PointObject;
import org.object.UserObject;
import org.object.VenueObject;
import org.utils.Distance;
import org.utils.Function;
import org.utils.Utils;

public class Evaluation {	
	private int numUsers;
	
	private int numVenues;
	
	private boolean isSigmoid;
	
	private boolean isAverageLoc;
	
	private double numCksEachUser;
	
	private HashMap<String, HashMap<String, Integer>> cksMap;
	private HashMap<String, PointObject> userLoc;
	private HashMap<String, PointObject> venueLoc;
	private HashMap<String, Double> venueScope;
	HashMap<String, ArrayList<String>>  neighbors;
	HashMap<String, AreaObject> areaMap;
	
	private double threshold;
	
	public Evaluation(int numUsers, int numVenues, int numCksEachUser, double threshold, boolean isSigmoid, boolean isAverageLoc){
		this.numUsers = numUsers;
		this.numVenues = numVenues;
		this.isSigmoid = isSigmoid;
		this.isAverageLoc = isAverageLoc;
		this.threshold = threshold;
		this.numCksEachUser = (double) numCksEachUser;
	}
	
	public void createSyntheticData() {
		Random r = new Random();
		double max_x = 1.305066; double min_y = 103.818895;
		double min_x = 1.272307; double max_y = 103.860973;
		
		userLoc = new HashMap<>();
		venueLoc = new HashMap<>();
		venueScope = new HashMap<>();
		
		// init location of users
		for (int i = 0; i < numUsers; i++) {
			double x = min_x + (max_x - min_x) * r.nextDouble();
			double y = min_y + (max_y - min_y) * r.nextDouble();
			PointObject p = new PointObject(x, y);
			userLoc.put(String.valueOf(i), p);
		}
		
		// init location and scope of venues
		for (int i = 0; i < numVenues; i++) {
			double x = min_x + (max_x - min_x) * r.nextDouble();
			double y = min_y + (max_y - min_y) * r.nextDouble();
			PointObject p = new PointObject(x, y);
			venueLoc.put(String.valueOf(i), p);
			double scope = 50 + (100.0 - 50.0) * r.nextDouble();
			venueScope.put(String.valueOf(i), scope);
		}
		
		// calculate the neighbors 
		neighbors = new HashMap<>();
		for (int i = 0; i < numVenues; i++) {
			PointObject p1 = venueLoc.get(String.valueOf(i));
			for (int j = i+1; j < numVenues; j++) {
				System.out.println(i + ":" + j);
				PointObject p2 = venueLoc.get(String.valueOf(j));
				double d = Distance.calculateDistance(p1, p2);
				if (d < threshold) {
					// neighbors if distance between 2 venues are not >= 100 meter
					ArrayList<String> n_i = neighbors.get(String.valueOf(i));
					ArrayList<String> n_j = neighbors.get(String.valueOf(j));
					if (n_i == null) {
						n_i = new ArrayList<>();
						neighbors.put(String.valueOf(i), n_i);
					}
					n_i.add(String.valueOf(j));
					if (n_j == null) {
						n_j = new ArrayList<>();
						neighbors.put(String.valueOf(j), n_j);
					}
					n_j.add(String.valueOf(i));
				}
			}
		}
		
		// create area map
		areaMap = new HashMap<>();
		for (int i = 0; i < numVenues; i++) {
			ArrayList<String> ns = neighbors.get(String.valueOf(i));
			PointObject p = venueLoc.get(String.valueOf(i ));
			double lat = p.getLat(); double lng = p.getLng();
			double scope = venueScope.get(String.valueOf(i));
			scope *= scope;
			
			if (isAverageLoc) {
				// if ns == null => venue has no neighbor, info of area = info of venue
				if (ns != null) {
					for (String n : ns) {
						PointObject pp = venueLoc.get(n);
						lat += pp.getLat();
						lng += pp.getLng();
					}
					lat /= (double) (ns.size() + 1);
					lng /= (double) (ns.size() + 1);
				} 
			}
			
			// if ns == null => venue has no neighbor, info of area = info of venue
			if (ns != null) {
				for (String n : ns) {
					double s = venueScope.get(n);
					scope += s * s;
				}
			}
			PointObject pa = new PointObject(lat, lng);
			AreaObject ao = new AreaObject(String.valueOf(i), Math.sqrt(scope / (double) (ns.size() + 1)), pa);
			areaMap.put(String.valueOf(i), ao);
		}
		
		// create check-in. Each user has numCksEachUser check-in and it will be distributed among venues
		HashMap<String, HashMap<String, Double>> probCksMap = new HashMap<>();
		for (int i = 0; i < numUsers; i++) {
			PointObject pu = userLoc.get(String.valueOf(i));
			HashMap<String, Double> eachVenueMap = probCksMap.get(String.valueOf(i));
			if (eachVenueMap == null){
				eachVenueMap = new HashMap<>();
				probCksMap.put(String.valueOf(i), eachVenueMap);
			}
			
			for (int j = 0; j < numVenues; j++) {
				AreaObject ao = areaMap.get(String.valueOf(j));
				PointObject pa = ao.getLocation();
				double areaScope = ao.getScope();
				double distance = Distance.calSqEuDistance(pu, pa);
				double probToArea = (1.0 / areaScope) * Math.exp(-distance / (2.0 * areaScope * areaScope));
				ArrayList<String> ns = neighbors.get(String.valueOf(j));
				
				double probWinsNeighbors = 1.0;
				double sigma_v = venueScope.get(String.valueOf(j));
				for (String n : ns) {
					double probWinsEachNeighbor;
					double sigma_n = venueScope.get(String.valueOf(n));
					
					if (isSigmoid) {
						probWinsEachNeighbor = Function.sigmoidFunction(sigma_v - sigma_n);
					} else {
						probWinsEachNeighbor = Function.GaussDesnsity(sigma_v - sigma_n);
					}
					
					probWinsNeighbors *= probWinsEachNeighbor;
				}
				
				double prob = probToArea * probWinsNeighbors; // probability that user i checkins in venue j
				eachVenueMap.put(String.valueOf(i), prob);
			}
		}
		
		cksMap = new HashMap<>();
		for (int i = 0; i < numUsers; i++) {
			HashMap<String, Double> eachVenue = probCksMap.get(String.valueOf(i));
			double total = 0.0;
			
			for (String v : eachVenue.keySet()) {
				total += eachVenue.get(v);
			}
			
			HashMap<String, Integer> cksEachVenue = new HashMap<>();
			cksMap.put(String.valueOf(i ), cksEachVenue);
			
			for (String v : eachVenue.keySet()) {
				int numCks = (int) (numCksEachUser * total / eachVenue.get(v));
				cksEachVenue.put(v, numCks);
			}
		}
	}
	
	public void writeDataToFile(String cksfilename, String uLocfname, String vLocfname, String vScopeFname) throws IOException {
		SaveData.writeHashMap(userLoc, uLocfname);
		SaveData.writeHashMap(venueLoc, vLocfname);
		SaveData.writeHashMap(venueScope, vScopeFname);
		SaveData.writeHashMapHashMap(cksMap, cksfilename);
	}
	
	public Model makeModel() {
		Random r = new Random();
		
		// construct user map and unknownLocUsers
		ArrayList<String> unknownLocUsers = new ArrayList<>();
		HashMap<String, UserObject> userMap = new HashMap<>();
		for (int i = 0; i < numUsers; i++) {
			String id = String.valueOf(i);
			double prob = r.nextDouble();
			boolean isKnownLocation = true;
			if (prob < 0.3) {
				isKnownLocation = false;
				unknownLocUsers.add(id);
			}
			HashMap<String, Integer> checkinMap = cksMap.get(id);
			UserObject uo = new UserObject(id, userLoc.get(id), isKnownLocation, checkinMap);
			userMap.put(id, uo);
		}
		
		// construct venue map
		HashMap<String, Integer> countMap = Utils.countCks(cksMap);
		HashMap<String, ArrayList<String>> userOfVenueMap = Utils.collectUsers(cksMap);
		HashMap<String, VenueObject> venueMap = new HashMap<>();
		for (int i = 0; i < numVenues; i++) {
			String id = String.valueOf(i);
			//parse the location of venues
			PointObject location = venueLoc.get(id);
			
			ArrayList<String> neighborIds = neighbors.get(id);
			
			int numCks = countMap.get(id);
			
			ArrayList<String> listOfUsers = userOfVenueMap.get(id);
			
			VenueObject vo = new VenueObject(id, numCks, location, neighborIds, listOfUsers);
			
			venueMap.put(id, vo);
		}
		
		// construct area map. It is already done
		 
		return new Model(userMap, venueMap, areaMap, unknownLocUsers, isSigmoid);
	}
}
