package io;
import java.awt.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;

import datatype.Annotation;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import loci.formats.FormatException;
import util.FileOper;
/* 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */


/**
 * This class takes a JSON file from WIPP 
 * with image annotations defined as a set of points defining a polygon or a rectangle or a circle
 * and associated textual labels and colors
 * it returns an array list of Annotation.java data structures
 * 
 * The class also contains several static methods for manipulating the annotation object
 *  
 * @author peter bajcsy 
 *
 */
public class AnnotationLoader {

	ArrayList<Annotation> _annotations = new ArrayList<Annotation>();
	private boolean _debug = false;

	public ArrayList<Annotation> getAnnotations(){
		return _annotations;
	}
	// reset all annotations
	private void removeAllAnnotations(){
		if( !_annotations.isEmpty()){
			while(_annotations.size() != 0){
				_annotations.remove(0);
			}			
		}
	}
	// this method is never used. it was only prototyped as an alternative approach
	public ArrayList<Annotation>  readJSONfromWIPP(String JSONfileName) throws IOException{
		
		removeAllAnnotations();
		
		// Create Json and print
		InputStream is = new FileInputStream(JSONfileName);
		String result = org.apache.commons.io.IOUtils.toString(is, "UTF-8");
		//System.out.println(result);

		JsonReader jsonReader = Json.createReader(new StringReader(result));
		//javax.json.JsonStructure test = jsonReader.read();
		//System.out.println("json struct = " + test.toString());
		//json = json.replaceAll("\r?\n", "");

		JsonObject jobj = jsonReader.readObject();        
		//System.out.println(jobj);
				
		JsonArray arrObj = jobj.getJsonArray("objects");
		for (int i = 0; i < arrObj.size(); i++)	{
		    String objType = arrObj.getJsonObject(i).getString("type");    
		    //System.out.println("objType i = " + i + " objType = " + objType);
		    
		    switch(objType){
		    case "rect":	    	
		    	addRectAnnotation(arrObj.getJsonObject(i));
		    	break;
		    case "circle":
		    	addCircleAnnotation(arrObj.getJsonObject(i));
		    	break;
		    case "path":
		      	addFreehandAnnotation(arrObj.getJsonObject(i));  	
		    	break;	    	
		    default:
		    	System.err.println("ERROR: unrecognized type =" + objType);
		    	break;
		 
		    
		    }
		}
		
		return _annotations;

	}
	/**
	 * This method extract info from the circle object
	 *  
	 * @param obj - JsonOvject of type "circle"
	 */
	private void addCircleAnnotation(JsonObject obj){
		
		//Setting up variables
		Annotation localAnnot = new Annotation();
		double x = 0;
		double y = 0;
		double height = 0;
		double width = 0;

		//String shape = "circle";
		
		//Get x coordinate of upper left corner of circle
		JsonNumber left = obj.getJsonNumber("left");
		if(left != null){
			x = obj.getJsonNumber("left").doubleValue();
		}

		//Get y coordinate of upper left corner of circle
		JsonNumber top = obj.getJsonNumber("top");
		if(top != null){
			y = obj.getJsonNumber("top").doubleValue();
		}

		//Get width of circle
		JsonNumber widthJ = obj.getJsonNumber("width");
		if(widthJ != null){
			width = obj.getJsonNumber("width").doubleValue();
		}

		//Get height of circle
		JsonNumber heightJ = obj.getJsonNumber("height");
		if(heightJ != null ){
			height = obj.getJsonNumber("height").doubleValue();
		}
		
		//Get fill color
		String temp = obj.getString("fill");
		temp = temp.replaceAll("[()]", "");
		temp = temp.replaceAll("rgba", "");
		String[] rgba = temp.split(",");
		float[] frgba = new float[rgba.length];
		int i = 0;
		for (String st : rgba) {
			float d = Float.parseFloat(st);
			frgba[i] = d/255;
			i++;
		}

		//Find coordinates of center of circle for drawing purposes
		double centerX = x-(width/2);
		double centerY = y-(height/2);

		//Create ROI and add to rois arraylist
		OvalRoi circle1 = new OvalRoi(centerX, centerY, width, height);
		circle1.setFillColor(new Color(frgba[0], frgba[1], frgba[2], frgba[3]));
		localAnnot.shape = (Roi)circle1.clone();
		localAnnot.setShapeName(Annotation.CIRCLE);
		
		//ArrayList<Roi> rois = new ArrayList<Roi>();
		
		JsonObject wdzt = obj.getJsonObject("wdzt");
		if(wdzt != null && !wdzt.isEmpty()){
			JsonObject labels = wdzt.getJsonObject("labels");
			if(labels != null && !labels.isEmpty()){
				// if textual annotation is missing then skip and use the default values ""
				String author = labels.getString("author");
				if(author != null && !author.isEmpty())
					localAnnot.author = author;
					
				String annotationText = labels.getString("annotationText");
				if(annotationText != null && !annotationText.isEmpty())
					localAnnot.label = annotationText;				
			}
			
			
		}
		
		_annotations.add(localAnnot);
		if(_debug)
			System.out.println("INFO: added annotation: circle = " + localAnnot.shape.toString() + "\n text=" + localAnnot.label);
		
	}

