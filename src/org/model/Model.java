package org.model;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.object.AreaObject;
import org.object.PointObject;
import org.object.UserObject;
import org.object.VenueObject;
import org.utils.Distance;
import org.utils.Function;
import org.utils.ReadFile;
import org.utils.Utils;

/**
 * 
 * @author tndoan
 *
 */
public class Model {
	
	/**
	 * this is used to indicate the function of winning of venue over its neighbors
	 * if true, it is modeled as sigmoid function
	 * else, use CDF function
	 */
	private boolean isSigmoid;
	
	/**
	 * key is venue id, value is venue object corresponding to the venue id
	 */
	private HashMap<String, VenueObject> venueMap;
	
	/**
	 * key is user id, value is user object corresponding to user id
	 */
	private HashMap<String, UserObject> userMap;
	
	/**
	 * key is the area id(same as venue id), value is area object 
	 */
	private HashMap<String, AreaObject> areaMap;
	
	/**
	 * list of user id whose home location are unknown
	 */
	private Set<String> unknownLocUsers;

	/**
	 * indicate which effects are used in model
	 * see ModeModel class for more details
	 */
	private int modeModel;
	
	public Model() {
		
	}
	
	public Model(HashMap<String, UserObject> userMap, HashMap<String, VenueObject> venueMap, 
			HashMap<String, AreaObject> areaMap, Set<String> unknownLocUsers, boolean isSigmoid, int modeModel){
		assert (modeModel == ModeModel.DISTANCE_AREAATTRACTION ||
				modeModel == ModeModel.NEIGHBORHOOD_COMPETITION ||
				modeModel == ModeModel.COMBINED);
		this.userMap = userMap;
		this.venueMap = venueMap;
		this.areaMap = areaMap;
		this.unknownLocUsers = unknownLocUsers;
		this.isSigmoid = isSigmoid;
		this.modeModel = modeModel;
	}
	
	/**
	 * construct model without home location of users so we use center of the mass as home locations of users
	 * @param venueLocFile
	 * @param cksFile
	 * @param isAverageLocation
	 * @param isSigmoid
	 * @param scale
	 * @param modeModel				indicate if neighborhood competition, area attraction or both are used in our model
	 */
	public Model(String venueLocFile, String cksFile, boolean isAverageLocation, boolean isSigmoid, double scale,
				 int modeModel) {
		//TODO need to test carefully before using
		assert (modeModel == ModeModel.DISTANCE_AREAATTRACTION ||
				modeModel == ModeModel.NEIGHBORHOOD_COMPETITION ||
				modeModel == ModeModel.COMBINED);
		this.isSigmoid = isSigmoid;
		this.modeModel = modeModel;
		
		// initialize 
		venueMap = new HashMap<>();
		userMap = new HashMap<>();
		unknownLocUsers = new HashSet<>();
		
		//read data from files
		HashMap<String, String> vInfo = ReadFile.readLocation(venueLocFile);
		HashMap<String, HashMap<String, Integer>> cksMap = ReadFile.readNumCksFile(cksFile);
		
		HashMap<String, ArrayList<String>> userOfVenueMap = Utils.collectUsers(cksMap);
		
		// make venue object
		HashMap<String, PointObject> vLocInfo = new HashMap<>();
		for (String vId : vInfo.keySet()) {
			PointObject p = new PointObject(vInfo.get(vId));
			vLocInfo.put(vId, p);
		}
		
		HashMap<String, Integer> countMap = Utils.countCks(cksMap);

		areaMap = new HashMap<>();
		venueMap = Utils.createNeighborsBox(vLocInfo, areaMap, countMap, userOfVenueMap, scale, isAverageLocation);
		
		// make user object
		Set<String> uSet = cksMap.keySet();
		for (String uId : uSet) {
			HashMap<String, Integer> checkinMap = cksMap.get(uId);
			PointObject uPoint = Utils.calculateCenterOfMass(checkinMap, vLocInfo);
			UserObject u = new UserObject(uId, uPoint, true, checkinMap);
			userMap.put(uId, u);
		}
	}
		
