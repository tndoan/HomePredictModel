package org.utils;

import org.apache.commons.math3.distribution.NormalDistribution;

public class Function {
	
	private static NormalDistribution standardGau = new NormalDistribution();
	
	/**
	 * calculate the sigmoid function of x
	 * @param x
	 * @return
	 */
	public static double sigmoidFunction(double x) {
		double result = 1.0 / ( 1.0 + Math.exp(-x));
		return result;
	}
	
	/**
	 * taking differentiation of Sigmoid function at one point.
	 * Should be careful if the parameter is -x
	 * @param x
	 * @return
	 */
	public static double diffSigmoidFunction(double x) {
		double s = sigmoidFunction(x);
		return s * ( 1 - s );
	}
	
	/**
	 * taking differentiation of cdf of standard Gaussian distribution
 	 * Should be careful if the parameter is -x
	 * @param x	
	 * @return
	 */
	public static double diffCDF(double x) {
		return standardGau.density(x) / standardGau.cumulativeProbability(x);
	}
	
	/**
	 * return cumulative density function for a specific value
	 * @param x	data point that we want to compute
	 * @return
	 */
	public static double cdf(double x) {
		return standardGau.cumulativeProbability(x);
	}
}
