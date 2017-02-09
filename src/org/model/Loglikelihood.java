package org.model;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.javafx.sg.prism.NGShape;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.object.AreaObject;
import org.object.UserObject;
import org.object.VenueObject;
import org.utils.Distance;
import org.utils.Function;

public class Loglikelihood {
	/**
	 * 
	 */
	private static NormalDistribution standardGau = new NormalDistribution();
	
	/**
	 * the total log-likelihood of the whole data
	 * @param userMap
	 * @param venueMap
	 * @param areaMap
	 * @param isSigmoid
	 * @param modeModel
	 * @return
	 */
	public static double calculateLLH(HashMap<String, UserObject> userMap, HashMap<String, VenueObject> venueMap, 
			HashMap<String, AreaObject> areaMap, boolean isSigmoid, int modeModel){
		double llh = 0;

		if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.DISTANCE_AREAATTRACTION) {
			// first component of log likelihood
			for (String userId : userMap.keySet()) {
				UserObject uo = userMap.get(userId);
				for (String venueId : venueMap.keySet()) {
					VenueObject vo = venueMap.get(venueId);
					String areaId = vo.getAreaId();
					AreaObject ao = areaMap.get(areaId);

					double distance = Distance.calSqEuDistance(uo.getLocation(), ao.getLocation());

					double w_iv = uo.retrieveNumCks(venueId);
					llh += w_iv * (-2.0 * Math.log(ao.getScope()) - distance / (2 * ao.getScope() * ao.getScope()));
				}
			}
		}

		if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.NEIGHBORHOOD_COMPETITION) {
			// second component of log likelihood
			for (String venueId : venueMap.keySet()) {
				VenueObject vo = venueMap.get(venueId);
				ArrayList<String> neighbors = vo.getNeighbors();

				double w_v = vo.getTotalCks();
				double w = 0.0;

				for (String vId : neighbors) {
					VenueObject neighbor = venueMap.get(vId);

					double diff = vo.getInfluenceScope() - neighbor.getInfluenceScope();
					double logdiff = 0.0;

					if (isSigmoid) {
						double sigmoid = Function.sigmoidFunction(diff);
						logdiff = Math.log(sigmoid);
					} else {
						double cdf = standardGau.cumulativeProbability(diff);
						logdiff = Math.log(cdf);
					}

					w += logdiff;
				}

				llh += w * w_v;
			}
		}
		
		return llh;
	}
	
	/**
	 * log likelihood of one venue. omit constant. Refer to report for more details
	 * @param venueId	id of venue we want to compute
	 * @param sigma_v	the influence scope of venue whose id is given. It is parameter because we want to test the loglikelihood.
	 * @param modeModel	indicate if neighborhood competition, area attraction or both is used in our model
	 * @return
	 */
	public static double calculateLLH(HashMap<String, UserObject> userMap, HashMap<String, VenueObject> venueMap, 
			HashMap<String, AreaObject> areaMap, boolean isSigmoid, String venueId, double sigma_v, int modeModel) {
		double llh = 0;
		
		VenueObject vo = venueMap.get(venueId);
		AreaObject ao = areaMap.get(vo.getAreaId());
		ArrayList<String> neighbors = vo.getNeighbors();

		if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.DISTANCE_AREAATTRACTION) {
			// calculate the first term
			double tempSqScope = ao.getScope() * ao.getScope() - vo.getInfluenceScope() * vo.getInfluenceScope() + sigma_v * sigma_v;
			ArrayList<String> users = vo.getUserIds();
			for (String uId : users) {
				UserObject uo = userMap.get(uId);

				double w = uo.retrieveNumCks(venueId);
				double d = Distance.calSqEuDistance(ao.getLocation(), uo.getLocation());
				llh += w * (-2.0 * Math.log(Math.sqrt(tempSqScope)) - d / (2.0 * tempSqScope));
			}

			// second term
			for (String nId : neighbors) { // loop over all neighbors of venue
				VenueObject no = venueMap.get(nId);
				ArrayList<String> nUsers = no.getUserIds();
				if (nUsers == null) { // this neighbor does not have any visits from users
					continue;
				}
				AreaObject na = areaMap.get(no.getAreaId()); // na = neighbor area
				// do this because we want to use the new value of sigma_v;
				// vo contains the old one
				double sqScope = na.getScope() * na.getScope() - vo.getInfluenceScope() * vo.getInfluenceScope() + sigma_v * sigma_v;
				for (String nUId : nUsers) {
					UserObject u = userMap.get(nUId);
					double w = u.retrieveNumCks(nId);
					double d = Distance.calSqEuDistance(u.getLocation(), na.getLocation());
					llh += w * (-2.0 * Math.log(Math.sqrt(sqScope)) - d / (2.0 * sqScope));
				}
			}
		}

		if (modeModel == ModeModel.COMBINED || modeModel == ModeModel.NEIGHBORHOOD_COMPETITION) {
			// third term
			double w_v = vo.getTotalCks();
			double c = 0.0;
//			double sigma_v = vo.getInfluenceScope();
			for (String neighbor : neighbors) {
				VenueObject no = venueMap.get(neighbor); // no = neighbor object
				if (isSigmoid) {
					c += Math.log(Function.sigmoidFunction(sigma_v - no.getInfluenceScope()));
				} else {
					c += Math.log(standardGau.cumulativeProbability(sigma_v - no.getInfluenceScope()));
				}
			}
			llh += w_v * c;

			// fourth term
			for (String neighbor : neighbors) {
				VenueObject no = venueMap.get(neighbor); // no = neighbor object
				double w = no.getTotalCks();
				if (isSigmoid) {
					llh += w * Math.log(Function.sigmoidFunction(no.getInfluenceScope() - sigma_v));
				} else {
					llh += w * Math.log(standardGau.cumulativeProbability(no.getInfluenceScope() - sigma_v));
				}
			}
		}
		
		return llh;
	}
}
