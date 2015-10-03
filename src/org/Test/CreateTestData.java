package org.Test;

import java.io.IOException;

import org.evaluation.Evaluation;
import org.model.Model;

public class CreateTestData {

	public static void main(String[] args) throws IOException {
		Evaluation eval = new Evaluation(20, 100, 100, 500.0, true, true);
		eval.createSyntheticData();
//		eval.writeDataToFile("cks.txt", "uLoc.txt", "vLoc.txt", "vScope.txt");
//		
//		Model m = eval.makeModel();
//		m.learnParameter();
//		m.saveResult("result_userLoc.txt", "result_venueScope.txt");
		
	}
}
