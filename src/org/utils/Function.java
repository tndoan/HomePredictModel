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
	 * taking differentiation of log Sigmoid function at one point.
	 * Should be careful if the parameter is -x
	 * @param x		value
	 * @return		differentiation
	 */
	public static double diffLogSigmoidFunction(double x) {
		double s = sigmoidFunction(x);
		return (1.0 - s);
	}
	
	/**
	 * taking differentiation of log cdf of standard Gaussian distribution
 	 * Should be careful if the parameter is -x
	 * @param x		value
	 * @return		differentitaion
	 */
	public static double diffLogCDF(double x) {
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
