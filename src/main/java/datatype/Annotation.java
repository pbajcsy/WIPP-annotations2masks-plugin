package datatype;

import java.awt.Color;


import ij.gui.Roi;

/**
Disclaimer:  IMPORTANT:  This software was developed at the National Institute of Standards and Technology by employees of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the United States Code this software is not subject to copyright protection and is in the public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties, and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic. We would appreciate acknowledgement if the software is used. This software can be redistributed and/or modified freely provided that any derivative works bear some notice that they are derived from it, and any modified versions bear some notice that they have been modified.
*/
public class Annotation {
	public Roi shape;
	public String label;
	public String author;
	
	public static String RECTANGLE = new String("Rectangle");
	public static String CIRCLE = new String("Circle");
	public static String FREEHAND = new String("Freehand");
	
	public Annotation() {
		shape = new Roi(0,0,100,100);
		shape.setFillColor(Color.BLACK);
		shape.setName(RECTANGLE);
		label = new String("");
		author = new String("");
	}
	
	public Annotation(Roi r, String s) {
		shape = r;
		label = s;
		author = new String("");
	}
	

	public Annotation(Annotation a) {
		this.shape = a.shape;
		this.label = a.label;
		this.author = a.author;
	}

	public void setFillColor(Color shapeColor) {
		shape.setFillColor(shapeColor);		
	}
	
	public Color getFillColor() {
		return shape.getFillColor();
	}

	public void setShapeName(String shapeName) {
		//sanity check
		if(shapeName == null){
			System.err.println("ERROR: setShapeName is null");
			return;
		}
		if(shapeName.equalsIgnoreCase(RECTANGLE)){
			shape.setName(RECTANGLE);
			return;
		}
		if(shapeName.equalsIgnoreCase(CIRCLE)){
			shape.setName(CIRCLE);
			return;
		}
		if(shapeName.equalsIgnoreCase(FREEHAND)){
			shape.setName(FREEHAND);
			return;
		}
		
		System.err.println("ERROR: setShapeName failed since the shape string is not recognized = " + shapeName);
	
	}
	
	public String getShapeName() {
		return shape.getName();
	}
	
	
	public Color getGrayscaleFromFillColor() {
		int gray = (shape.getFillColor().getRed() + shape.getFillColor().getGreen() + shape.getFillColor().getBlue())/3;
		return new Color(gray, gray, gray);
	}
	
	public String toString() {
		return ("shape=" + this.shape.toString() + ", color " + this.shape.getFillColor().toString() +
				"\n annotationText= " + this.label + ", author =" + this.author);
	}
}
