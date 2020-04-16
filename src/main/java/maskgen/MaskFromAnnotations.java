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
package maskgen;

import java.awt.Color;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.nio.ByteBuffer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import datatype.Annotation;
import datatype.ConcreteMaskColorMap;
import datatype.ConcreteMaskLabelMap;
import datatype.MaskColorMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import io.AnnotationLoader;
import io.CsvMyWriter;
import io.RenameAnnotations;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.services.OMEXMLService;

import java.util.logging.Logger;
import util.BioFormatsUtils;

import loci.formats.IFormatReader;
import loci.formats.codec.CompressionType;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.out.OMETiffWriter;

import util.FileOper;

/**
 * This class converts annotations extracted from a JSON file from WIPP
 * and creates a mask image based on unique color, shape, or text 
 * 
 * @author peter bajcsy
 * @author Mohamed Ouladi <mohamed.ouladi at nist.gov>
 *
 */

/*
 */
public class MaskFromAnnotations {

	// these are the mappings between unique labels/colors/shapes and their corresponding grayscale mask values
	protected HashMap<String, Integer> _label2grayMapping = new HashMap<String, Integer>();
	protected HashMap<Color, Integer> _color2grayMapping = new HashMap<Color, Integer>();
	protected HashMap<String, Integer> _shape2grayMapping = new HashMap<String, Integer>();
	
	public static final int UNIQUE_TYPE_LABEL = 1;
	public static final int UNIQUE_TYPE_COLOR = 2;
	public static final int UNIQUE_TYPE_SHAPE = 3;
	
	// Tile size used in WIPP
	private static final int TILE_SIZE = 1024;
	private int width;
	private int height;
	
	/////////////////////////////////////
	// getters and setters for the mappings
	// between unique labels/colors/shapes and their corresponding grayscale mask values
	public HashMap<String, Integer> getLabel2grayMapping(){
		return _label2grayMapping;
	}
	@SuppressWarnings("unchecked")
	public boolean setLabel2grayMapping(HashMap<String, Integer> val){
		if(val.isEmpty()){
			return false;
		}
		_label2grayMapping.clear();
		Iterator<String> keySetIterator = val.keySet().iterator();
		while(keySetIterator.hasNext()){
		  String key = keySetIterator.next();
		  _label2grayMapping.put(key,val.get(key));
		}
		//test
		printLabel2grayMapping();
		return true;
	}
	public void resetLabel2grayMapping(){
		_label2grayMapping.clear();
	}	
	public String printLabel2grayMapping(){
		if(_label2grayMapping.isEmpty()){
			System.out.println("INFO: there is no label 2 gray mapping ");
			return "NA";
		}
		System.out.println("label 2 gray mapping: " + _label2grayMapping.toString());
		return ("label 2 gray mapping, " + _label2grayMapping.toString());
	}
	/////////////////
	public HashMap<Color, Integer> getColor2grayMapping(){
		return _color2grayMapping;
	}
	@SuppressWarnings("unchecked")
	public boolean setColor2grayMapping(HashMap<Color, Integer> val){
		if(val.isEmpty()){
			return false;
		}
		_color2grayMapping.clear();
		Iterator<Color> keySetIterator = val.keySet().iterator();
		while(keySetIterator.hasNext()){
		  Color key = keySetIterator.next();
		  _color2grayMapping.put(key,val.get(key));
		}
		// test
		printColor2grayMapping();
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
		System.out.println("color-2-gray mapping: " + _color2grayMapping.toString());	
		return ("color-2-gray mapping, " + _color2grayMapping.toString());
	}
	//////////////////
	public HashMap<String, Integer> getShape2grayMapping(){
		return _shape2grayMapping;
	}
	@SuppressWarnings("unchecked")
	public boolean setShape2grayMapping(HashMap<String, Integer> val){
		if(val.isEmpty()){
			return false;
		}
		_shape2grayMapping.clear();
		Iterator<String> keySetIterator = val.keySet().iterator();
		while(keySetIterator.hasNext()){
		  String key = keySetIterator.next();
		  _shape2grayMapping.put(key,val.get(key));
		}
		return true;
	}		
	public void resetShape2grayMapping(){
		_shape2grayMapping.clear();
	}		
	public String printShape2grayMapping(){
		if(_shape2grayMapping.isEmpty()){
			System.out.println("INFO: there is no shape 2 gray mapping ");
			return "NA";
		}
		System.out.println("shape 2 gray mapping: " + _shape2grayMapping.toString());
		return ("shape 2 gray mapping, " + _shape2grayMapping.toString());
	}
	
