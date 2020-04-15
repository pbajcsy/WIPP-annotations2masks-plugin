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
public class ConcreteMaskColorMap extends MaskColorMap {

	// damage labels
	public final static int PASTE_DAMAGE = 1;
	public final static int AGGREGATE_DAMAGE = 2;
	public final static int AIR_VOIDS = 3;

	// contextual labels
	public final static int PASTE = 4;
	public final static int FELDSPAR = 5;
	public final static int QUARTZITE = 6;
	public final static int VOLCANIC_GLASS = 7;
	public final static int CRACKS = 8;
	public final static int DISSOLUTION = 9;
	public final static int IRON_OXIDE = 10;
	public final static int AUGITE = 11;
	
	// added for reporting purposes
	public final static int VOLCANIC_DAMAGE = 12;
	
	public final static int NUM_OF_CLASSES = 12;

	private HashMap<Color, Integer> _mapping = new HashMap<Color, Integer>();

	/**
	 * The constructor will force the specific color to gray mapping for the
	 * concrete project
	 * WARNING: do not select colors that have all three values the same since those 
	 * pixels are considered to be grayscale  
	 */
	public ConcreteMaskColorMap() {
		ArrayList<Color> uniqueColors = new ArrayList<Color>();
		Color uniqueColor = null;

		// if(annotationSet == 1 ){
		////////////////////////
		// this option corresponds to the pixel-level annotations
		// from Steve using ImageJ/Fiji tool

		// red --> cracks
		uniqueColor = new Color(255, 0, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.CRACKS);
		
		// yellow --> dissolution
		uniqueColor = new Color(255, 255, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.DISSOLUTION);
		
		// dark yellow --> air voids
		uniqueColor = new Color(255, 200, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.AIR_VOIDS);
		
		// green --> paste
		uniqueColor = new Color(0, 255, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.PASTE);
		

		// blue --> Feldspar
		uniqueColor = new Color(0, 0, 255);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.FELDSPAR);


