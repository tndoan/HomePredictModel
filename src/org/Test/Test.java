package org.Test;

import org.model.Model;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Model m = new Model("data/vLoc.txt", "data/uLoc.txt", "data/cks.txt",
				"data/neighbors.txt", true, true);

		// double g1 = -m.calculateLLH("2", 1.0 + 0.00001) - Math.log(1.0 +
		// 0.00001);
		// double g2 = -m.calculateLLH("2", 1.0 - 0.00001) - Math.log(1.0 -
		// 0.00001);
		// double g = m.grad("2", 1.0, 1.0);
		//
		// System.out.println(g);
		// System.out.println(( g1 - g2) / (2 * 0.00001) );

		// System.out.println(m.calculateLLH());
		// double x = m.maximizeScopeOfVenue("1", 1.0);
		// m.updateInfScope("1", x);
		// System.out.println(m.calculateLLH());
		m.learnParameter();
		// m.updateInfScope("6", 13.5);
		// System.out.println(m.calculateLLH());
		//
		// m.updateInfScope("6", 11.83);
		// System.out.println(m.calculateLLH());

		// System.out.println(m.calculateLLH("6", s));
		// m.updateLocOfUsers();
		// System.out.println(m.calculateLLH());
		// System.out.println(m.calculateLLH("6", s));

	}

}
