package org.object;

public class AreaObject {
	
	private PointObject location;
	
	private String id;
	
	private double scope;
	
	public AreaObject(String id, double scope, PointObject location){
		this.id = id;
		this.scope = scope;
		this.location = location;
	}

	public PointObject getLocation() {
		return location;
	}

	public String getId() {
		return id;
	}

	public double getScope() {
		return scope;
	}
	
	public void updateScope(double s){
		this.scope = s;
	}
}
