package org.model;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
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
	private ArrayList<String> unknownLocUsers;
	
	public Model() {
		
	}
	
	public Model(HashMap<String, UserObject> userMap, HashMap<String, VenueObject> venueMap, 
			HashMap<String, AreaObject> areaMap, ArrayList<String> unknowLocUsers, boolean isSigmoid){
		this.userMap = userMap;
		this.venueMap = venueMap;
		this.areaMap = areaMap;
		this.unknownLocUsers = unknowLocUsers;
		this.isSigmoid = isSigmoid;
	}
		
	public Model(String venueLocFile, String userLocFile, String cksFile, String neighborFile, boolean isAverageLocation, boolean isSigmoid){
		this.isSigmoid = isSigmoid;
		
		// initialize 
		venueMap = new HashMap<>();
		userMap = new HashMap<>();
		unknownLocUsers = new ArrayList<>();
		areaMap = new HashMap<>();
		
		// read data from files
		HashMap<String, String> vInfo = ReadFile.readLocation(venueLocFile);
		HashMap<String, String> uInfo = ReadFile.readLocation(userLocFile);
		HashMap<String, ArrayList<String>> neighbors = ReadFile.readNeighbors(neighborFile);
		HashMap<String, HashMap<String, Integer>> cksMap = ReadFile.readNumCksFile(cksFile);
		
		HashMap<String, ArrayList<String>> userOfVenueMap = Utils.collectUsers(cksMap);
		
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
		
		// make venue objects
		for (String venueId: vInfo.keySet()){
			
			//parse the location of venues
			String locInfo = vInfo.get(venueId);
			PointObject location = new PointObject(locInfo);
			
			ArrayList<String> neighborIds = neighbors.get(venueId);
			
			int numCks = countMap.get(venueId);
			
			ArrayList<String> listOfUsers = userOfVenueMap.get(venueId);
			
			VenueObject vo = new VenueObject(venueId, numCks, location, neighborIds, listOfUsers);
			
			venueMap.put(venueId, vo);
		}
		
		// make area objects
		for (String venueId : vInfo.keySet()) {
			VenueObject vo = venueMap.get(venueId);
			ArrayList<String> venueIds = vo.getNeighbors();
			
			double lat = vo.getLocation().getLat();
			double lng = vo.getLocation().getLng();
			double scope = vo.getInfluenceScope() * vo.getInfluenceScope();
			
			if (isAverageLocation) {
				for (String vId : venueIds){
					VenueObject v = venueMap.get(vId);
					lat += v.getLocation().getLat();
					lng += v.getLocation().getLng();
					scope += v.getInfluenceScope() * v.getInfluenceScope();
				}
				lat /= (double) (venueIds.size() + 1);
				lng /= (double) (venueIds.size() + 1);
			} else {
				for (String vId : venueIds){
					VenueObject v = venueMap.get(vId);
					scope += v.getInfluenceScope() * v.getInfluenceScope();
				}
			}
			
			scope /= (double) (venueIds.size() + 1);
			PointObject p = new PointObject(lat, lng);
			AreaObject a = new AreaObject(venueId, Math.sqrt(scope), p);
			areaMap.put(venueId, a);
		}
	}
	


	
	public ArrayList<String> getUnknownLocUsers() {
		return unknownLocUsers;
	}
	
	public void learnParameter(){
		boolean conv = false;
		double prev_llh = calculateLLH();
		int iteration = 0;
		
		System.out.println("init LLH:" + prev_llh);
		
		while (!conv) {
			// update location of users
			updateLocOfUsers();
			double llh = calculateLLH();
			System.out.println("after update loc of users: " + llh);
			
			if (Math.abs(llh - prev_llh) < Params.threshold){
				break; // convergence
			}
			
			prev_llh = llh;
			
			// update the influence scope of venues
			HashMap<String, Double> updatedScope = new HashMap<>(); // the new scope of each venue
			// step 1: calculate the scope of each venue and then put them to updatedScope
			for (String venueId : venueMap.keySet()) {
				double curScope = venueMap.get(venueId).getInfluenceScope();
				double scope = maximizeScopeOfVenue(venueId, curScope);
				updatedScope.put(venueId, scope);
			}
			// step 2: use new value to override old one
			for (String venueId : venueMap.keySet()){
				double scope = updatedScope.get(venueId);
				VenueObject v = venueMap.get(venueId);
				v.updateInfluenceScope(scope);
			}
			
			// checking convergence
			llh = calculateLLH();
			System.out.println("after update scope of users:" + llh);
			if (iteration > 0 && Math.abs(prev_llh - llh) < Params.threshold) {
				conv = true;
			}
			
			prev_llh = llh;
			iteration++;
		}
	}
	
	/**
	 * 
	 */
	public void updateLocOfUsers() {
		for (String uId : unknownLocUsers) {
			UserObject uo = userMap.get(uId);
			Set<String> venues = uo.getAllVenues();
			
			double numerator_x = 0.0; 
			double numerator_y = 0.0;
			double denominator = 0.0;
			
			for (String vId : venues) {
				AreaObject ao = areaMap.get(vId);
				double weight = ((double) uo.retrieveNumCks(vId)) / (ao.getScope() * ao.getScope()); 
				numerator_x += weight * ao.getLocation().getLat();
				numerator_y += weight * ao.getLocation().getLng();
				denominator += weight;
			}
			
			uo.updateLocation(new PointObject(numerator_x / denominator, numerator_y /  denominator));
		}
	}
	
	public double maximizeScopeOfVenue(String venueId, double sigma_v) {
		VenueObject vObj = venueMap.get(venueId);
		ArrayList<String> neighbors = vObj.getNeighbors();

		// \sigma_v is in all the area of their neighbors. so when we update sigma_v, the influence scope of areas which contain venue v are also
		// needed to be updated. Key of this map is id of area, venue is the square influence scope which does not contain venue v. 
		HashMap<String, Double> areaSourdingMap = new HashMap<>();
		
//		double sqSigma_v = sigma_v * sigma_v;
		double sqSigma_v = vObj.getInfluenceScope() * vObj.getInfluenceScope();
		for (String n : neighbors) {
			double currentScope = areaMap.get(n).getScope();
			double sqCurrentScope =  currentScope * currentScope;
			areaSourdingMap.put(n, sqCurrentScope - sqSigma_v);
		}
		double tempSqCurrentScope = areaMap.get(venueId).getScope() * areaMap.get(venueId).getScope();
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
				double grad = grad(venueId, sigma_v, areaSourdingMap, t);
				
				//TODO : have bug in backtracking
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
				double obj = - t * llh - Math.log(sigma_v);
//				System.out.println("pre_obj:" + preObj + " obj:" + obj + " sigma:" + sigma_v + " llh:" + (llh)+ " learningRate:" + learningRate);
				if (iter > 1 && Math.abs(obj - preObj) < Params.threshold) {
					inner_conv = true;
				}
				preObj = obj;
				iter++;
			}
