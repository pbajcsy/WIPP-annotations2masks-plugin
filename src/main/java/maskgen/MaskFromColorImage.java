/**
 * 
 */
package maskgen;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;

import datatype.ConcreteMaskColorMap;
import datatype.MaskColorMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.util.ImageProcessorSource;
import util.FileOper;

/**
 * This class was created to take color-coded input images as annotations of concrete classes
 * and convert them consistently into grayscale masks for CNN training.
 * 
 * @author pnb
 *
 */
public class MaskFromColorImage {
	
	
	public static ImagePlus convertMaskImage2Color(ImagePlus imgMask, MaskColorMap mapping)  {
		
			ImageProcessor ip_mask = imgMask.getProcessor();
			
			ColorProcessor ip_color = new ColorProcessor(ip_mask.getWidth(), ip_mask.getHeight());
			ip_color.set(0);

			int pixelGray = 0;
			for (int x = 0; x < ip_mask.getWidth(); x++) {
				for (int y = 0; y < ip_mask.getHeight(); y++) {
					pixelGray = ip_mask.getPixel(x, y);					
	
					Color val = findColorForUniqueGray(pixelGray, mapping.getColor2grayMapping());
					
					//debug
	/*				if(val == Color.black){
						System.err.println("x="+x+",y="+y);
						Iterator<Color> keySetIterator2 = mapping.getColor2grayMapping().keySet().iterator();
						while(keySetIterator2.hasNext()){
							  Color key = keySetIterator2.next();
							  System.out.println("Color key: " + key.toString() + " value: " + mapping.getColor2grayMapping().get(key));
						}
					}*/
					
					ip_color.setColor(val);
					ip_color.drawPixel(x, y);
				}
			}

			ImagePlus imgColor = new ImagePlus("color image", ip_color);

			return imgColor;
	}
	

	/**
	 * This method takes computed gray masks and converts them into color masks 
	 * according to the color encoding define during ImageJ/Fiji annotations 
	 * 
	 * @param maskImageFolder - input folder with gray masks
	 * @param mapping - mapping between color and gray values
	 * @param outputPath - output location for the color masks
	 * 
	 * @return boolean about the success of the method

	 */
	public static boolean convertMaskImage2ColorBatch(String maskImageFolder, MaskColorMap mapping, String outputPath)  {
		
		///////////////////////////////////////////////////////////////
		// getting annotated images to process
		Collection<String> dirMaskFiles = FileOper.readFileDirectory(maskImageFolder);

		// select images with the right suffix
		String suffixTIF = new String(".tif");
		Collection<String> dirSelectMaskFiles = FileOper.selectFileType(dirMaskFiles,suffixTIF );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectMaskFiles,	FileOper.SORT_ASCENDING);
			
		
		String maskFileName = new String();
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			maskFileName = k.next();
			String nameMask = (new File(maskFileName)).getName();
			nameMask = nameMask.substring(0, nameMask.length()-4);
			System.out.println("INFO: name mask = " +  nameMask );
			
			System.out.println("INFO: loading file:"+ maskFileName);
			ImagePlus imgMask = IJ.openImage(maskFileName);
			ImagePlus imgColor = MaskFromColorImage.convertMaskImage2Color(imgMask, mapping);