	/**
	 * This method extract info from the rectangle object
	 *  
	 * @param obj - JsonOvject of type "rect"
	 */
	private void addRectAnnotation(JsonObject obj){
		
		//Setting up variables
		Annotation localAnnot = new Annotation();
		double x = 0;
		double y = 0;
		double height = 0;
		double width = 0;

		//String shape = "circle";
		
		//Get x coordinate of upper left corner of circle
		JsonNumber left = obj.getJsonNumber("left");
		if(left != null){
			x = obj.getJsonNumber("left").doubleValue();
		}

		//Get y coordinate of upper left corner of circle
		JsonNumber top = obj.getJsonNumber("top");
		if(top != null){
			y = obj.getJsonNumber("top").doubleValue();
		}

		//Get width of circle
		JsonNumber widthJ = obj.getJsonNumber("width");
		if(widthJ != null){
			width = obj.getJsonNumber("width").doubleValue();
		}

		//Get height of circle
		JsonNumber heightJ = obj.getJsonNumber("height");
		if(heightJ != null ){
			height = obj.getJsonNumber("height").doubleValue();
		}
		
		//Get fill color
		String temp = obj.getString("fill");
		temp = temp.replaceAll("[()]", "");
		temp = temp.replaceAll("rgba", "");
		String[] rgba = temp.split(",");
		float[] frgba = new float[rgba.length];
		int i = 0;
		for (String st : rgba) {
			float d = Float.parseFloat(st);
			frgba[i] = d/255;
			i++;
		}

		// create the ROI 
		Roi rect1 = new Roi(x, y, width, height);
		rect1.setFillColor(new Color(frgba[0],frgba[1],frgba[2],frgba[3]));
		localAnnot.shape = (Roi)rect1.clone();
		localAnnot.setShapeName(Annotation.RECTANGLE);

		// extract textual annotations
		JsonObject wdzt = obj.getJsonObject("wdzt");
		if(wdzt != null && !wdzt.isEmpty()){
			JsonObject labels = wdzt.getJsonObject("labels");
			if(labels != null && !labels.isEmpty()){
				// if textual annotation is missing then skip and use the default values ""
				String author = labels.getString("author");
				if(author != null && !author.isEmpty())
					localAnnot.author = author;
					
				String annotationText = labels.getString("annotationText");
				if(annotationText != null && !annotationText.isEmpty())
					localAnnot.label = annotationText;				
			}	
		}
		_annotations.add(localAnnot);
		if(_debug)
			System.out.println("INFO: added annotation: rect = " + localAnnot.shape.toString() + "\n text=" + localAnnot.label);
		
	}