		// magenta --> quartzite
		uniqueColor = new Color(255, 0, 255);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.QUARTZITE);

		
		// cyan --> VOLCANIC_GLASS
		uniqueColor = new Color(0, 255, 255);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.VOLCANIC_GLASS);

		// white or bright yellow --> IRON_OXIDE
		uniqueColor = new Color(255, 255, 200);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.IRON_OXIDE);
			
		// bright magenta--> AUGITE
		uniqueColor = new Color(255, 200, 255);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.AUGITE);

		// bright yellow/green --> VOLCANIC DAMAGE
		uniqueColor = new Color(150, 200, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.VOLCANIC_DAMAGE);
		
		////////////////////////////////////////////////////////
		// this option is form the mosaic-based annotations
		/*
		// from mosaic-based annotations
		uniqueColor = new Color(0, 128, 64);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.PASTE);

		// from mosaic-based annotations
		// guesssed 
		uniqueColor = new Color(0, 128, 0);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.PASTE);

		// from mosaic-based annotations
		uniqueColor = new Color(0, 128, 192);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.FELDSPAR);
		

		// from mosaic-based annotations
		uniqueColor = new Color(255, 128, 192);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.QUARTZITE);

		// from mosaic-based annotations
		uniqueColor = new Color(128, 0, 255);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.VOLCANIC_GLASS);
		
		
		// from mosaic-based annotations
		// orange --> AIR_VOIDS
		uniqueColor = new Color(255, 128, 0);
		uniqueColors.add(uniqueColor);
	    _mapping.put(uniqueColor, ConcreteMaskColorMap.AIR_VOIDS);
	    
		*/
		
		////////////////////////////////////////////////////////
		
		// }else{
		////////////////////////
		// This option corresponds to the WIPP annotations
		// from Steve created by overlapping raw intensity FOVs
		// and model-based damage binary images

		// green --> PASTE_DAMAGE (0,191,0) or (53,173,39)
		uniqueColor = new Color(53, 173, 39);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.PASTE_DAMAGE);

		// this color should be close to red (original cracks) and close to colors selected by Steve
		uniqueColor = new Color(200, 0, 100);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.PASTE_DAMAGE);
		
		// cyan --> AGGREGATE_DAMAGE - added to discriminate volcanic from aggregate damage in all claases
		uniqueColor = new Color(0, 200, 200);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.AGGREGATE_DAMAGE);

		// cyan --> AGGREGATE_DAMAGE
		uniqueColor = new Color(0, 250, 250);
		uniqueColors.add(uniqueColor);
		_mapping.put(uniqueColor, ConcreteMaskColorMap.AGGREGATE_DAMAGE);
		

		
	    
	    // }

		// MaskColorMapInit(uniqueColors);
		MaskColorMapInit();
	}

	/**
	 * Implementation of the abstract method
	 */
	public void MaskColorMapInit() {
		setColor2grayMapping(_mapping);
		// initMaskColorMap(uniqueColors);
	}

	/**
	 * This method converts integer codes for concrete classes to strings 
	 * it is used for printing purposes
	 * @param classID - integer code for concrete class
	 * @return - string code for concrete class
	 */
	public static String convertClass2String(int classID){
		//sanity check
		if(classID< 0 || classID > ConcreteMaskColorMap.NUM_OF_CLASSES){
			System.err.println("ERROR: classID = " + classID + " is out of range [0, "+ConcreteMaskColorMap.NUM_OF_CLASSES );;
			return null;
		}
		
		switch(classID){
		case 0:
			return (new String("Background"));
		case ConcreteMaskColorMap.AGGREGATE_DAMAGE:
			return ConcreteMaskLabelMap.AGGREGATE_DAMAGE;
		case ConcreteMaskColorMap.AIR_VOIDS:
			return ConcreteMaskLabelMap.AIR_VOIDS;
		case ConcreteMaskColorMap.CRACKS:
			return ConcreteMaskLabelMap.CRACKS;
		case ConcreteMaskColorMap.DISSOLUTION:
			return ConcreteMaskLabelMap.DISSOLUTION;
		case ConcreteMaskColorMap.FELDSPAR:
			return ConcreteMaskLabelMap.FELDSPAR;
		case ConcreteMaskColorMap.PASTE:
			return ConcreteMaskLabelMap.PASTE;
		case ConcreteMaskColorMap.PASTE_DAMAGE:
			return ConcreteMaskLabelMap.PASTE_DAMAGE;
		case ConcreteMaskColorMap.QUARTZITE:
			return ConcreteMaskLabelMap.QUARTZITE;
		case ConcreteMaskColorMap.IRON_OXIDE:
			return ConcreteMaskLabelMap.IRON_OXIDE;
		case ConcreteMaskColorMap.AUGITE:
			return ConcreteMaskLabelMap.AUGITE;
		case ConcreteMaskColorMap.VOLCANIC_GLASS:
			return ConcreteMaskLabelMap.VOLCANIC_GLASS;
		case ConcreteMaskColorMap.VOLCANIC_DAMAGE:
			return ConcreteMaskLabelMap.VOLCANIC_DAMAGE;			
		default:
			System.err.println("ERROR: classID = " + classID + " is out of range [0, "+ConcreteMaskColorMap.NUM_OF_CLASSES );;
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
			for (int i = 0; i < ConcreteMaskColorMap.NUM_OF_CLASSES+1; i++) {
				switch (i) {
				case 0:
					str += "BKG/no damage, ";
					break;
				case ConcreteMaskColorMap.PASTE_DAMAGE:
					str += "PASTE_DAMAGE, ";
					break;
				case ConcreteMaskColorMap.AGGREGATE_DAMAGE:
					str += "AGGREGATE_DAMAGE, ";
					break;
				case ConcreteMaskColorMap.AIR_VOIDS:
					str += "AIR_VOIDS, ";
					break;
				case ConcreteMaskColorMap.PASTE:
					str += "PASTE, ";
					break;
				case ConcreteMaskColorMap.FELDSPAR:
					str += "FELDSPAR, ";
					break;
				case ConcreteMaskColorMap.QUARTZITE:
					str += "QUARTZITE, ";
					break;
				case ConcreteMaskColorMap.VOLCANIC_GLASS:
					str += "VOLCANIC_GLASS, ";
					break;
				case ConcreteMaskColorMap.CRACKS:
					str += "CRACKS, ";
					break;
				case ConcreteMaskColorMap.DISSOLUTION:
					str += "DISSOLUTION, ";
					break;
				case ConcreteMaskColorMap.IRON_OXIDE:
					str += "IRON_OXIDE, ";
					break;
				case ConcreteMaskColorMap.AUGITE:
					str += "AUGITE, ";
					break;
				case ConcreteMaskColorMap.VOLCANIC_DAMAGE:
					str += "VOLCANIC_DAMAGE, ";
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
		if (grayCount == null || grayCount.length > ConcreteMaskColorMap.NUM_OF_CLASSES + 1) {
			System.out.println("INFO: header only or input grayCount = null or the length is larger than "
					+ (ConcreteMaskColorMap.NUM_OF_CLASSES + 1));
			return str;
		}
		// create entries
		for (int i = 0; i < grayCount.length; i++) {
			switch (i) {
			case 0:
				str += grayCount[0] + ", ";
				break;
			case ConcreteMaskColorMap.PASTE_DAMAGE:
				str += grayCount[ConcreteMaskColorMap.PASTE_DAMAGE] + ", ";
				break;
			case ConcreteMaskColorMap.AGGREGATE_DAMAGE:
				str += grayCount[ConcreteMaskColorMap.AGGREGATE_DAMAGE] + ", ";
				break;
			case ConcreteMaskColorMap.AIR_VOIDS:
				str += grayCount[ConcreteMaskColorMap.AIR_VOIDS] + ", ";
				break;
			case ConcreteMaskColorMap.PASTE:
				str += grayCount[ConcreteMaskColorMap.PASTE] + ", ";
				break;
			case ConcreteMaskColorMap.FELDSPAR:
				str += grayCount[ConcreteMaskColorMap.FELDSPAR] + ", ";
				break;
			case ConcreteMaskColorMap.QUARTZITE:
				str += grayCount[ConcreteMaskColorMap.QUARTZITE] + ", ";
				break;
			case ConcreteMaskColorMap.VOLCANIC_GLASS:
				str += grayCount[ConcreteMaskColorMap.VOLCANIC_GLASS] + ", ";
				break;
			case ConcreteMaskColorMap.CRACKS:
				str += grayCount[ConcreteMaskColorMap.CRACKS] + ", ";
				break;
			case ConcreteMaskColorMap.DISSOLUTION:
				str += grayCount[ConcreteMaskColorMap.DISSOLUTION] + ", ";
				break;
			case ConcreteMaskColorMap.IRON_OXIDE:
				str += grayCount[ConcreteMaskColorMap.IRON_OXIDE] + ", ";
				break;
			case ConcreteMaskColorMap.AUGITE:
				str += grayCount[ConcreteMaskColorMap.AUGITE] + ", ";
				break;
			case ConcreteMaskColorMap.VOLCANIC_DAMAGE:
				str += grayCount[ConcreteMaskColorMap.VOLCANIC_DAMAGE] + ", ";
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