			FileSaver fs = new FileSaver(imgColor);

			
			if(outputPath.endsWith(File.separator)){
				// color-coded mask
				fs.saveAsPng(outputPath + nameMask + ".png");
			}else{
				fs.saveAsPng(outputPath + File.separator + nameMask + ".png");			}

	
		}
		return true;
	}
	
	
	//Converts Dr. Feldman's most recent PNG annotations into grayscale TIFF image tiles
	/**
	 * This method converts Color Images (e.g., PNG file format) with pre-defined colors assigned to each color
	 * and creates a grayscale mask image
	 * 
	 * @param annotatedImageFolder - input color images with annotations - assumed to be in PNG file format!!
	 * @param mapping - predefined mapping between Colors and grayscale mask values
	 * @param outputPath - output folder for mask images
	 * 
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public static boolean convertColorImage2Mask(String annotatedImageFolder, String fileSuffix, MaskColorMap mapping, String outputPath) throws IOException, FormatException {
	
		/*
		 // this code is commented because of the bioformats library 
		   
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
		*/
		
		///////////////////////////////////////////////////////////////
		// getting annotated images to process
		Collection<String> dirRawFiles = FileOper.readFileDirectory(annotatedImageFolder);

		// select images with the right suffix
		//String suffixPNG = new String(".png");
		Collection<String> dirSelectAnnotFiles = FileOper.selectFileType(dirRawFiles,fileSuffix );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectAnnotFiles,
				FileOper.SORT_ASCENDING);
			
		
		String AnnotFileName = new String();
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			AnnotFileName = k.next();
			String nameAnnot = (new File(AnnotFileName)).getName();
			nameAnnot = nameAnnot.substring(0, nameAnnot.length()-4);
			System.out.println("INFO: nameAnnot = " +  nameAnnot );
			// annotated color image
			//BufferedImage annots = ImageIO.read(new File(AnnotFileName));
			//ImagePlus annotated = new ImagePlus("temp", annots);
			ImagePlus annotated = IJ.openImage(AnnotFileName);
			if(annotated == null){
				System.err.println("ERROR could not open "+ AnnotFileName);
				continue;
			}
			ImageProcessor ip = annotated.getProcessor();
			ColorProcessor colorIP = (ColorProcessor) ip.convertToRGB();
			// mask grayscale image - force to be 8 BPP !!! because otherwise the pre-defined labels
			// get messed up
			ImageProcessor maskIP = ip.convertToByte(false).duplicate();
			
			for (int x = 0; x < colorIP.getWidth(); x++) {
				for (int y = 0; y < colorIP.getHeight(); y++) {
					Color pixelColor = colorIP.getColor(x, y);
					int gray = findGrayColorForUniqueRGBColor(pixelColor, mapping.getColor2grayMapping());
					if(gray == 4){
						// the missing regions in annotated images should be labeled as paste or green
						ip.setColor(Color.green);
						ip.drawPixel(x,y);
					}
					maskIP.setColor(new Color(gray,gray,gray));
					maskIP.drawPixel(x, y);
				}
			}
	/*		for (int x = 0; x < colorIP.getWidth(); x++) {
				for (int y = 0; y < colorIP.getHeight(); y++) {
					Color pixelColor = colorIP.getColor(x, y);
					if (pixelColor.getRed() == pixelColor.getBlue() &&
							pixelColor.getBlue() == pixelColor.getGreen()) {
						maskIP.setColor(new Color(64,64,64));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 255 && pixelColor.getGreen() == 255 &&
							pixelColor.getBlue() == 0) {
						maskIP.setColor(new Color(170,170,170));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 0 && pixelColor.getGreen() == 255 &&
							pixelColor.getBlue() == 255) {
						maskIP.setColor(new Color(59,59,59));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 0 && pixelColor.getGreen() == 0 &&
							pixelColor.getBlue() == 255) {
						maskIP.setColor(new Color(106,106,106));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 255 && pixelColor.getGreen() == 0 &&
							pixelColor.getBlue() == 255) {
						maskIP.setColor(new Color(191,191,191));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 255 && pixelColor.getGreen() == 200 &&
							pixelColor.getBlue() == 0) {
						maskIP.setColor(new Color(127,127,127));
						maskIP.drawPixel(x, y);
					} else if (pixelColor.getRed() == 255 && pixelColor.getGreen() == 0 &&
							pixelColor.getBlue() == 0) {
						maskIP.setColor(new Color(85,85,85));
						maskIP.drawPixel(x, y);
					}
				}
			}
	*/
			ImagePlus converted = new ImagePlus("complete", maskIP);
			FileSaver temp = new FileSaver(converted);
			FileSaver tempOrig = new FileSaver(annotated);
			
			if(outputPath.endsWith(File.separator)){
				// grayscale mask
				temp.saveAsTiff(outputPath + "mask_" + nameAnnot + ".tif");
				// completed annotated image for the concrete project
				tempOrig.saveAsTiff(outputPath + "annot_" + nameAnnot + ".tif");
			}else{
				temp.saveAsTiff(outputPath + File.separator + "mask_" + nameAnnot + ".tif");
				tempOrig.saveAsTiff(outputPath + File.separator+ "annot_" + nameAnnot + ".tif");
			}

	
		}
		

