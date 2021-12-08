package org.diskproject.client.components.brain;

public class BrainConfigLine {
	public String name;
	public float pval;
	public float[] color;
	
	public BrainConfigLine(String name, float pval, float[] color) {
		this.name = name;
		this.pval = pval;
		this.color = color;
	}
}