	public Model(String venueLocFile, String userLocFile, String cksFile, boolean isAverageLocation, boolean isSigmoid,
				 double scale, int modeModel){
		assert (modeModel == ModeModel.DISTANCE_AREAATTRACTION ||
				modeModel == ModeModel.NEIGHBORHOOD_COMPETITION ||
				modeModel == ModeModel.COMBINED);
		this.modeModel = modeModel;
		this.isSigmoid = isSigmoid;
		
		// initialize 
		venueMap = new HashMap<>();
		userMap = new HashMap<>();
		unknownLocUsers = new HashSet<>();
		
		// read data from files
		HashMap<String, String> vInfo = ReadFile.readLocation(venueLocFile);
		HashMap<String, String> uInfo = ReadFile.readLocation(userLocFile);
		HashMap<String, HashMap<String, Integer>> cksMap = ReadFile.readNumCksFile(cksFile);
		
		HashMap<String, ArrayList<String>> userOfVenueMap = Utils.collectUsers(cksMap);
		
		HashMap<String, PointObject> vLocInfo = new HashMap<>();
		for (String vId : vInfo.keySet()) {
			PointObject p = new PointObject(vInfo.get(vId));
			vLocInfo.put(vId, p);
		}
		
		// making user objects
		for(String userId : uInfo.keySet()){
			// parse the location of users 
			String locInfo = uInfo.get(userId);
			PointObject location = new PointObject(locInfo);
			
			boolean isKnownHome = true;
			if (locInfo.equals("?"))
				isKnownHome = false;
			
			HashMap<String, Integer> checkinMap = cksMap.get(userId);
			
			UserObject uo = new UserObject(userId, location, isKnownHome, checkinMap);
			userMap.put(userId, uo);
			
			// if location is unknow, we need to find it
			if (!isKnownHome)
				unknownLocUsers.add(userId);
		}
		
		HashMap<String, Integer> countMap = Utils.countCks(cksMap);

		areaMap = new HashMap<>();
		venueMap = Utils.createNeighborsBox(vLocInfo, areaMap, countMap, userOfVenueMap, scale, isAverageLocation);
	}
	
	
	public Set<String> getUnknownLocUsers() {
		return unknownLocUsers;
	}
	
	/**
	 * 
	 * @param checkinMode 1: use actual # of check-in; 2: log(# cks of user); 3: binary check-in
	 */
	public void learnParameter(int checkinMode){
		boolean conv = false;
		double prev_llh = calculateLLH();
		int iteration = 0;
		
		System.out.println("init LLH:" + prev_llh);
		Set<String> allVenues = venueMap.keySet();
		Set<String> validVenues = new HashSet<>();// only venue with some check-in will be added to this list
		
		for (String vId : allVenues) {
			VenueObject vo = venueMap.get(vId);
			if (vo.getUserIds() != null) // this venue have some visits from users
				validVenues.add(vId);
		}

		Set<String> allAreaId = areaMap.keySet(); 
		
		double llh = prev_llh;
		
		while (!conv) {
			// update location of users
			updateLocOfUsers(checkinMode);
//			double llh = calculateLLH();
//			System.out.println("after update loc of users: " + llh);
			System.out.println("after update loc of users: " + calculateLLH());
			
//			if (Math.abs(llh - prev_llh) < Params.threshold){
//				break; // convergence
//			}
//			
//			prev_llh = llh;
			
			// update the influence scope of venues
			HashMap<String, Double> updatedScope = new HashMap<>(); // the new scope of each venue
			
			// step 1: calculate the scope of each venue and then put them to updatedScope
//			for (String venueId : validVenues) {
			validVenues.parallelStream().forEach(venueId -> {				
				VenueObject vo = venueMap.get(venueId);
				
				double curScope = vo.getInfluenceScope();
//				System.out.println("------------");
				double scope = maximizeScopeOfVenue(venueId, curScope, checkinMode);
//				System.out.println("------------");
				updatedScope.put(venueId, scope);
			});
//			}
			
			// step 2: use new value to override old one
			validVenues.parallelStream().forEach(venueId -> {
				VenueObject v = venueMap.get(venueId);
				
				double scope = updatedScope.get(venueId);
				v.updateInfluenceScope(scope);
			});
			
			// step 3: update the scope of area
			allAreaId.parallelStream().forEach(areaId -> {
				AreaObject a = areaMap.get(areaId);
				Set<String> venues = a.getSetOfVenueIds();
				if (venues != null) {
					double new_scope = 0.0;
					for (String vId : venues) {
						double vScope = venueMap.get(vId).getInfluenceScope();
						new_scope += vScope * vScope;
					}
					// scope of area is the sum of scope of all venues inside
//					new_scope /= (double) venues.size();
					a.updateScope(Math.sqrt(new_scope));
				}
			});

			llh = calculateLLH();
			System.out.println("after update scope of venues:" + llh);
			
			// checking convergence
			if (iteration > 0 && Math.abs((prev_llh - llh)/llh) < Params.threshold) {
				conv = true;
			}
			
			prev_llh = llh;
			iteration++;
		}
	}
	