/*			reader.setId(outputPath + Integer.toString(i));
			ImageProcessorSource ip3 = new ImageProcessorSource(reader);
			ImageProcessor ip4 = (ImageProcessor) ip3.getObject(0);
			createTiles(ip4);*/
			
		//}
	
		//Close reader.
		//reader.close();
		return true;
	}

	
	public static Color findColorForUniqueGray(int pixelGray, HashMap <Color, Integer> color2grayMapping ){
		// sanity check
		if (pixelGray < 0 || pixelGray > 255) {
			System.err.println("ERROR: pixelGray should be within [0,255] but it is "+ pixelGray);
			return null;
		}
		if (color2grayMapping.size() < 1) {
			System.out.println("WARNING: empty color2grayMapping");
			return null;
		}
		Iterator<Color> keySetIterator = color2grayMapping.keySet().iterator();

		while(keySetIterator.hasNext()){
		  Color key = keySetIterator.next();
		  //System.out.println("Color key: " + key.toString() + " value: " + _color2grayMapping.get(key));
		  if(color2grayMapping.get(key) == pixelGray){
			  return key;
		  }
		}

		//System.out.println("WARNING: unrecognized pixelGray =" + pixelGray + " in the image w.r.t. color2grayMapping. "); 	
		
		return (Color.BLACK);
	}
	
	/**
	 * This method will find the gray value for a pixel among the pre-specified Colors of classes
	 *  
	 * @param pixelColor - Color of a pixel
	 * @param color2grayMapping - mapping definition
	 * 
	 * @return integer value of a grayscale value
	 */
	public static int findGrayColorForUniqueRGBColor(Color pixelColor, HashMap <Color, Integer> color2grayMapping ){
		// sanity check
		if (pixelColor == null) {
			System.err.println("ERROR: missing uniqueColor");
			return -1;
		}
		if (color2grayMapping.size() < 1) {
			System.out.println("WARNING: empty color2grayMapping");
			return -1;
		}
		int gray = -1;
		Iterator<Color> keySetIterator = color2grayMapping.keySet().iterator();

		while(keySetIterator.hasNext()){
		  Color key = keySetIterator.next();
		  //System.out.println("Color key: " + key.toString() + " value: " + _color2grayMapping.get(key));
		  if(key.getRed() == pixelColor.getRed() && key.getGreen() == pixelColor.getGreen() && key.getBlue() == pixelColor.getBlue()){			  
			  return color2grayMapping.get(key);
		  }
		}
		// check whether it is a grayscale value
		if(pixelColor.getRed() == pixelColor.getGreen() && pixelColor.getGreen() == pixelColor.getBlue()){		
			// every grayscale value is completed as green or paste class
			gray = 4;
			return gray;
		}
		System.out.println("WARNING: unrecognized color in the image w.r.t. color2grayMapping. Mask Color = 0 or BKG"); 
		gray = 0;			
		return gray;
	}
	

	//Converts RGB image to 8BPP TIFF
	/**
	 * This method was used to convert the test image with 4 quadrants of GT
	 * to a TIFF image
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 * @throws FormatException
	 * @throws IOException
	 */
	public static void convertRGBTiff(String inputFileName, String outputFileName) throws FormatException, IOException {
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

		//Read TIFF image for background and declare name and location of final TIFF file
		reader.setId(inputFileName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);
		ImagePlus imp = new ImagePlus("Temp", ip3);
		FileSaver cropSave = new FileSaver(imp);
		cropSave.saveAsTiff(outputFileName);
		
		//Close reader.
		reader.close();
	}
	
	//Returns # of pixels based on color
	/**
	 * This method is for counting pixels per class in a grayscale converted image
	 * 
	 * Note: The ConcreteMaskColorMap has a function for printing semantic class categories
	 * associated with each class gray value
	 * 
	 * @param inputGrayMaskFileName
	 * @param maxLabelValue
	 * @return
	 * @throws FormatException
	 * @throws IOException
	 */
	public static int [] countPixelsPerColor(String inputGrayMaskFileName, int maxLabelValue) throws FormatException, IOException {
		//sanity check
		if(inputGrayMaskFileName == null){
			System.err.println("ERROR: inputGrayMaskFileName is null");
			return null;
		}
		if(maxLabelValue < 0 ){
			System.err.println("ERROR: maxLabelValue is invalid = " + maxLabelValue);
			return null;
		}
	
		int grayCount [] = new int[maxLabelValue];
		for(int i = 0; i < grayCount.length; i++){
			grayCount[i] = 0 ;
		}
		//////////////////////////////////////////////////////////////////
		//Initialize image reader and writer
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

		reader.setId(inputGrayMaskFileName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);
			//ColorProcessor colorIP = (ColorProcessor) ip.convertToRGB();
		for (int y = 0; y < ip3.getHeight(); y++) {
			for (int x = 0; x< ip3.getWidth(); x++) {
				int currColor = (int) ip3.getPixelValue(x, y);
				//System.out.println(currColor);
				grayCount[currColor] ++;
			}
		}
			
		return grayCount;
	}
	

	/**
	 * This method is for converting gray masks from color completed masks annotated by Steve and completed for all pixels
	 * The conversion creates a binary image with 
	 * FRG (white) = cracks + dissolution + air voids (+ volcanics)
	 * or FRG (white) = quartzite, .... 
	 * and the rest of color annotated pixels being black
	 * 
	 * @param grayMaskImageFolder
	 * @param outputPath
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public static boolean convertGrayMask2BinaryMask(String grayMaskImageFolder, int label, String outputPath) throws IOException, FormatException {

		// sanity check
		if(label < 0 || label > ConcreteMaskColorMap.NUM_OF_CLASSES){			
			System.err.println("ERROR: label is out of range = " + label);
			return false;
		}
		
		///////////////////////////////////////////////////////////////
		// getting annotated images to process
		Collection<String> dirRawFiles = FileOper.readFileDirectory(grayMaskImageFolder);

		// select images with the right suffix
		String suffixTIFF = new String(".tif");
		Collection<String> dirSelectAnnotFiles = FileOper.selectFileType(dirRawFiles,suffixTIFF );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectAnnotFiles,
				FileOper.SORT_ASCENDING);
			
		ImagePlus imgPlus = null;
		String grayMaskFileName = new String();
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			grayMaskFileName = k.next();
			String nameGrayMask = (new File(grayMaskFileName)).getName();
			nameGrayMask = nameGrayMask.substring(0, nameGrayMask.length()-4);
			System.out.println("INFO: colorMaskFileName = " +  nameGrayMask );
			
			imgPlus = IJ.openImage(grayMaskFileName);
			ImageProcessor colorIP = imgPlus.getProcessor();

			// mask grayscale image			
			ImageProcessor maskIP = new ByteProcessor(colorIP.getWidth(), colorIP.getHeight());
			maskIP.set(0);
			
			for (int x = 0; x < colorIP.getWidth(); x++) {
				for (int y = 0; y < colorIP.getHeight(); y++) {
					int gray = colorIP.getPixel(x, y);
					// this is in C:\PeterB\Projects\TestData\concrete_SteveFeldman\TrainingImageMasks\AnnotatedImages2018-07-19\binaryReferenceDamage
					//if(gray == ConcreteMaskColorMap.CRACKS || gray == ConcreteMaskColorMap.DISSOLUTION || gray == ConcreteMaskColorMap.VOIDS){
					// this is in C:\PeterB\Projects\TestData\concrete_SteveFeldman\TrainingImageMasks\AnnotatedImages2018-07-19\binaryReferenceDamageWithVolcanics
					// if(gray == ConcreteMaskColorMap.CRACKS || gray == ConcreteMaskColorMap.DISSOLUTION || gray == ConcreteMaskColorMap.AIR_VOIDS || gray == ConcreteMaskColorMap.VOLCANICS){
					// this is in binaryReferenceAggregate
					//if(gray == ConcreteMaskColorMap.QUARTZITE || gray == ConcreteMaskColorMap.FELDSPAR ){						
					// this is in binaryReferenceVolcanicsDissolution
					//if(gray == ConcreteMaskColorMap.VOLCANICS || gray == ConcreteMaskColorMap.DISSOLUTION ){											
					// this is in binaryReferenceVoids
					//if(gray == ConcreteMaskColorMap.AIR_VOIDS){
					// this is in binaryReferenceDissolution
					//if(gray == ConcreteMaskColorMap.DISSOLUTION ){											
					if(gray == label ){
											
					//if(gray == ConcreteMaskColorMap.QUARTZITE  ){
					//if(gray == ConcreteMaskColorMap.FELDSPAR  ){
					//if(gray == ConcreteMaskColorMap.VOLCANICS){
						maskIP.set(x, y, 255);
					}else{
						maskIP.set(x, y, 0);
					}
				}
			}
	
			ImagePlus converted = new ImagePlus("complete", maskIP);
			FileSaver temp = new FileSaver(converted);

			
			if(outputPath.endsWith(File.separator)){
				// binary mask
				temp.saveAsTiff(outputPath + "binary_" + nameGrayMask + ".tif");

			}else{
				temp.saveAsTiff(outputPath + File.separator + "binary_" + nameGrayMask + ".tif");

			}

	
		}
		

		return true;
	}

	/**
	 * This method is a batch processing of all pixel-level annotated images with multiple class labels 
	 * into binary masks corresponding to each unique label
	 * 
	 * @param grayMaskImageFolder - folder with all grayscale masks
	 * @param outputPath - root folder where all sub-folders with binary masks per class will be placed
	 * @return boolean success
	 * @throws IOException
	 * @throws FormatException
	 */
	public static boolean convertGrayMask2BinaryMaskBatch(String grayMaskImageFolder, String outputPath) throws IOException, FormatException {
		
		boolean ret = true;
		for(int label=1; label<=ConcreteMaskColorMap.NUM_OF_CLASSES; label++){
			String className = new String(ConcreteMaskColorMap.convertClass2String(label));
			String outputPathClass = new String(outputPath + File.separator + "binaryReference" + className);
	        File file = new File(outputPathClass);
	        if (!file.exists()) {
	            if (file.mkdir()) {
	                System.out.println("Directory is created!");
	            } else {
	                System.out.println("Failed to create directory!");
	            }
	        }
	        
			ret &= MaskFromColorImage.convertGrayMask2BinaryMask(grayMaskImageFolder, label, outputPathClass);
				
		}
		return ret;
	}

		
	/**
	 * @param args
	 * @throws FormatException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, FormatException {
		
		// sanity check
/*		if(args == null || args.length < 3){
			System.err.println("expected three argumants: annotatedPath, unannotatedPath and output Mask Filename");
			return;			
		}

		MaskFromColorImage myClass = new MaskFromColorImage();
		
		String annotatedPath = args[0];
		String unannotatedPath = args[1];
		String outFileFolder = args[2];	
		System.out.println("args[0] annotatedPath="+annotatedPath);
		System.out.println("args[1] unannotatedPath=" + unannotatedPath);
		System.out.println("args[2] outFileFolder="+outFileFolder);
*/
		