	/**
	 * This method will take a set of unique labels and create a mask image
	 * in such a way that the same mask value is assigned to all annotations that share 
	 * the same label. 
	 * Note: If some annotations have different color or shape but have the same textual annotation/label
	 * then they will have the same mask value in the mask image.
	 * 
	 * @param annotations - an array of Annotation objects
	 * @param uniqueLabels - an array of unique labels in the Annotation object
	 * @param rawImageName - path to a raw image
	 * @param outFileName - path to an output location for the mask image
	 * @return - boolean about the execution success
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean convertUniqueLabelsToMask(ArrayList<Annotation> annotations, ArrayList<String> uniqueLabels, boolean isMappingFixed, String rawImageName, String outFileName) throws IOException, FormatException {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return false;
		}
		if(uniqueLabels == null || uniqueLabels.size() < 1){
			System.err.println("ERROR: missing uniqueLabels");
			return false;
		}
		if(!isMappingFixed){
			// reset the label2graymapping
			resetLabel2grayMapping();
		}// else keep the fixed mapping
		
		
/*		//Initialize image reader and writer
		ImageReader reader = new ImageReader();
		IMetadata meta;

		//Set up essential elements to convert to TIFF
		try {
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			meta = service.createOMEXMLMetadata();
		}
		catch (DependencyException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}
		catch (ServiceException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}

		reader.setMetadataStore(meta);
		
		reader.setId(rawImageName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);
*/		
		
		//Reading a tiled tiff with Bioformats and converting it to an ImagePlus 
		ImagePlus imgPlus = BioFormatsUtils.readImage(rawImageName);
		ImageProcessor ip3 = imgPlus.getProcessor();
		
		// reset values and min and max values
		ip3.set(0);
		ip3.resetMinAndMax();

		// any any input image that is different from grayscale
		// based on the grayColor set to true
		boolean grayColor = true;
	    	
		int grayValueCounter = 1;
//		If commented out, then the mask won't be drawn
		for(Annotation a : annotations){
	
			boolean signal = false;
			for(String unique :  uniqueLabels){
				
				if (!signal && a.label.toLowerCase().equalsIgnoreCase(unique.toLowerCase())){
					signal = true;
					ip3.draw(a.shape);
					if(!grayColor){
						ip3.setColor(a.shape.getFillColor());
					}else{
						grayValueCounter = findGrayColorForUniqueLabel(unique,isMappingFixed);
						System.out.println("label=" + unique + ", assigned gray value =" + grayValueCounter);
						
						ip3.setColor(new Color(grayValueCounter,grayValueCounter,grayValueCounter));						
					}
					ip3.fill(a.shape);
										
				}
			}
		}

		//Define ROI 
		ShapeRoi compositeRoi = new ShapeRoi(annotations.get(0).shape);

		//Select everything that wasn't drawn and fill in black
		for (int i = 1; i < annotations.size(); i++) {
			boolean signal = false;
			for(String unique :  uniqueLabels){
				
				if (!signal && annotations.get(i).label.toLowerCase().equalsIgnoreCase(unique.toLowerCase())){
					signal = true;	
					compositeRoi = compositeRoi.or(new ShapeRoi(annotations.get(i).shape));
				}
			}
		}
		compositeRoi = compositeRoi.xor(new ShapeRoi(new Roi(0,0,ip3.getWidth(),ip3.getHeight())));
		ip3.setRoi(compositeRoi);
		//	ip3.draw(compositeRoi);
		ip3.setColor(Color.BLACK);
		ip3.fill(compositeRoi);

		// For short and float images, recalculates the min and max image values needed to correctly display the image. For ByteProcessors, resets the LUT.
		ip3.resetMinAndMax();		
		System.out.println("min = "+ ip3.getMin() +", max="+ ip3.getMax() );
		
		ImagePlus imp = new ImagePlus("Result", ip3);
		// any any input image that is different from grayscale
		// based on the grayColor set to true
		//final ByteProcessor src;
		if (grayColor && imp.getType() != ImagePlus.GRAY8){
/*			ImageConverter conv = new ImageConverter(imp);
			conv.setDoScaling(false);
			conv.convertToGray8();*/

			// false argument for no scaling
	        ip3 = (ByteProcessor) ip3.convertToByte(false).duplicate();
		}
		
		OMEXMLMetadata metadata = getMetadata(rawImageName);		
		byte[] bytesArr = (byte[]) ip3.getPixels();
		writeTiledOMETiff(metadata, bytesArr, outFileName);
		System.out.println("Done!");

