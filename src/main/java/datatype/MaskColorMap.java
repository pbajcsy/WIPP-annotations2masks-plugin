package datatype;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This is a container for pre-defined mapping that maps 
 * color images with a set of unique colors defining an image region class
 * to a set of unique gray scale values in an image mask. The image mask is the inputs
 * to the CNN training algorithm.
 * 
 * @author pnb
 *
 */

abstract public class MaskColorMap {

	private HashMap<Color, Integer> _color2grayMapping = new HashMap<Color, Integer>();

	abstract public void MaskColorMapInit();
	
	/**
	 * initialize the color to gray mapping
	 * @param uniqueColors
	 */
	public void initMaskColorMap(ArrayList<Color> uniqueColors){
	
		int gray = 1;
		for (Iterator<Color> k = uniqueColors.iterator(); k.hasNext();) {
			Color uniqueColor = k.next();
			_color2grayMapping.put(uniqueColor, gray);
			gray++;
		}
	}
	/////////////////////////////////////////////////////
	public HashMap<Color, Integer> getColor2grayMapping(){
		return _color2grayMapping;
	}
	public boolean setColor2grayMapping(HashMap<Color, Integer> mapping){
		if(mapping == null || mapping.isEmpty()){
			System.err.println("ERROR: setColor2grayMapping is empty");
			return false;
		}
		_color2grayMapping = mapping;
		return true;
	}
	public void resetColor2grayMapping(){
		_color2grayMapping.clear();
	}	
	public String printColor2grayMapping(){
		if(_color2grayMapping.isEmpty()){
			System.out.println("INFO: there is no color 2 gray mapping ");
			return "NA";
		}
		System.out.println("color 2 gray mapping: " + _color2grayMapping.toString());	
		return ("color 2 gray mapping, " + _color2grayMapping.toString());
	}
	
}