/*		// Step 1: 
		//This code is used for preparing 12 pixel-level annotated images from color annotated PNG files from Steve 
		//String annotatedPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\annotatedFOV\\");
		//String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\grayMask2\\");;	
		String annotatedPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\grayMasks\\");;	
		
		System.out.println(" annotatedPath="+annotatedPath);
		System.out.println("outFileFolder="+outFileFolder);
		ConcreteMaskColorMap mapping  = new ConcreteMaskColorMap();
	
		boolean ret = MaskFromColorImage.convertColorImage2Mask(annotatedPath, mapping, outFileFolder);
		System.out.println("color to gray mapping");
		mapping.printColor2grayMapping();*/
		
/*		//String inputGrayMaskFileName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\grayMasks\\mask_Sample_(4,23).tif");
		String inputGrayMaskFileName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\grayMasks\\mask_Sample_(9,35).tif");
			
		int  maxLabelValue = 8;
		int [] pixelCount = MaskFromColorImage.countPixelsPerColor(inputGrayMaskFileName,  maxLabelValue); 
		String str = ConcreteMaskColorMap.reportPixelCountPerClass(pixelCount);*/
		
		
		//creates individual binary masks for each class
/*  		String colorMaskImageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\grayMasks");
		String outputPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\binaryReferenceDissolution\\");
		boolean ret = MaskFromColorImage.convertGrayMask2BinaryMask(colorMaskImageFolder, ConcreteMaskColorMap.DISSOLUTION, outputPath);*/

		// Step 2: move the color versions of the mask from grayMasks folder out before running this step
		// updated 12 pixel-level annotated images from Steve