	/**
	 * @param checkinMode 1: use actual # of check-in; 2: log(# cks of user); 3: binary check-in
	 */
	public void updateLocOfUsers(int checkinMode) {
		unknownLocUsers.parallelStream().forEach((uId) -> {
			UserObject uo = userMap.get(uId);
			Set<String> venues = uo.getAllVenues();

			double numerator_x = 0.0; 
			double numerator_y = 0.0;
			double denominator = 0.0;

			for (String vId : venues) {
				VenueObject vo = venueMap.get(vId);
				String areaId = vo.getAreaId();
				AreaObject ao = areaMap.get(areaId);
				
				double numCks = 1.0;
				if (checkinMode == 1) 
					numCks = (double) uo.retrieveNumCks(vId);
				else if (checkinMode == 2) 
					numCks = Math.log((double) uo.retrieveNumCks(vId));
				
//				double weight = ((double) uo.retrieveNumCks(vId)) / (ao.getScope() * ao.getScope());
				double weight = numCks / (ao.getScope() * ao.getScope());
				numerator_x += weight * ao.getLocation().getLat();
				numerator_y += weight * ao.getLocation().getLng();
				denominator += weight;
			}
			
			uo.updateLocation(new PointObject(numerator_x / denominator, numerator_y /  denominator));
		});
	}

