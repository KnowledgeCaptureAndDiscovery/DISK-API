package org.diskproject.client.components.brain;

public class MeshProperties {
	public String name, value, roi_key, filename;
	public float[] color;
	
	public MeshProperties(String name, float[] color, String value, String roi_key) {
		this.name = name;
		this.color = color;
		this.value = value;
		this.roi_key = roi_key;
	}
}