/*  		String grayMaskImageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\grayMasks");
		String outputPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019");
		boolean ret = MaskFromColorImage.convertGrayMask2BinaryMaskBatch(grayMaskImageFolder, outputPath);
*/		
	
		// convert all test masks
		//String grayMaskImageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\grayMasks");
		//String outputPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\composedGrayMasks\\");

		
		// convert all masks derived from Steve's annotations of damage layers
		//String grayMaskImageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\December2018\\output\\processedAnnotModel");
		//String outputPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\December2018\\output\\processedAnnotModelColor\\");

/*		String grayMaskImageFolder = new String("Y:\\wippAssist\\segnet-inference-20190510T133909-0");
		String outputPath = new String("Y:\\wippAssist\\segnet-inference-20190510T133909-0_PNG\\");*/
		
/*		String grayMaskImageFolder = new String("X:\\contextAssist\\unet-inference");
		String outputPath = new String("X:\\contextAssist\\unet-inference_PNG"+File.separator);*/
		
		String grayMaskImageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\manualAnnotMike\\grayMasks");
		String outputPath = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\manualAnnotMike\\colorMask"+File.separator);
		
		ConcreteMaskColorMap mapping = new ConcreteMaskColorMap();
		//HashMap<Color, Integer> convert_mapping = mapping.getColor2grayMapping();
		boolean ret = MaskFromColorImage.convertMaskImage2ColorBatch(grayMaskImageFolder,mapping, outputPath);

		/*
		//////////////////////////////////////////////////////////////////////
		// this code is used to generate color legends for the web based validation
		int numLabels = 3;
		int [] classLabels = new int[numLabels];
		classLabels[0] = ConcreteMaskColorMap.AGGREGATE_DAMAGE;
		classLabels[1] = ConcreteMaskColorMap.PASTE_DAMAGE;
		//classLabels[2] = ConcreteMaskColorMap.VOLCANIC_DAMAGE;
		classLabels[2] = ConcreteMaskColorMap.AIR_VOIDS;
	*/