	/**
	 * 
	 * @param venueId
	 * @param sigma_v
	 * @param checkinMode 1: use actual # of check-in; 2: log(# cks of user); 3: binary check-in
	 * @return
	 */
	public double maximizeScopeOfVenue(String venueId, double sigma_v, int checkinMode) {
		VenueObject vObj = venueMap.get(venueId);
		ArrayList<String> neighbors = vObj.getNeighbors();
		String areaId = vObj.getAreaId();

		// \sigma_v is in all the area of their neighbors. so when we update sigma_v, the influence scope of areas which contain venue v are also
		// needed to be updated. Key of this map is id of area, venue is the square influence scope which does not contain venue v. 
		HashMap<String, Double> areaSourdingMap = new HashMap<>();
		
//		double sqSigma_v = sigma_v * sigma_v;
		double sqSigma_v = vObj.getInfluenceScope() * vObj.getInfluenceScope();
		for (String n : neighbors) {
			String nAreaId = venueMap.get(n).getAreaId();
			double currentScope = areaMap.get(nAreaId).getScope();
			double sqCurrentScope =  currentScope * currentScope;
			areaSourdingMap.put(n, sqCurrentScope - sqSigma_v);
		}
		double tempSqCurrentScope = areaMap.get(areaId).getScope() * areaMap.get(areaId).getScope();
		areaSourdingMap.put(venueId, tempSqCurrentScope - sqSigma_v);
		
		double t = 1.0;

		boolean inner_conv = false;
		boolean outter_conv = false;
		
		double preObj = - t * calculateLLH(venueId, sigma_v) - Math.log(sigma_v);
//		System.out.println(preObj);
		while (!outter_conv){
			int iter = 0;

			while (!inner_conv){
				// Step 1: calculate gradient calculation 
				double grad = grad(venueId, sigma_v, areaSourdingMap, t, checkinMode);
				
				double learningRate = 1.0; // it will be decreased using backtracking 
				double s = sigma_v - learningRate * grad;
				double lhs = - t * calculateLLH(venueId, s) - Math.log(s);
				double rhs = preObj - Params.alpha * learningRate * grad * grad;
				int inter_iter = 0; // if learning rate goes to far, it creates computational error
				while ((!Double.isFinite(lhs) || !Double.isFinite(rhs) || lhs > rhs) && inter_iter != 1000) {
					learningRate = Params.beta * learningRate;
					s = sigma_v - learningRate * grad;
					lhs = -t * calculateLLH(venueId, s) - Math.log(s);
					rhs = preObj - Params.alpha * learningRate * grad * grad;
					inter_iter++;
				}
				
				if (inter_iter == 1000) 
					break;
				
				sigma_v = sigma_v - learningRate * grad;
				
				// checking the convergence rate
				double llh = calculateLLH(venueId, sigma_v);
//				System.out.println(llh);
				double obj = - t * llh - Math.log(sigma_v);
//				System.out.println("pre_obj:" + preObj + " obj:" + obj + " sigma:" + sigma_v + " llh:" + (llh)+ " learningRate:" + learningRate);
				if (iter > 1 && Math.abs(obj - preObj) < Params.threshold) {
					inner_conv = true;
				}
				preObj = obj;
				iter++;
			}
//			System.out.println("===========");
//			if (1.0 / t < Params.threshold)
				outter_conv = true;
//			else {
//				t = 2.0 * t;
//				inner_conv = false; // continue using stochastic gradient descent to find the maximum
//			}
		}
		
		return sigma_v;
	}

	/*
	public double grad(String venueId, double sigma_v, double t) {
		VenueObject vObj = venueMap.get(venueId);
		String areaId = vObj.getAreaId();
		ArrayList<String> neighbors = vObj.getNeighbors();

		// \sigma_v is in all the area of their neighbors. so when we update sigma_v, the influence scope of areas which contain venue v are also
		// needed to be updated. Key of this map is id of area, venue is the square influence scope which does not contain venue v. 
		HashMap<String, Double> areaSourdingMap = new HashMap<>();
		
		double sqSigma_v = vObj.getInfluenceScope() * vObj.getInfluenceScope();
		for (String n : neighbors) {
			VenueObject nObj = venueMap.get(n);
			double currentScope = areaMap.get(nObj.getAreaId()).getScope();
			double sqCurrentScope =  currentScope * currentScope;
			areaSourdingMap.put(n, sqCurrentScope - sqSigma_v);
		}
		double tempSqCurrentScope = areaMap.get(areaId).getScope() * areaMap.get(areaId).getScope();
		areaSourdingMap.put(venueId, tempSqCurrentScope - sqSigma_v);
		
		return grad(venueId, sigma_v, areaSourdingMap, t);
	}
	*/
	
	/**
	 * 
	 * @param venueId
	 * @param sigma_v
	 * @param areaSurroundingMap
	 * @param t
	 * @param checkinMode 		1: use actual # of check-in; 2: log(# cks of user); 3: binary check-in
	 * @return
	 */
	public double grad(String venueId, double sigma_v, HashMap<String, Double> areaSurroundingMap, double t, int checkinMode) {
		double grad = 0.0;
		
		VenueObject vObj = venueMap.get(venueId);
		String areaId = vObj.getAreaId();
		double w_v = vObj.getTotalCks();
		ArrayList<String> neighbors = vObj.getNeighbors();
		
		ArrayList<String> users = vObj.getUserIds();
		if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.DISTANCE_AREAATTRACTION) {
			for (String userId : users) {
				UserObject uo = userMap.get(userId);

				double w = 1.0;
				if (checkinMode == 1)
					w = (double) uo.retrieveNumCks(venueId);
				else if (checkinMode == 2)
					w = Math.log((double) uo.retrieveNumCks(venueId));

				double d = Distance.calSqEuDistance(uo.getLocation(), areaMap.get(areaId).getLocation());
				double sq_sigma_v_prime = areaSurroundingMap.get(venueId) + sigma_v * sigma_v;
				grad += w * (-2.0 * sigma_v / sq_sigma_v_prime + sigma_v * d / (sq_sigma_v_prime * sq_sigma_v_prime));
			}
		}
		