		return true;
	}

	/**
	 * This method will take a set of unique Colors and create a mask image
	 * in such a way that the same mask value is assigned to all annotations that share 
	 * the same Color. 
	 * Note: If some annotations have different label or shape but have the same Color
	 * then they will have the same mask value in the mask image.
	 * 
	 * @param annotations - an array of Annotation objects
	 * @param uniqueLabels - an array of unique labels in the Annotation object
	 * @param rawImageName - path to a raw image
	 * @param outFileName - path to an output location for the mask image
	 * @return - boolean about the execution success
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean convertUniqueColorsToMask(ArrayList<Annotation> annotations, ArrayList<Color> uniqueColors, boolean isMappingFixed, String rawImageName, String outFileName) throws IOException, FormatException {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return false;
		}
		if(uniqueColors == null || uniqueColors.size() < 1){
			System.err.println("ERROR: missing uniqueColors");
			return false;
		}
		if(!isMappingFixed){
			// reset the color2graymapping
			resetColor2grayMapping();
		}// else keep the fixed mapping
		
		//Initialize image reader and writer
/*		ImageReader reader = new ImageReader();
		IMetadata meta;

		//Set up essential elements to convert to TIFF
		try {
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			meta = service.createOMEXMLMetadata();
		}
		catch (DependencyException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}
		catch (ServiceException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}

		reader.setMetadataStore(meta);

		//Read TIFF image for background and declare name and location of final TIFF file
		//reader.setId("/home/kpb3/Downloads/Core-M3-Total_StitchedWafer_16BPP.ome(1).tif");
		reader.setId(rawImageName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);		
		*/
		
		//Reading a tiled tiff with Bioformats and converting it to an ImagePlus 
		ImagePlus imgPlus = BioFormatsUtils.readImage(rawImageName);
		ImageProcessor ip3 = imgPlus.getProcessor();
		
		// reset values and min and max values
		ip3.set(0);
		ip3.resetMinAndMax();

		boolean grayColor = true;
		int grayValue = 1;
		for(Annotation a : annotations){
			
			boolean signal = false;
			for(Color unique :  uniqueColors){
				
				if (!signal && a.getFillColor().equals(unique)){
					signal = true;
					ip3.draw(a.shape);
					if(!grayColor){
						ip3.setColor(a.shape.getFillColor());
					}else{
						// for each unique shape fill color, find a unique gray scale value for the mask
						grayValue = findGrayColorForUniqueRGBColor(a.shape.getFillColor(),isMappingFixed);
						ip3.setColor(new Color(grayValue,grayValue,grayValue));
					}
					ip3.fill(a.shape);
										
				}
			}
		}

		//Define ROI 
		ShapeRoi compositeRoi = new ShapeRoi(annotations.get(0).shape);

		//Select everything that wasn't drawn and fill in black
		for (int i = 1; i < annotations.size(); i++) {
			boolean signal = false;
			for(Color unique :  uniqueColors){
				
				if (!signal && annotations.get(i).shape.getFillColor().equals(unique)){
					signal = true;	
					compositeRoi = compositeRoi.or(new ShapeRoi(annotations.get(i).shape));

				}
			}
		}
		compositeRoi = compositeRoi.xor(new ShapeRoi(new Roi(0,0,ip3.getWidth(),ip3.getHeight())));
		ip3.setRoi(compositeRoi);
		//	ip3.draw(compositeRoi);
		ip3.setColor(Color.BLACK);
		ip3.fill(compositeRoi);

		// For short and float images, recalculates the min and max image values needed to correctly display the image. For ByteProcessors, resets the LUT.
		ip3.resetMinAndMax();		
		System.out.println("min = "+ ip3.getMin() +", max="+ ip3.getMax() );
		
		OMEXMLMetadata metadata = getMetadata(rawImageName);
		byte[] bytesArr = (byte[]) ip3.getPixels();
		writeTiledOMETiff(metadata, bytesArr, outFileName);
		System.out.println("Done!");

		return true;
	}

	/**
	 * This method will take a set of unique shapes and create a mask image
	 * in such a way that the same mask value is assigned to all annotations that share 
	 * the same shape. 
	 * Note: If some annotations have different color or label but have the same shape
	 * then they will have the same mask value in the mask image.
	 * 
	 * @param annotations - an array of Annotation objects
	 * @param uniqueShapes - an array of unique shapes in the Annotation object
	 * @param rawImageName - path to a raw image
	 * @param outFileName - path to an output location for the mask image
	 * @return - boolean about the execution success
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean convertUniqueShapesToMask(ArrayList<Annotation> annotations, ArrayList<String> uniqueShapes, boolean isMappingFixed, String rawImageName, String outFileName) throws IOException, FormatException {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return false;
		}
		if(uniqueShapes == null || uniqueShapes.size() < 1){
			System.err.println("ERROR: missing uniqueShapes");
			return false;
		}
		if(!isMappingFixed){
			// reset the shape2graymapping
			resetShape2grayMapping();
		}// else keep the fixed mapping
		
		
/*		//Initialize image reader and writer
		ImageReader reader = new ImageReader();
		IMetadata meta;

		//Set up essential elements to convert to TIFF
		try {
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			meta = service.createOMEXMLMetadata();
		}
		catch (DependencyException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}
		catch (ServiceException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}

		reader.setMetadataStore(meta);

		//Read TIFF image for background and declare name and location of final TIFF file
		//reader.setId("/home/kpb3/Downloads/Core-M3-Total_StitchedWafer_16BPP.ome(1).tif");
		reader.setId(rawImageName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);*/
		
		//Reading a tiled tiff with Bioformats and converting it to an ImagePlus 
		ImagePlus imgPlus = BioFormatsUtils.readImage(rawImageName);
		ImageProcessor ip3 = imgPlus.getProcessor();
		
		// reset values and min and max values
		ip3.set(0);
		ip3.resetMinAndMax();
		
		boolean grayColor = true;
		int grayValueCounter = 1;
		//		If commented out, then the mask won't be drawn
		for(Annotation a : annotations){
			
			boolean signal = false;
			for(String unique :  uniqueShapes){
				
				if (!signal && a.getShapeName().equalsIgnoreCase(unique)){
					signal = true;
					ip3.draw(a.shape);
					if(!grayColor){
						ip3.setColor(a.shape.getFillColor());
					}else{
						grayValueCounter = findGrayColorForUniqueShape(unique,isMappingFixed);
						System.out.println("label=" + unique + ", assigned gray value =" + grayValueCounter);
						if(grayValueCounter < 0){
							System.err.println("label=" + unique + ", assigned gray value =" + grayValueCounter);
							ip3.setColor(Color.BLACK);
						}else{							
							ip3.setColor(new Color(grayValueCounter,grayValueCounter,grayValueCounter));
						}
						
					}
					ip3.fill(a.shape);
										
				}
			}
		}

		//Define ROI 
		ShapeRoi compositeRoi = new ShapeRoi(annotations.get(0).shape);

		//Select everything that wasn't drawn and fill in black
		for (int i = 1; i < annotations.size(); i++) {
			boolean signal = false;
			for(String unique :  uniqueShapes){
				
				if (!signal && annotations.get(i).getShapeName().equalsIgnoreCase(unique)){
					signal = true;	
					compositeRoi = compositeRoi.or(new ShapeRoi(annotations.get(i).shape));
				}
			}
		}
		compositeRoi = compositeRoi.xor(new ShapeRoi(new Roi(0,0,ip3.getWidth(),ip3.getHeight())));
		ip3.setRoi(compositeRoi);
		//	ip3.draw(compositeRoi);
		ip3.setColor(Color.BLACK);
		ip3.fill(compositeRoi);

		// For short and float images, recalculates the min and max image values needed to correctly display the image. For ByteProcessors, resets the LUT.
		ip3.resetMinAndMax();		
		System.out.println("min = "+ ip3.getMin() +", max="+ ip3.getMax() );
		
		OMEXMLMetadata metadata = getMetadata(rawImageName);
		byte[] bytesArr = (byte[]) ip3.getPixels();
		writeTiledOMETiff(metadata, bytesArr, outFileName);
		System.out.println("Done!");

		return true;
	}

	/**
	 * This method is a helper for the convertUniqueLabelsToMask method
	 * 
	 * @param uniqueLabel
	 * @return
	 */
	public int findGrayColorForUniqueLabel(String uniqueLabel, boolean isMappingFixed){
		int gray = -1;
		Iterator<String> keySetIterator = _label2grayMapping.keySet().iterator();

		if(uniqueLabel.equalsIgnoreCase("")){
			System.out.println("DEBUG: _label2grayMapping.size()=" + _label2grayMapping.size());
		}
		while(keySetIterator.hasNext()){
		  String key = keySetIterator.next();
		  //System.out.println("Text key: " + key + " value: " + _label2grayMapping.get(key));
		  if(key.equalsIgnoreCase(uniqueLabel)){
			  return _label2grayMapping.get(key);
		  }
		  // try to match the pre-defined keys (Aggregate_damage, Paste_damage, Air_void) without underscore
		  if(key.replace("_", "").equalsIgnoreCase(uniqueLabel)){
			  return _label2grayMapping.get(key);
		  }
		}
		if(isMappingFixed){
			// return the background color
			System.out.println("DEBUG: FIXED CASE could not find a match to a label: " +  uniqueLabel);
			return 0;
		}
		// else modify the mapping and add a new unique element
		gray = _label2grayMapping.size()+1;
		_label2grayMapping.put(uniqueLabel, gray);
		return gray;
	}
	/**
	 * This method is a helper for the convertUniqueShapesToMask method
	 * 
	 * @param uniqueShape
	 * @return
	 */
	public int findGrayColorForUniqueShape(String uniqueShape, boolean isMappingFixed){
		int gray = -1;
		Iterator<String> keySetIterator = _shape2grayMapping.keySet().iterator();

		if(!uniqueShape.equalsIgnoreCase(Annotation.RECTANGLE) && !uniqueShape.equalsIgnoreCase(Annotation.CIRCLE) && 
				!uniqueShape.equalsIgnoreCase(Annotation.FREEHAND)  ){
				System.err.println("ERROR: uniqueShape =" + uniqueShape + " is not valid string");
				System.out.println("DEBUG: _shape2grayMapping.size()=" + _shape2grayMapping.size());
				return -1;
		}
		while(keySetIterator.hasNext()){
		  String key = keySetIterator.next();
		  //System.out.println("Text key: " + key + " value: " + _shape2grayMapping.get(key));
		  if(key.equalsIgnoreCase(uniqueShape)){
			  return _shape2grayMapping.get(key);
		  }
		}
		if(isMappingFixed){
			// return the background color
			System.out.println("DEBUG: FIXED CASE could not find a match to a shape: " +  uniqueShape);
			return 0;
		}
		// else modify the mapping and add a new unique element
		gray = _shape2grayMapping.size()+1;
		_shape2grayMapping.put(uniqueShape, gray);
		return gray;
	}
	/**
	 * 
	 * This method is a helper for the convertUniqueColorsToMask method
	 * 
	 * @param uniqueColor
	 * @return
	 */
	public int findGrayColorForUniqueRGBColor(Color uniqueColor, boolean isMappingFixed){
		int gray = -1;
		Iterator<Color> keySetIterator = _color2grayMapping.keySet().iterator();

		while(keySetIterator.hasNext()){
		  Color key = keySetIterator.next();
		  //System.out.println("Color key: " + key.toString() + " value: " + _color2grayMapping.get(key));
		  if(key.getRed() == uniqueColor.getRed() && key.getGreen() == uniqueColor.getGreen() && key.getBlue() == uniqueColor.getBlue()){			  
			  return _color2grayMapping.get(key);
		  }
		}
		if(isMappingFixed){
			// return the background color
			System.out.println("DEBUG: FIXED CASE could not find a match to a color: " +  uniqueColor.toString());
			return 0;
		}
		// else modify the mapping and add a new unique element		
		gray = _color2grayMapping.size()+1;
		_color2grayMapping.put(uniqueColor, gray);
		return gray;
	}
	
	/**
	 * This method is for batch execution applied to a folder with JSON files
	 * the files will be converted to mask, one per color
	 * The code for used for organelle tracking project
	 * Note: this method is designed for all rawImageFiles to be the same!!
	 * 
	 * @param inputJSONFileFolder
	 * @param outFileFolder
	 * @throws IOException
	 * @throws FormatException
	 */
	public void convertUniqueColorsToMaskBatch(String inputJSONFileFolder, boolean isMappingFixed, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (inputJSONFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null inputFileFolder or outFileFolder ");
			return;
		}
		
		// getting images to process
		Collection<String> dirFiles = FileOper.readFileDirectory(inputJSONFileFolder);

		// select images with the right suffix
		String suffixFrom = new String(".json");
		Collection<String> dirSelectFiles = FileOper.selectFileType(dirFiles,suffixFrom );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectFiles,
				FileOper.SORT_ASCENDING);
			

		/////////////////////////////
		AnnotationLoader annotClass = new AnnotationLoader();
		
		String JSONfileName = new String();	
		String outFileName = new String();
		//TODO - this is a hard-coded file name since all files have the same dimension
		String rawFileName = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations\\testInput\\A3_02_c1_p1Z00_BrightField_t001_maxXY.tif");
		ArrayList<String> strSaveMapping = new ArrayList<String>();
		
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			JSONfileName = k.next();
			
			ArrayList<Annotation> annotations =  annotClass.readJSONfromWIPP(JSONfileName);
			//AnnotationLoader.printArrayListAnnot(annotations);

			MaskFromAnnotations myClass = new MaskFromAnnotations();
			// construct the output file name
			String name = (new File(JSONfileName)).getName();
			name = name.substring(0, name.length()-5) + ".tif";
			
			ArrayList<Color> uniqueColors = AnnotationLoader.getUniqueColors(annotations);
			for(int j = 0; j < uniqueColors.size(); j++){
				ArrayList<Color> oneColor = new ArrayList<Color>();
				oneColor.add(uniqueColors.get(j));
				String colorName = AnnotationLoader.mapColorValue2ColorName(uniqueColors.get(j));
				//test
				if(!colorName.equalsIgnoreCase("Red")){
					System.out.println("colorName="+colorName);
				}
				outFileName = new String(outFileFolder + File.separator + "mask_" + colorName + "_" + name);
				System.out.println("outFileName="+outFileName);
			
				// create the mask based on unique colors
				myClass.convertUniqueColorsToMask(annotations,oneColor,isMappingFixed, rawFileName, outFileName);
				strSaveMapping.add(new String(colorName + ", " + name + ", " + myClass.printColor2grayMapping()) );

			}

		}
		// save the mappings
		outFileName = new String(outFileFolder + File.separator + "mappings.csv");
		CsvMyWriter.SaveArrayListString(strSaveMapping, outFileName);	
		
	}
	
	/**
	 * This method is designed to be launched from a command line (e.g., Docker packaged execution from a command line)
	 * Note: The method assumes that the JSON files and RawImageFiles in their appropriate folders have matching names except for their suffixes.
	 * 		If combineAllUnique is set to true, masks are created with the same name in separate folders depending on the colors/shapes/labels.
	 * @param inputJSONFileFolder - input folder with JSON files
	 * @param uniqueType - integer defining whether the masks are created by label (1), color (2) or shape (3)
	 * @param combineAllUnique - boolean flag whether to create one mask with all unique types of color/label/shape (value = true) or one mask per of color/label/shape unique type (value = false)
	 * @param inputRawFileFolder - input folder with raw TIFF files
	 * @param outFileFolder - output folder for the generated masks
	 * @return boolean depending on the success
	 * 
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean CMDlaunch(String inputJSONFileFolder, int uniqueType, boolean combineAllUnique, boolean isMappingFixed, String inputRawFileFolder, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (inputJSONFileFolder == null || inputRawFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null inputFileFolder, inputRawFileFolder or outFileFolder ");
			return false;
		}
		if (uniqueType <1 || uniqueType > 3) {
			System.err.println("Error: uniqueType is out of bounds ");
			return false;
		}
		/*Check directory if exist*/
		File directory=new File(inputJSONFileFolder);
		if(!directory.exists()){
			System.out.println("Input JSON Directory does not exist: " + inputJSONFileFolder);
			return false;
		}
		directory=new File(inputRawFileFolder);
		if(!directory.exists()){
			System.err.println("Input RAW Directory does not exist: " + inputRawFileFolder);
			return false;
		}
		directory=new File(outFileFolder);
		if(!directory.exists()){
			System.err.println("output Directory does not exist: " + outFileFolder);
			return false;
		}
		///////////////////////////////////////////////////////////
		// getting JSON files to process
		if(inputJSONFileFolder.endsWith(File.separator)) {
			inputJSONFileFolder = inputJSONFileFolder.substring(0, inputJSONFileFolder.length()-1);
		}
		Collection<String> dirJSONFiles = FileOper.readFileDirectory(inputJSONFileFolder);
		if (dirJSONFiles.isEmpty()){
			System.err.println("inputJSONFileFolder Directory is empty: " + inputJSONFileFolder);
			return false;
		}
		// select JSON files with the right suffix
		String suffixJSON = new String(".json");
		Collection<String> dirSelectJSONFiles = FileOper.selectFileType(dirJSONFiles,suffixJSON );	
		
		// sort stacks to process
		Collection<String> sortedJSONInFolder = FileOper.sort(dirSelectJSONFiles,
				FileOper.SORT_ASCENDING);
		
		///////////////////////////////////////////////////////////////
		// getting images to process
		Collection<String> dirRawFiles = FileOper.readFileDirectory(inputRawFileFolder);
		if (dirRawFiles.isEmpty()){
			System.err.println("inputRawFileFolder Directory is empty: " + inputRawFileFolder);
			return false;
		}
		// select images with the right suffix
		String suffixTIFF = new String(".tif");
		Collection<String> dirSelectRawFiles = FileOper.selectFileType(dirRawFiles,suffixTIFF );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectRawFiles,
				FileOper.SORT_ASCENDING);
			
		//////////////////////////////////////////////////////
		AnnotationLoader annotClass = new AnnotationLoader();
		
		String JSONfileName = new String();	
		String rawFileName = new String();
		String outFileName = new String();
			
		// store metadata about the execution
		ArrayList<String> strSaveMapping = new ArrayList<String>();
		
		boolean foundMatch = false;
		for (Iterator<String> k = sortedJSONInFolder.iterator(); k.hasNext();) {
			JSONfileName = k.next();
			String nameJSON = (new File(JSONfileName)).getName();
			nameJSON = nameJSON.substring(0, nameJSON.length()-9); // TODO check what to remove, this case is for  .ome.json
			// find matching rawFileName
			foundMatch = false;
			for(Iterator<String> r = sortedImagesInFolder.iterator(); !foundMatch && r.hasNext(); ){
				rawFileName = r.next();
				String nameTIFF = (new File(rawFileName)).getName();
				nameTIFF = nameTIFF.substring(0, nameTIFF.length()-8); // TODO check what to remove, this case is for  .ome.tif
				if(nameJSON.equalsIgnoreCase(nameTIFF)){
					
					foundMatch = true;
				}
			}
			if(!foundMatch){
				System.err.println("ERROR: could not find a matching raw TIFF image to the JSON file");
				continue;
			}
			System.out.println("INFO: matching pair: JSON = " +  JSONfileName + " TIFF = " + rawFileName);
			
			ArrayList<Annotation> annotations =  annotClass.readJSONfromWIPP(JSONfileName);
			System.out.println("before clean up");
			AnnotationLoader.printArrayListAnnot(annotations);
			// clean up annotations
			boolean ret =  AnnotationLoader.cleanupLabels(annotations);
			System.out.println("after clean up");
			AnnotationLoader.printArrayListAnnot(annotations);

			// construct the output file name
			String name = (new File(JSONfileName)).getName();
			name = name.substring(0, name.length()-5) + ".tif";
			
			////////////////////////////////
			// TODO: specify the prefix from the argument?
			String maskPreffix = new String(); ;// or "mask_"
			
			switch(uniqueType){
				case UNIQUE_TYPE_LABEL:
					ArrayList<String> uniqueLabels = AnnotationLoader.getUniqueLabels(annotations);
					if(combineAllUnique){
						outFileName = new String(outFileFolder + File.separator + maskPreffix + name);
						convertUniqueLabelsToMask(annotations,uniqueLabels, isMappingFixed, rawFileName, outFileName);
						strSaveMapping.add(new String(name + ", " + printLabel2grayMapping()) );
					}else{
						for(int j = 0; j < uniqueLabels.size(); j++){
							ArrayList<String> oneLabel = new ArrayList<String>();
							oneLabel.add(uniqueLabels.get(j));
							String labelName = uniqueLabels.get(j);

							outFileName = new String(outFileFolder + File.separator + "label" + j + File.separator);
							directory = new File(outFileName);
							if (!directory.exists()) {
								directory.mkdir();
								System.out.println("output mask Directory was created: " + outFileName);
							}
							outFileName +=  maskPreffix + labelName + "_" + name;
							System.out.println("outFileName="+outFileName);

							// create the mask based on unique colors
							convertUniqueLabelsToMask(annotations,oneLabel,isMappingFixed, rawFileName, outFileName);
							strSaveMapping.add(new String(labelName + ", " + name + ", " + printLabel2grayMapping()) );
						}			
					}
					break;
				case UNIQUE_TYPE_COLOR:
					ArrayList<Color> uniqueColors = AnnotationLoader.getUniqueColors(annotations);
					if(combineAllUnique){
						outFileName = new String(outFileFolder + File.separator + maskPreffix + name);
						convertUniqueColorsToMask(annotations,uniqueColors, isMappingFixed, rawFileName, outFileName);
						strSaveMapping.add(new String(name + ", " + printColor2grayMapping()) );						
					}else{
						for(int j = 0; j < uniqueColors.size(); j++){
							ArrayList<Color> oneColor = new ArrayList<Color>();
							oneColor.add(uniqueColors.get(j));
							String colorName = AnnotationLoader.mapColorValue2ColorName(uniqueColors.get(j));

							outFileName = new String(outFileFolder + File.separator + "color" + j + File.separator);
							directory = new File(outFileName);
							if (!directory.exists()) {
								directory.mkdir();
								System.out.println("output mask Directory was created: " + outFileName);
							}
							//outFileName +=  maskPreffix + colorName + "_" + name;
							// This is important for automation otherwise the names have different length for downstream processing
							outFileName +=  name;
							System.out.println("outFileName="+outFileName);
						
							// create the mask based on unique colors
							convertUniqueColorsToMask(annotations,oneColor, isMappingFixed, rawFileName, outFileName);
							strSaveMapping.add(new String(colorName + ", " + name + ", " + printColor2grayMapping()) );
						}			
					}
					break;
				case UNIQUE_TYPE_SHAPE:
					ArrayList<String> uniqueShapes = AnnotationLoader.getUniqueShapes(annotations);
					if(combineAllUnique){
						outFileName = new String(outFileFolder + File.separator + maskPreffix + name);
						convertUniqueShapesToMask(annotations,uniqueShapes, isMappingFixed, rawFileName, outFileName);
						strSaveMapping.add(new String(name + ", " + printShape2grayMapping()) );
					}else{
						for(int j = 0; j < uniqueShapes.size(); j++){
							ArrayList<String> oneShape = new ArrayList<String>();
							oneShape.add(uniqueShapes.get(j));
							String shapeName = uniqueShapes.get(j);

							outFileName = new String(outFileFolder + File.separator + "shape" + j + File.separator);
							directory = new File(outFileName);
							if (!directory.exists()) {
								directory.mkdir();
								System.out.println("output mask Directory was created: " + outFileName);
							}
							outFileName +=  maskPreffix + shapeName + "_" + name;
							System.out.println("outFileName="+outFileName);

							// create the mask based on unique colors
							convertUniqueShapesToMask(annotations,oneShape, isMappingFixed, rawFileName, outFileName);
							strSaveMapping.add(new String(shapeName + ", " + name + ", " + printShape2grayMapping()) );
						}			
					}					
					break;
				default:
					break;
			}

		}
		// save the mappings
		outFileName = new String(outFileFolder + File.separator + "mappings.csv");
		CsvMyWriter.SaveArrayListString(strSaveMapping, outFileName);	

		return true;
	}
	
	
	//Inspired from the WIPP-image-assembling-plugin
	private OMEXMLMetadata getMetadata(String tile) {
		OMEXMLMetadata metadata;
		try {
			OMEXMLService omeXmlService = new ServiceFactory().getInstance(
					OMEXMLService.class);
			metadata = omeXmlService.createOMEXMLMetadata();
		} catch (DependencyException ex) {
			throw new RuntimeException("Cannot find OMEXMLService", ex);
		} catch (ServiceException ex) {
			throw new RuntimeException("Cannot create OME metadata", ex);
		}
		try (ImageReader imageReader = new ImageReader()) {
			IFormatReader reader;
			reader = imageReader.getReader(tile);
			reader.setOriginalMetadataPopulated(false);
			reader.setMetadataStore(metadata);
			reader.setId(tile);
			this.width = reader.getSizeX();
			this.height = reader.getSizeY();
		} catch (FormatException | IOException ex) {
			throw new RuntimeException("No image reader found for file "
					+ tile, ex);
		}

		return metadata;
	}
	
	
	public void writeTiledOMETiff(OMEXMLMetadata metadata, byte[] bytesArr, String outFileName){
		//Writing the output tiled tiff
		try (OMETiffWriter imageWriter = new OMETiffWriter()) {
			imageWriter.setMetadataRetrieve(metadata);
			imageWriter.setTileSizeX(TILE_SIZE);
			imageWriter.setTileSizeY(TILE_SIZE);
			imageWriter.setInterleaved(metadata.getPixelsInterleaved(0));
			imageWriter.setCompression(CompressionType.LZW.getCompression());
			imageWriter.setId(outFileName);

			// Determined the number of tiles to read and write
			int nXTiles = this.width / TILE_SIZE;
			int nYTiles = this.height / TILE_SIZE;
			if (nXTiles * TILE_SIZE != this.width) nXTiles++;
			if (nYTiles * TILE_SIZE != this.height) nYTiles++;

			for (int k=0; k<nYTiles; k++) {
				for (int l=0; l<nXTiles; l++) {
					
					int tileX = l * TILE_SIZE;
					int tileY = k * TILE_SIZE;
					
					int effTileSizeX = (tileX + TILE_SIZE) < this.width ? TILE_SIZE : this.width - tileX;
					int effTileSizeY = (tileY + TILE_SIZE) < this.height ? TILE_SIZE : this.height - tileY;

					//buf = reader.openBytes(0, tileX, tileY, effTileSizeX, effTileSizeY);
					imageWriter.saveBytes(0, bytesArr, tileX, tileY, effTileSizeX, effTileSizeY);
				}
			}
			
			
			//imageWriter.saveBytes(0, bytesArr);

		} catch (FormatException | IOException ex) {
			throw new RuntimeException("No image writer found for file "
					+ outFileName, ex);
		}
	}
	
	
	/**
	 * @param args
	 * @throws IOException testing
	 * @throws FormatException 
	 */
	public static void main(String[] args) throws IOException, FormatException {		
		// sanity checks
		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}	

		Options options = new Options();

		Option inputJson = new Option("ij", "inputannotations", true, "input json folder");
		inputJson.setRequired(true);
		options.addOption(inputJson);
		
		Option inputRaw = new Option("ir", "inputrawimages", true, "input raw images folder");
		inputRaw.setRequired(true);
		options.addOption(inputRaw);
		
		Option inputSV = new Option("sv", "stitchingvector", true, "stitching vector folder");
		inputSV.setRequired(true);
		options.addOption(inputSV);

		Option uniqueTypeOpt = new Option("t", "uniquetype", true, "unique type");
		uniqueTypeOpt.setRequired(true);
		options.addOption(uniqueTypeOpt);
		
		Option combineAU = new Option("c", "combineallunique", true, "combine all unique");
		combineAU.setRequired(true);
		options.addOption(combineAU);

		Option output = new Option("o", "outputmasks", true, "output masks folder");
		output.setRequired(true);
		options.addOption(output);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("the required arguments", options);

			System.exit(1);
			return;
		}

		String inputJSONFileFolder = cmd.getOptionValue("inputannotations");
		String inputRawFileFolder = cmd.getOptionValue("inputrawimages");
		String inputStitchingFileFolder = cmd.getOptionValue("stitchingvector");
		String uniqueTypeStr = cmd.getOptionValue("uniquetype");
		String combineAllUniqueStr = cmd.getOptionValue("combineallunique");
		String outFileFolder = cmd.getOptionValue("outputmasks");
		
		int uniqueType = Integer.parseInt(uniqueTypeStr);
		boolean combineAllUnique = Boolean.parseBoolean(combineAllUniqueStr);
		
		Path tempDirWithPrefix = Files.createTempDirectory("tempjson");
		String modifiedJSONFolder = tempDirWithPrefix.toString();
		
		File inputFolder = new File(inputRawFileFolder);
		File[] tiles =  inputFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".tif");
			}
		});

		if (tiles == null || tiles.length == 0) {
			throw new NullPointerException("Input folder is empty or no images were found.");
		}
				
		if(!RenameAnnotations.CMD_renameAnnotJSONbasedOnStitching(inputJSONFileFolder, inputStitchingFileFolder, modifiedJSONFolder)) {
			System.err.println("ERROR: failed renaming "+ inputJSONFileFolder + " based on "+ inputStitchingFileFolder + " into " + modifiedJSONFolder);
			return;
		}
		
		MaskFromAnnotations myClass = new MaskFromAnnotations();
		boolean isMappingFixed = false;
		boolean ret = myClass.CMDlaunch(modifiedJSONFolder, uniqueType, combineAllUnique, isMappingFixed, inputRawFileFolder, outFileFolder );
	}

}