//			System.out.println("===========");
			if (1.0 / t < Params.threshold)
				outter_conv = true;
			else {
				t = 2.0 * t;
				inner_conv = false; // continue using stochastic gradient descent to find the maximum
			}
		}
		
		return sigma_v;
	}
	
	public double grad(String venueId, double sigma_v, double t) {
		VenueObject vObj = venueMap.get(venueId);
		ArrayList<String> neighbors = vObj.getNeighbors();

		// \sigma_v is in all the area of their neighbors. so when we update sigma_v, the influence scope of areas which contain venue v are also
		// needed to be updated. Key of this map is id of area, venue is the square influence scope which does not contain venue v. 
		HashMap<String, Double> areaSourdingMap = new HashMap<>();
		
		double sqSigma_v = vObj.getInfluenceScope() * vObj.getInfluenceScope();
		for (String n : neighbors) {
			double currentScope = areaMap.get(n).getScope();
			double sqCurrentScope =  currentScope * currentScope;
			areaSourdingMap.put(n, sqCurrentScope - sqSigma_v);
		}
		double tempSqCurrentScope = areaMap.get(venueId).getScope() * areaMap.get(venueId).getScope();
		areaSourdingMap.put(venueId, tempSqCurrentScope - sqSigma_v);
		
		return grad(venueId, sigma_v, areaSourdingMap, t);
	}
	
	private double grad(String venueId, double sigma_v, HashMap<String, Double> areaSourdingMap, double t) {
		double grad = 0.0;
		
		VenueObject vObj = venueMap.get(venueId);
		double w_v = vObj.getTotalCks();
		ArrayList<String> neighbors = vObj.getNeighbors();
		
		ArrayList<String> users = vObj.getUserIds(); 
		for (String userId : users) {
			UserObject uo = userMap.get(userId);
			double w = uo.retrieveNumCks(venueId);
			double d = Distance.calSqEuDistance(uo.getLocation(), areaMap.get(venueId).getLocation());
			double sq_sigma_v_prime = areaSourdingMap.get(venueId) + sigma_v * sigma_v;
			grad += w * (-2.0 * sigma_v / sq_sigma_v_prime + sigma_v * d / (sq_sigma_v_prime * sq_sigma_v_prime));
		}
		
		for (String neighbor : neighbors){
			VenueObject neighborObj = venueMap.get(neighbor);
			AreaObject neighborAreaObj = areaMap.get(neighbor);
			double w_n = neighborObj.getTotalCks();
			
			// second term of gradient
			double sq_sigma_n_prime = areaSourdingMap.get(neighbor) + sigma_v * sigma_v;
			ArrayList<String> uOfNeighbors = neighborObj.getUserIds();
			for (String u : uOfNeighbors) {
				UserObject uo = userMap.get(u);
				double w_in = uo.retrieveNumCks(neighbor);
				double d = Distance.calSqEuDistance(uo.getLocation(), neighborAreaObj.getLocation());
				grad += w_in * (-2.0 * sigma_v / sq_sigma_n_prime + sigma_v *d / (sq_sigma_n_prime * sq_sigma_n_prime));
			}
			
			// third term and 4th term of gradient
			double diff = sigma_v - neighborObj.getInfluenceScope();
			double p_vn = 0.0; double p_nv = 0.0;
			if (isSigmoid){ // sigmoid function
				p_vn = Function.diffSigmoidFunction(diff);
				p_nv = -Function.diffSigmoidFunction(-diff);
			} else { // CDF function
				p_vn = Function.diffCDF(diff);
				p_nv = - Function.diffCDF(-diff);
			}
			grad += w_v * p_vn + w_n * p_nv;
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
	 * 
	 * @param vId
	 * @param s
	 */
	public void updateInfScope(String vId, double s) {
		VenueObject vo = venueMap.get(vId);
		double sqOldvalue = vo.getInfluenceScope() * vo.getInfluenceScope();
		vo.updateInfluenceScope(s);
		ArrayList<String> neighbors = venueMap.get(vId).getNeighbors();
		for (String neighbor : neighbors) {
			AreaObject ao = areaMap.get(neighbor);
			ao.updateScope(Math.sqrt(ao.getScope() * ao.getScope() - sqOldvalue + s * s));
		}
		
		AreaObject ao = areaMap.get(vId);
		ao.updateScope(Math.sqrt(ao.getScope() * ao.getScope() - sqOldvalue + s * s));
	}
	
	public double calculateLLH() {
		return Loglikelihood.calculateLLH(userMap, venueMap, areaMap, isSigmoid);
	}
	
	public double calculateLLH(String venueId, double sigma_v) {
		return Loglikelihood.calculateLLH(userMap, venueMap, areaMap, isSigmoid, venueId, sigma_v);
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
}