		for (String neighbor : neighbors){
			VenueObject neighborObj = venueMap.get(neighbor);
			AreaObject neighborAreaObj = areaMap.get(neighborObj.getAreaId());
			double w_n = neighborObj.getTotalCks();

			if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.DISTANCE_AREAATTRACTION) {
				// second term of gradient
				double sq_sigma_n_prime = areaSurroundingMap.get(neighbor) + sigma_v * sigma_v;
				ArrayList<String> uOfNeighbors = neighborObj.getUserIds();
				if (uOfNeighbors == null)
					continue;
				for (String u : uOfNeighbors) {
					UserObject uo = userMap.get(u);

					double w_in = 1.0;
					if (checkinMode == 1)
						w_in = (double) uo.retrieveNumCks(neighbor);
					else if (checkinMode == 2)
						w_in = Math.log((double) uo.retrieveNumCks(neighbor));

					double d = Distance.calSqEuDistance(uo.getLocation(), neighborAreaObj.getLocation());
					grad += w_in * (-2.0 * sigma_v / sq_sigma_n_prime + sigma_v * d / (sq_sigma_n_prime * sq_sigma_n_prime));
				}
			}

			if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.NEIGHBORHOOD_COMPETITION) {
				// third term and 4th term of gradient
				double diff = sigma_v - neighborObj.getInfluenceScope();
				double p_vn = 0.0;
				double p_nv = 0.0;
				if (isSigmoid) { // sigmoid function
					p_vn = Function.diffLogSigmoidFunction(diff);
					p_nv = -Function.diffLogSigmoidFunction(-diff);
				} else { // CDF function
					p_vn = Function.diffLogCDF(diff);
					p_nv = -Function.diffLogCDF(-diff);
				}
				grad += w_v * p_vn + w_n * p_nv;
			}
		}
		
		grad = -t * grad;
		
		// 5th term of gradient : gradient of log
		grad -= 1.0 / sigma_v;
		return grad;
	}
	
	public double getInfluenceScope(String vId) {
		return venueMap.get(vId).getInfluenceScope();
	}
	
	/**
	 * Unused
	 * @param vId
	 * @param s
	 */
	public void updateInfScope(String vId, double s) {
		VenueObject vo = venueMap.get(vId);
		double sqOldvalue = vo.getInfluenceScope() * vo.getInfluenceScope();
		vo.updateInfluenceScope(s);
		ArrayList<String> neighbors = venueMap.get(vId).getNeighbors();
		for (String neighbor : neighbors) {
			VenueObject nObj = venueMap.get(neighbor);
			AreaObject ao = areaMap.get(nObj.getAreaId());
			ao.updateScope(Math.sqrt(ao.getScope() * ao.getScope() - sqOldvalue + s * s));
		}
		
		AreaObject ao = areaMap.get(vo.getAreaId());
		ao.updateScope(Math.sqrt(ao.getScope() * ao.getScope() - sqOldvalue + s * s));
	}
	
	public double calculateLLH() {
		return Loglikelihood.calculateLLH(userMap, venueMap, areaMap, isSigmoid, modeModel);
	}
	
	public double calculateLLH(String venueId, double sigma_v) {
		return Loglikelihood.calculateLLH(userMap, venueMap, areaMap, isSigmoid, venueId, sigma_v, modeModel);
	}
	
	/**
	 * 
	 * @param userLocFname
	 * @param venueScopeFname
	 * @throws IOException
	 */
	public void saveResult(String userLocFname, String venueScopeFname) throws IOException {
		// write location of users
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(userLocFname), "utf-8"));
		    for (String uId : unknownLocUsers) {
		    	PointObject uo = userMap.get(uId).getLocation();
		    	writer.write(uId + "," + uo.getLat() + "," + uo.getLng() + "\n");
		    }
		} catch (IOException ex) {
		  // report
		} finally {
		   writer.close();
		}
		
		// write scope of venues to file
		writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(venueScopeFname), "utf-8"));
		    for (String vId : venueMap.keySet()) {
		    	double scope = venueMap.get(vId).getInfluenceScope();
		    	writer.write(vId + "," + scope + "\n");
		    }
		} catch (IOException ex) {
		  // report
		} finally {
		   writer.close();
		}
	}
	
	public void printInfluenceScope() {
		for (VenueObject v : venueMap.values()) {
			System.out.println("venue id:\t" + v.getId() + "\tinfluence scope:" + v.getInfluenceScope());
		}
	}
	
	public void printInfluenceScope(String fname) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		ArrayList<String> result = new ArrayList<>();
		for (VenueObject v : venueMap.values()) {
			result.add(v.getId() + "," + v.getInfluenceScope());
		}
		Utils.writeFile(result, fname);
	}
	
	public void printInfScopeVenueArea(String fname) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		ArrayList<String> result = new ArrayList<>();
		for (VenueObject v : venueMap.values()) {
			String aId = v.getAreaId();
			AreaObject aObj = areaMap.get(aId);
			result.add(v.getId() + "," + v.getInfluenceScope() + "," + aObj.getScope());
		}
		Utils.writeFile(result, fname);
	}

	/**
	 * 
	 * @param userId
	 * @return
	 */
	public PointObject getUserLoc(String userId) {
		return userMap.get(userId).getLocation();
	}
	
	/**
	 * save all result for later use in JSON format
	 * @param fname filename
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public void saveResult(String fname) throws UnsupportedEncodingException, FileNotFoundException, IOException {		
		// save area info
		ArrayList<String> aString = new ArrayList<>();
		for (AreaObject ao : areaMap.values()) {
			String id = ao.getId();
			String scope = String.valueOf(ao.getScope());
			PointObject loc = ao.getLocation();
			String lat = String.valueOf(loc.getLat());
			String lng = String.valueOf(loc.getLng());
//			sb.append("\"" + id + "\" : { \"scope\":" + scope +  ",\"lat\":" + lat + ", \"lng\" :" + lng + "}");
			aString.add(id + "," + scope +  "," + lat + "," + lng);
		}
		Utils.writeFile(aString, fname + "_area");		
		
		// save venue scope, neighbors and area which it belong to
		ArrayList<String> vString = new ArrayList<>();
		for (VenueObject vo : venueMap.values()) {			
			String id = vo.getId();
			ArrayList<String> neighbors = vo.getNeighbors();
			String aId = vo.getAreaId();
			String scope = String.valueOf(vo.getInfluenceScope());
//			sb.append("\"").append(id).append("\" : { \"aId\" :").append(aId).append(", \"scope\":").append(scope)
//				.append(", \"neighbors\":[");
			vString.add(id + "," + aId + "," + scope );
			StringBuffer sb = new StringBuffer();
			boolean isB = true;
			for (String neighbor : neighbors) {
				if(isB) isB = false;
				else sb.append(",");
				sb.append(neighbor);
			}
			vString.add(sb.toString());
		}
		Utils.writeFile(vString, fname + "_venue");
		
		// save user location
//		ArrayList<String> uString = new ArrayList<>();
//		for (UserObject uo : userMap.values()) {
//			String id = uo.getId();
//			PointObject home = uo.getLocation();
//			String lat = String.valueOf(home.getLat());
//			String lng = String.valueOf(home.getLng());
//			sb.append("\"" + id + "\" : { \"lat\":" + lat + ", \"lng\" :" + lng + "}");
//		}
//		sb.append("}");
//		result.add(sb.toString());
//		Utils.writeFile(result, fname);
	}

	public VenueObject getVenueObj(String vId) {
		return this.venueMap.get(vId);
	}

	public AreaObject getAreaObj(String aId) {
		return this.areaMap.get(aId);
	}
}
