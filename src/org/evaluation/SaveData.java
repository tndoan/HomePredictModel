package org.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.object.PointObject;

public class SaveData {
	public static <T> void writeHashMap(HashMap<String, T> map,
			String filename) throws IOException {
		BufferedWriter output = null;
		try {
			File file = new File(filename);
			output = new BufferedWriter(new FileWriter(file));
			for (String key : map.keySet()) {
				Object value = map.get(key);
				if (value instanceof PointObject) {
					PointObject p = (PointObject) value;
					output.write(key + "," + Double.toString(p.getLat()) + "," + Double.toString(p.getLng()) + "\n");
				} else 
					output.write(key + "," + String.valueOf(value) + "\n"); // for example, scope of venue
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null)
				output.close();
		}
	}
	
	public static void writeHashMapHashMap(HashMap<String, HashMap<String, Integer>> map, String filename) throws IOException {
		BufferedWriter output = null;
		try {
			File file = new File(filename);
			output = new BufferedWriter(new FileWriter(file));
			for (String key : map.keySet()) {
				HashMap<String, Integer> submap = map.get(key);
				for (String k : submap.keySet()) {
					output.write(key + "," + k + "," + submap.get(k).toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null)
				output.close();
		}
		
	}
}
