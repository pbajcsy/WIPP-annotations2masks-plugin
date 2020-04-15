package datatype;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * This class is specific to the concrete project with Steve Feldman It contains
 * the definition of mask gray according to colors in the annotations
 * 
 * @author pnb
 *
 */
public class OrganelleMaskColorMap extends MaskColorMap {

	// organelle project labels
	public final static int ORGANELLE = 1;
	public final static int CELL = 2;
	public final static int ORGANELLE_TRACK = 3;

	public final static int NUM_OF_CLASSES = 3;

	private HashMap<Color, Integer> _mapping = new HashMap<Color, Integer>();

	/**
	 * The constructor will force the specific color to gray mapping for the
	 * organelle project
	 * WARNING: do not select colors that have all three values the same since those 
	 * pixels are considered to be grayscale  
	 */
	public OrganelleMaskColorMap() {
		ArrayList<Color> uniqueColors = new ArrayList<Color>();
		Color uniqueColor = null;

		// red --> organelles
		uniqueColor = new Color(255, 0, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, OrganelleMaskColorMap.ORGANELLE);
		
		// yellow --> cell region
		uniqueColor = new Color(128,255,128);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, OrganelleMaskColorMap.CELL);
		
		// yellow --> organelle trackn
		uniqueColor = new Color(255,255,128);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, OrganelleMaskColorMap.ORGANELLE_TRACK);
		
		
		////////////////////////////////////////////////////////
		
		MaskColorMapInit();
	}

	/**
	 * Implementation of the abstract method
	 */
	public void MaskColorMapInit() {
		setColor2grayMapping(_mapping);
	}

	/**
	 * This method converts integer codes for organelle classes to strings 
	 * it is used for printing purposes
	 * @param classID - integer code for concrete class
	 * @return - string code for concrete class
	 */
	public static String convertClass2String(int classID){
		//sanity check
		if(classID< 0 || classID > OrganelleMaskColorMap.NUM_OF_CLASSES){
			System.err.println("ERROR: classID = " + classID + " is out of range [0, "+OrganelleMaskColorMap.NUM_OF_CLASSES );;
			return null;
		}
		
		switch(classID){
		case 0:
			return (new String("Background"));
		case OrganelleMaskColorMap.ORGANELLE:
			return OrganelleMaskLabelMap.ORGANELLE;
		case OrganelleMaskColorMap.CELL:
			return OrganelleMaskLabelMap.CELL;
		default:
			System.err.println("ERROR: classID = " + classID + " is out of range [0, "+OrganelleMaskColorMap.NUM_OF_CLASSES );;
			return null;
		}
	
	}
	/**
	 * This method will report the results of pixel counting per class the
	 * method for counting is implemented in MaskFromColorImage public static
	 * int [] countPixelsPerColor(String inputGrayMaskFileName, int
	 * maxLabelValue)
	 * 
	 * @param grayCount
	 *            - an array of int with the count deciphering the count index
	 *            is based on the gray values assigned in ths class
	 * @param withHeader - if true then the returning string includes header else only numbers
	 * @return String with the pixel counts
	 * 
	 */
	public static String reportPixelCountPerClass(int[] grayCount, boolean withHeader) {

		String str = new String();
		if (withHeader) {
			// create header
			for (int i = 0; i < OrganelleMaskColorMap.NUM_OF_CLASSES+1; i++) {
				switch (i) {
				case 0:
					str += "BKG/no damage, ";
					break;
				case OrganelleMaskColorMap.ORGANELLE:
					str += "ORGANELLE, ";
					break;
				case OrganelleMaskColorMap.CELL:
					str += "CELL, ";
					break;
					default:
					System.err.println("ERROR: gray value is not defined val = " + i);
					break;
				}

			}
			//str += "\n";
		}
		// sanity check
		// as well as a method how to return only the header
		if (grayCount == null || grayCount.length > OrganelleMaskColorMap.NUM_OF_CLASSES + 1) {
			System.out.println("INFO: header only or input grayCount = null or the length is larger than "
					+ (OrganelleMaskColorMap.NUM_OF_CLASSES + 1));
			return str;
		}
		// create entries
		for (int i = 0; i < grayCount.length; i++) {
			switch (i) {
			case 0:
				str += grayCount[0] + ", ";
				break;
			case OrganelleMaskColorMap.ORGANELLE:
				str += grayCount[OrganelleMaskColorMap.ORGANELLE] + ", ";
				break;
			case OrganelleMaskColorMap.CELL:
				str += grayCount[OrganelleMaskColorMap.CELL] + ", ";
				break;
			default:
				System.err.println("ERROR: gray value is not defined val = " + i);
				break;
			}

		}
		System.out.println(str);
		return str;
	}
	
	/**
	 * This method creates a legend image with gray colors per label
	 * 
	 * @param classLabels - array of integer that correspond to the labels defined in this class
	 * @param widthPerLabel - width of the legend area containing one label specific gray value
	 * @param heightPerLabel - height of the legend area containing one label specific gray value
	 * @return
	 */
	public static ImagePlus createGrayLegend(int [] classLabels, int widthPerLabel, int heightPerLabel){
		
		// sanity check
		if(classLabels == null || classLabels.length < 1){
			System.err.println("ERROR: missing class Labels");
			return null;
		}
		if(widthPerLabel < 5 || heightPerLabel < 5){
			System.err.println("ERROR: width and height per label shold be at least 5 pixels");
			return null;			
		}
		
		// legend image			
		ByteProcessor legendIP = new ByteProcessor(widthPerLabel * classLabels.length, heightPerLabel);
		legendIP.set(0);
		ImagePlus imRes = new ImagePlus("Result", legendIP);
		
		for(int label = 0; label < classLabels.length; label ++){
			if(convertClass2String(classLabels[label]) == null ){
				System.err.println("ERROR: label = " + classLabels[label] + " does not match existing labels");				
				continue;							
			}
		    
			for (int x = widthPerLabel*label; x < widthPerLabel* (label+1) && x <legendIP.getWidth(); x++) {
				for (int y = 0; y < legendIP.getHeight(); y++) {
					legendIP.set(x, y, classLabels[label]);
				}
			}
		
		}
		
		return imRes;
		
	}

}
