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
	
	public static double GaussDesnsity(double x) {
		return standardGau.density(x);
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
}