	/**
	 * This method extract info from the freehand object
	 *  
	 * @param obj - JsonOvject of type "path"
	 */
	private void addFreehandAnnotation(JsonObject obj){

		//Setting up variables
		Annotation localAnnot = new Annotation();
		double x = 0;
		double y = 0;
		double height = 0;
		double width = 0;

		//Get x coordinate of upper left corner of circle
		JsonNumber left = obj.getJsonNumber("left");
		if(left != null){
			x = obj.getJsonNumber("left").doubleValue();
		}

		//Get y coordinate of upper left corner of circle
		JsonNumber top = obj.getJsonNumber("top");
		if(top != null){
			y = obj.getJsonNumber("top").doubleValue();
		}

		//Get width of circle
		JsonNumber widthJ = obj.getJsonNumber("width");
		if(widthJ != null){
			width = obj.getJsonNumber("width").doubleValue();
		}

		//Get height of circle
		JsonNumber heightJ = obj.getJsonNumber("height");
		if(heightJ != null ){
			height = obj.getJsonNumber("height").doubleValue();
		}

		//Get fill color
		String temp = obj.getString("fill");
		temp = temp.replaceAll("[()]", "");
		temp = temp.replaceAll("rgba", "");
		String[] rgba = temp.split(",");
		float[] frgba = new float[rgba.length];
		int i = 0;
		for (String st : rgba) {
			float d = Float.parseFloat(st);
			frgba[i] = d/255;
			i++;
		}


		/////////////////////////// extract the free hand shape
		ArrayList<Float> xpoints = new ArrayList<Float>();
		ArrayList<Float> ypoints = new ArrayList<Float>();
		//Resetting all fields and lists used
		xpoints.clear();
		ypoints.clear();

		//End of list of coordinates marked with "z"
		boolean zFound = false;
		float val; 
		String character = new String();
		JsonArray bndpoints = obj.getJsonArray("path");
		for (int j = 0; !zFound && j < bndpoints.size(); j++)	{
			JsonArray pts = bndpoints.getJsonArray(j);
			//System.out.println("path j = " + j + " pts.get(0) = " + pts.get(0).toString());
			character = pts.get(0).toString().substring(1, pts.get(0).toString().length()-1);
			//System.out.println("path j = " + j + " character = " + character);
			switch(character){
			case "M":
				// beginning 
				val = Float.parseFloat(pts.get(1).toString());
				xpoints.add(val);
				val = Float.parseFloat(pts.get(2).toString());
				ypoints.add(val);
				break;
			case "Q":
				// middle 
				val = Float.parseFloat(pts.get(1).toString());
				xpoints.add(val);
				val = Float.parseFloat(pts.get(2).toString());
				ypoints.add(val);				
				break;
			case "L":
				// end
				val = Float.parseFloat(pts.get(1).toString());
				xpoints.add(val);
				val = Float.parseFloat(pts.get(2).toString());
				ypoints.add(val);	
				// exit after the last point or change the for loop upper limit to -1
				zFound = true;	
				break;				
			case "z":
				// finished with path
				zFound = true;	
				break;			
			}
		}

		//Create arrays from arraylist (toArray didn't seem to work so I had to manually create the array)
		i = 0;
		float[] xpoints1 = new float[xpoints.size()];
		for (Float f : xpoints) {
			xpoints1[i++] = f;
		}

		i = 0;
		float[] ypoints1 = new float[ypoints.size()];
		for (Float f : ypoints) {
			ypoints1[i++] = f;
		}

		//Create Freehand object Roi with arrays
		PolygonRoi freehand = new PolygonRoi(xpoints1, ypoints1, Roi.FREEROI);

		//Set color and add to rois arraylist
		freehand.setFillColor(new Color(frgba[0],frgba[1],frgba[2],frgba[3]));
		localAnnot.shape = (PolygonRoi)freehand.clone();
		localAnnot.setShapeName(Annotation.FREEHAND);

		// extract textual annotations
		JsonObject wdzt = obj.getJsonObject("wdzt");
		if(wdzt != null && !wdzt.isEmpty()){
			JsonObject labels = wdzt.getJsonObject("labels");
			if(labels != null && !labels.isEmpty()){
				// if textual annotation is missing then skip and use the default values ""
				String author = labels.getString("author");
				if(author != null && !author.isEmpty())
					localAnnot.author = author;

				String annotationText = labels.getString("annotationText");
				if(annotationText != null && !annotationText.isEmpty())
					localAnnot.label = annotationText;				
			}	
		}

		_annotations.add(localAnnot);
		if(_debug)
			System.out.println("INFO: added annotation: path = " + localAnnot.shape.toString() + "\n text=" + localAnnot.label);

	}