/*		int numLabels = 9;
		int [] classLabels = new int[numLabels];
		classLabels[0] = ConcreteMaskColorMap.AIR_VOIDS;
		classLabels[1] = ConcreteMaskColorMap.AUGITE;
		classLabels[2] = ConcreteMaskColorMap.CRACKS;
		classLabels[3] = ConcreteMaskColorMap.DISSOLUTION;
		classLabels[4] = ConcreteMaskColorMap.FELDSPAR;
		classLabels[5] = ConcreteMaskColorMap.IRON_OXIDE;
		classLabels[6] = ConcreteMaskColorMap.PASTE;
		classLabels[7] = ConcreteMaskColorMap.QUARTZITE;
		classLabels[8] = ConcreteMaskColorMap.VOLCANIC_GLASS;
		
		*/
		
		/*
		int widthPerLabel = 150;
		int heightPerLabel = 50;
		ConcreteMaskColorMap mapping  = new ConcreteMaskColorMap();
	
		ImagePlus grayLegend = ConcreteMaskColorMap.createGrayLegend(classLabels, widthPerLabel, heightPerLabel);
			
		ImagePlus colorLegend = MaskFromColorImage.convertMaskImage2Color(grayLegend, mapping);
		
		String outputFileName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\legendDamage.png");
		FileSaver fs = new FileSaver(colorLegend);
		fs.saveAsPng(outputFileName);
		System.out.println("Done: saved file = " + outputFileName );
			*/
	}

}