	/**
	 * This method extracts the unique textual labels over a set of annotations
	 * @param annotations - input annotations
	 * @return
	 */
	public static ArrayList<String> getUniqueLabels(ArrayList<Annotation> annotations) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return null;
		}
		ArrayList<String> labels = new ArrayList<>();
		int num = 0;
		for (Annotation a : annotations) {
			if (!labels.contains(a.label)) {
				System.out.println(a.label + ", " + a.getFillColor().toString());
				labels.add(a.label);
				num++;
			}
		}
		System.out.println("Number of unique labels = " + num);
		return labels;
	}

	
	public static boolean cleanupLabels(ArrayList<Annotation> annotations) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return false;
		}

		for (Annotation a : annotations) {
			// clean-up the annotations from , ;
			a.label = a.label.replaceAll("[\\p{Punct}&&[^.-]]", "_");
			a.label = a.label.replace(" ", "");
			// this is specific to concrete since Steve included some arbitrary commas in the text
			a.label = a.label.replace(",", "");
		}
		return true;
	}
	
	/**
	 * This method returns a list of unique colors
	 * @param annotations - input array of Annotations objects
	 * @return ArrayList<Color>
	 */
	public static ArrayList<Color> getUniqueColors(ArrayList<Annotation> annotations) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return null;
		}
		ArrayList<Color> unique = new ArrayList<>();
		int num = 0;
		for (Annotation a : annotations) {
			if (!unique.contains(a.shape.getFillColor())) {
				System.out.println(a.shape.getFillColor() + ", " + a.label);
				unique.add(a.shape.getFillColor());
				num++;
			}
		}
		System.out.println("Number of unique colors = " + num);
		return unique;

	}

	/**
	 * This method returns a list of unique shapes
	 * @param annotations - input array of Annotations objects
	 * @return ArrayList<String>
	 */
	public static ArrayList<String> getUniqueShapes(ArrayList<Annotation> annotations) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return null;
		}

		ArrayList<String> unique = new ArrayList<>();
		int num = 0;
		for (Annotation a : annotations) {
			//test
			
			//System.err.println("shape type = " + a.shape.getTypeAsString() );
			
		if (!unique.contains(a.shape.getName() )) {
				System.out.println("shape name = " + a.shape.getName());
				unique.add(a.shape.getName());
				num++;
			}
		}
		System.out.println("Number of unique shapes = " + num);
		return unique;

	}
	
	/**
	 * This method removes an element from an array list of Color objects
	 * based on the red, green, and blue values 
	 *  
	 * @param uniqueColors - array of Color objects
	 * @param toBeRemoved - Color object with RGB values defining the Color object to be removed 
	 */
	public static boolean removeColor(ArrayList<Color> uniqueColors, Color toBeRemoved) {
		// sanity check
		if (uniqueColors == null) {
			System.err.println("ERROR: missing uniqueColors");
			return false;
		}
		if (uniqueColors.size() < 1) {
			System.out.println("WARNING: empty uniqueColors");
			return false;
		}

		// unfortunately, the Color object cannot be removed by the function
		// uniqueColors.remove(membrane);
		// for a Color object instantiated via new Color(redValue, greenValue, blueValue);
		// the removal must key on the color
		int i = 0;
		int index = -1;
		for (Color unique : uniqueColors) {
			if (unique.getRed() == toBeRemoved.getRed() && unique.getGreen() == toBeRemoved.getGreen()
					&& unique.getBlue() == toBeRemoved.getBlue()) {
				index = i;
			}
			i++;
		}
		if (index != -1){
			uniqueColors.remove(index);
		}else{
			System.err.println("Did not find the color to be removed in the list");
			return false;
		}
		return true;
	}
	/**
	 * This method removes an element from an array list of Shape ROI objects
	 * based on the type value = {oval, rect, path, line} 
	 *  
	 * @param uniqueColors - array of Color objects
	 * @param toBeRemoved - Color object with RGB values defining the Color object to be removed 
	 */
	public static boolean removeShape(ArrayList<String> uniqueShape, String toBeRemoved) {
		// sanity check
		if (uniqueShape == null) {
			System.err.println("ERROR: missing uniqueShape");
			return false;
		}
		if (uniqueShape.size() < 1) {
			System.out.println("WARNING: empty uniqueShape");
			return false;
		}
		//sanity check
		if(toBeRemoved == null){
			System.err.println("ERROR: toBeRemoved is null");
			return false;
		}
		if(toBeRemoved.equalsIgnoreCase(Annotation.RECTANGLE) || toBeRemoved.equalsIgnoreCase(Annotation.CIRCLE) || 
				toBeRemoved.equalsIgnoreCase(Annotation.FREEHAND) ){
			return (uniqueShape.remove(toBeRemoved));			
		}
		
		System.err.println("ERROR: toBeRemoved is not a valid shape string  = " + toBeRemoved);
		return false;

	}
	
	/**
	 * This method counts the number of annotations with a unique color
	 * @param annotations - input list of Annotations
	 * @return HashMap<Color, Integer> 
	 */
	public static HashMap<Color, Integer> AnnotHistPerColor(ArrayList<Annotation> annotations){

		ArrayList<Color> uniqueColors = AnnotationLoader.getUniqueColors(annotations);
		HashMap<Color, Integer> hmap = new HashMap<Color,Integer>();
		Set<?> set = hmap.entrySet();
		Iterator<?> iterator = set.iterator();
		// initialize the HashMap
		for (Color unique : uniqueColors) {
			hmap.put(unique, 0);	

			if(iterator.hasNext()) {
				iterator.next();
			}
		}
		//System.out.println("initial HMap="+hmap);

		for (Annotation a : annotations) {
			Color currColor = a.shape.getFillColor();
			Iterator<?> it = hmap.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("unchecked")
				Map.Entry<Color, Integer> pair = (Map.Entry<Color, Integer>)it.next();
				Color col = (Color)pair.getKey();

				if (currColor.getRed() == col.getRed() && currColor.getGreen() == col.getGreen() && currColor.getBlue() == col.getBlue()) {
					Integer count = pair.getValue();
					count++;
					pair.setValue(count);
				}
				//System.out.println(pair.getKey() + " = " + pair.getValue());
				
			}
		}
		System.out.println("final Hist Color HMap="+hmap);
		return hmap;
	}
	/**
	 * This method counts the number of annotations with a unique textual annotation
	 * @param annotations - input list of Annotations
	 * @return HashMap<Color, Integer> 
	 */
	public static HashMap<String, Integer> AnnotHistPerLabel(ArrayList<Annotation> annotations){

		ArrayList<String> uniqueLabels = AnnotationLoader.getUniqueLabels(annotations);
		HashMap<String, Integer> hmap = new HashMap<String,Integer>();
		Set<?> set = hmap.entrySet();
		Iterator<?> iterator = set.iterator();
		// initialize the HashMap
		for (String unique : uniqueLabels) {
			hmap.put(unique, 0);	

			if(iterator.hasNext()) {
				iterator.next();
			}
		}
		//System.out.println("initial HMap="+hmap);

		for (Annotation a : annotations) {
			String currLabel = a.label;
			Iterator<?> it = hmap.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("unchecked")
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
				String col = (String)pair.getKey();

				if (currLabel.equalsIgnoreCase(col) ) {
					Integer count = pair.getValue();
					count++;
					pair.setValue(count);
				}
				//System.out.println(pair.getKey() + " = " + pair.getValue());
				
			}
		}
		System.out.println("final Hist Label HMap="+hmap);
		return hmap;
	}

	/**
	 * This method counts the number of annotations with a unique shape annotation
	 * @param annotations - input list of Annotations
	 * @return HashMap<String, Integer> 
	 */
	public static HashMap<String, Integer> AnnotHistPerShape(ArrayList<Annotation> annotations){

		ArrayList<String> uniqueShapes = AnnotationLoader.getUniqueShapes(annotations);
		HashMap<String, Integer> hmap = new HashMap<String,Integer>();
		Set<?> set = hmap.entrySet();
		Iterator<?> iterator = set.iterator();
		// initialize the HashMap
		for (String unique : uniqueShapes) {
			hmap.put(unique, 0);	

			if(iterator.hasNext()) {
				iterator.next();
			}
		}
		//System.out.println("initial HMap="+hmap);

		for (Annotation a : annotations) {
			String currShape = a.shape.getName();
			Iterator<?> it = hmap.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("unchecked")
				Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
				String col = (String)pair.getKey();

				if (currShape.equalsIgnoreCase(col) ) {
					Integer count = pair.getValue();
					count++;
					pair.setValue(count);
				}
				//System.out.println(pair.getKey() + " = " + pair.getValue());
				
			}
		}
		System.out.println("final Hist Shape HMap="+hmap);
		return hmap;
	}

	/**
	 * This method converts Color to name of the Color 
	 * @param col
	 * @return
	 */
	public static String mapColorValue2ColorName(Color col){
		
		if(col.equals(Color.BLACK)){
			return "BLACK";
		}
		if(col.equals(Color.RED)){
			return "RED";
		}
		if(col.equals(Color.GREEN)){
			return "GREEN";
		}
		if(col.equals(Color.BLUE)){
			return "BLUE";
		}
		if(col.equals(Color.YELLOW)){
			return "YELLOW";
		}
		if(col.equals(Color.CYAN)){
			return "CYAN";
		}
		if(col.equals(Color.PINK)){
			return "PINK";
		}
		if(col.equals(Color.DARK_GRAY)){
			return "DARK_GRAY";
		}
		if(col.equals(Color.GRAY)){
			return "GRAY";
		}
		if(col.equals(Color.LIGHT_GRAY)){
			return "LIGHT_GRAY";
		}
		if(col.equals(Color.MAGENTA)){
			return "MAGENTA";
		}
		if(col.equals(Color.ORANGE)){
			return "ORANGE";
		}
		if(col.equals(Color.WHITE)){
			return "WHITE";
		}
		
	
		if(col.getRed() == 255 && col.getGreen() == 0 && col.getBlue() == 0){
			return "Red";
		}
		if(col.getRed() == 0 && col.getGreen() == 255 && col.getBlue() == 0){
			return "Green";
		}
		if(col.getRed() == 0 && col.getGreen() == 0 && col.getBlue() == 255){
			return "Blue";
		}
		if(col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 0){
			return "Yellow";
		}
		if(col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 128){
			return "Light_Yellow";
		}
		if(col.getRed() == 255 && col.getGreen() == 0 && col.getBlue() == 255){
			return "Purple";
		}
		if(col.getRed() == 255 && col.getGreen() == 128 && col.getBlue() == 255){
			return "Light_Purple";
		}
		if(col.getRed() == 0 && col.getGreen() == 255 && col.getBlue() == 255){
			return "Cyan";
		}
		if(col.getRed() == 128 && col.getGreen() == 255 && col.getBlue() == 255){
			return "Light_Cyan";
		}
			
		String rgb_name = new String();
		rgb_name = "rgb_"+ col.getRed() + "_" + col.getGreen() + "_" + col.getBlue() + "_";
		return rgb_name;
		
	/*	System.out.println("Yellow: " + numYellow);
		System.out.println("Red: " + numRed);
		System.out.println("Orange: " + numOrange);
		System.out.println("Green: " + numGreen);
		System.out.println("Blue: " + numBlue);
		System.out.println("Pink: " + numPink);
		System.out.println("Purple: " + numPurple);*/
		
	}

	
	/**
	 * This method is for print all values in the annotations
	 * @param annotations
	 */
	public static void printArrayListAnnot(ArrayList<Annotation> annotations){
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return;
		}
		int idx = 0;
		for (Annotation a : annotations) {
			System.out.println("idx="+idx+":" + a.toString());
			idx++;
		}
		return;	
	}

	public static void printArrayListColor(ArrayList<Color> colors){
		// sanity check
		if(colors == null || colors.size() < 1){
			System.err.println("ERROR: missing Colors");
			return;
		}
		int idx = 0;
		for (Color a : colors) {
			String name = AnnotationLoader.mapColorValue2ColorName(a);
			System.out.println("idx="+idx+": name=" + name + ": value="+a.toString());
			idx++;
		}
		return;	
	}
	
	/**
	 * This method is specific to the project with Ronit Sharon-Frilling and her annotations
	 * 
	 * @param annotations - input array of annotations
	 * @param outFileName
	 */
	public static void saveCircleCenters(ArrayList<Annotation> annotations, String outFileName) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return;
		}
		if(outFileName == null){
			System.err.println("ERROR: missing outFileName");
			return;			
		}
		// estimate the number of CIRCLE objects
		HashMap<String, Integer> hashm = AnnotHistPerShape(annotations);
		Iterator<?> it = hashm.entrySet().iterator();
		int numberOfCircles = -1;
		boolean foundCircle = false;
		while (!foundCircle && it.hasNext()) {
			@SuppressWarnings("unchecked")
			Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
			String uniqueShape = (String)pair.getKey();

			if (uniqueShape.equalsIgnoreCase(Annotation.CIRCLE) ) {
				numberOfCircles = pair.getValue();
				foundCircle = true;
			}
			//System.out.println(pair.getKey() + " = " + pair.getValue());			
		}
		if(!foundCircle){
			System.out.println("WARNING: annotations do not contain circles");
			return;
		}

		// extract information from annotations
		double[][] table = new double [numberOfCircles][3];
		int index = 0;
		for (Annotation a : annotations) {
			if(a.shape.getName().equalsIgnoreCase(Annotation.CIRCLE)){
/*				System.out.println("Xbase=" + a.shape.getXBase());
				System.out.println("Ybase=" + a.shape.getYBase());
				System.out.println("FloatWidth=" + a.shape.getFloatWidth());
				System.out.println("FloatHeight=" + a.shape.getFloatHeight());*/
				//Find coordinates of center of circle for drawing purposes
				table[index][0] = a.shape.getXBase() + a.shape.getFloatWidth()/2; // centerX
				table[index][1] = a.shape.getYBase() + a.shape.getFloatHeight()/2; // centerY 
				table[index][2] = a.shape.getFloatWidth(); // radius
				index++;
			}
		}
		
		String[] header = new String [3];
		header[0] = new String("CenterX");
		header[1] = new String("CenterY");
		header[2] = new String("Radius");
		
		CsvMyWriter.SaveTable(header, table, outFileName);
		return;

	}
	/**
	 * Batch processing of a folder with annotations
	 * 
	 * @param inputFileFolder
	 * @param outFileFolder
	 * @throws IOException 
	 */
	public void saveFilteredObjectsBatch(String inputFileFolder, String filterObject, String outFileFolder) throws IOException {
		
		// sanity check
		if (inputFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null inputFileFolder or outFileFolder ");
			return;
		}
		if (filterObject == null ) {
			System.err.println("Error: null filterObject  ");
			return;
		}		
		// getting images to process
		Collection<String> dirFiles = FileOper.readFileDirectory(inputFileFolder);

		// select images with the right suffix
		String suffixFrom = new String(".json");
		Collection<String> dirSelectFiles = FileOper.selectFileType(dirFiles,suffixFrom );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectFiles,
				FileOper.SORT_ASCENDING);
			

		/////////////////////////////	
		String JSONfileName = new String();	
		String outFileName = new String();
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k
				.hasNext();) {
			JSONfileName = k.next();
			
			ArrayList<Annotation> res = this.readJSONfromWIPP(JSONfileName);
			// construct the output file name
			String name = (new File(JSONfileName)).getName();
			name = name.substring(0, name.length()-5) + ".csv";
			if(filterObject.equalsIgnoreCase("circle")){
				outFileName = new String(outFileFolder + File.separator + "Circles_" + name);
				System.out.println("outFileName="+outFileName);
				AnnotationLoader.saveCircleCenters(res, outFileName);
			}else{
				if(filterObject.equalsIgnoreCase("rectangle")){
                    outFileName = new String(outFileFolder + File.separator + "Rect_" + name);
					System.out.println("outFileName="+outFileName);
					AnnotationLoader.saveRectangle(res, outFileName);
				}else{
					System.err.println("Unrecognized filterObject = " + filterObject);
				}
				
			}
			
		}

	}
	
	/**
	 * This method is used for Concrete image analysis (Steve Feldman) and organelle analysis (Ronit Sharon-Frilling)
	 * 
	 * @param annotations - input array of annotations
	 * @param outFileName - CSV file with all rectangle information
	 */
	public static void saveRectangle(ArrayList<Annotation> annotations, String outFileName) {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return;
		}
		if(outFileName == null){
			System.err.println("ERROR: missing outFileName");
			return;			
		}
		// estimate the number of RECT objects
		HashMap<String, Integer> hashm = AnnotHistPerShape(annotations);
		Iterator<?> it = hashm.entrySet().iterator();
		int numberOfRect = -1;
		boolean foundRect = false;
		while (!foundRect && it.hasNext()) {
			@SuppressWarnings("unchecked")
			Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
			String uniqueShape = (String)pair.getKey();

			if (uniqueShape.equalsIgnoreCase(Annotation.RECTANGLE) ) {
				numberOfRect = pair.getValue();
				foundRect = true;
			}
			//System.out.println(pair.getKey() + " = " + pair.getValue());			
		}
		if(!foundRect){
			System.out.println("WARNING: annotations do not contain rectangles");
			return;
		}

		// extract information from annotations
		double[][] table = new double [numberOfRect][7];
		int index = 0;
		for (Annotation a : annotations) {
			if(a.shape.getName().equalsIgnoreCase(Annotation.RECTANGLE)){
/*				System.out.println("Xbase=" + a.shape.getXBase());
				System.out.println("Ybase=" + a.shape.getYBase());
				System.out.println("FloatWidth=" + a.shape.getFloatWidth());
				System.out.println("FloatHeight=" + a.shape.getFloatHeight());*/
				table[index][0] = a.shape.getXBase();
				table[index][1] = a.shape.getYBase();
				table[index][2] = a.shape.getFloatWidth();
				table[index][3] = a.shape.getFloatHeight();
				table[index][4] = a.shape.getFillColor().getRed();
				table[index][5] = a.shape.getFillColor().getGreen();
				table[index][6] = a.shape.getFillColor().getBlue();
				
				index++;
			}
		}
		
		String[] header = new String [7];
		header[0] = new String("Left OriginX");
		header[1] = new String("Left OriginY");
		header[2] = new String("Width");
		header[3] = new String("Height");
		header[4] = new String("Fill Color Red");
		header[5] = new String("Fill Color Green");
		header[6] = new String("Fill Color Blue");
		
		CsvMyWriter.SaveTable(header, table, outFileName);
		return;

	}
	/**
	 * @param args
	 * @throws IOException testing
	 * @throws FormatException 
	 */
	public static void main(String[] args) throws IOException, FormatException {
		// sanity check
		if(args == null || args.length < 2){
			System.err.println("expected two argumants: JSON file from WIPP and outFilename");
			return;			
		}

		String JSONfileName = args[0];
		System.out.println("JSONfileName="+JSONfileName);

		AnnotationLoader myClass = new AnnotationLoader();
		
		//ArrayList<Annotation> res = myClass.readJSONfromWIPP(JSONfileName);

     //		ArrayList<Annotation> res = myClass.parseJSONfromWIPP(JSONfileName);
		
		String outFileName = args[1];
		System.out.println("outFileName="+outFileName);
		//AnnotationLoader.saveCircleCenters(res, outFileName); 
		//String inputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations");
		//String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations\\testOutput");
		//String inputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-09-24\\version2");
		//String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-09-24\\version2\\testOutput");
		
		String inputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-10-14\\version1\\");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-10-14\\testOutput\\");
			
		String filterObject = new String("rectangle"); // new String("circle");
		myClass.saveFilteredObjectsBatch(inputFileFolder, filterObject, outFileFolder);
			
	/*	ArrayList<String> shapes = AnnotationLoader.getUniqueShapes(res);
		AnnotationLoader.getUniqueLabels(res);
		System.out.println("++++++++++++++++++++++++++");
		ArrayList<Color> colors = AnnotationLoader.getUniqueColors(res);
		AnnotationLoader.printArrayListColor(colors);
			
		
		System.out.println("++++++++++++++++++++++++++");
		HashMap<Color, Integer> hmapColor = AnnotationLoader.AnnotHistPerColor(res);
		HashMap<String, Integer> hmapLabel = AnnotationLoader.AnnotHistPerLabel(res);
*/


	}

}