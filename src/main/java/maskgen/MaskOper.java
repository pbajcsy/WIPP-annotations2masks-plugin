package maskgen;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import datatype.ConcreteMaskColorMap;
import datatype.MaskLabelMap;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import io.CSV_IOsupport;
import util.FileOper;

public class MaskOper {
	
	/**
	 * This method is for inserting a layer into an inputMask image over a filterMask image. The layer will have a value
	 * setValue and it will or will not overwrite the previous non-zero values in inputMask
	 * 
	 * @param inputMask - input mask image
	 * @param filterMask - filter mask image
	 * @param setValue - new value to be inserted 
	 * @param writeOver - boolean flag to indicate whether an existing non-zero value should be overwritten
	 * @return - created new ImagePlus object/mask image
	 */
	public static ImagePlus setValues(ImagePlus inputMask, ImagePlus filterMask, int setValue, boolean writeOver){
		//sanity check
		if(inputMask == null || filterMask == null){
			System.err.println("ERROR: input image(s) should not be null");
			return null;
		}
		ImageProcessor ip1 = inputMask.getProcessor();
		ImageProcessor ip2 = filterMask.getProcessor();
				
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		if(width != ip2.getWidth() || height != ip2.getHeight()){
			System.err.println("ERROR: input images do not have the same size");
			return null;			
		}	
		
		ByteProcessor ip_byte = new ByteProcessor(width, height);
		ImagePlus imRes = new ImagePlus("Result", ip_byte);
		ip_byte.set(0);
		
		int v1, v2, v3;
		for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	v1 = ip1.getPixel(x,y);//inputMask
	        	v2 = ip2.getPixel(x,y);//filterMask
	        	if (v2 != 0){
	        		// if filter mask is foreground
	        		if(writeOver){
	        			// if the filter mask overwrites any existing pixel values in inputMask
	        			ip_byte.putPixelValue(x, y, (byte)setValue);
	        		}else{
	        			if(v1 == 0){
	        				// write at the filter mask location only  if the inputMask is background
	        				ip_byte.putPixelValue(x, y, (byte)setValue);
	        			}else{
	        				ip_byte.putPixelValue(x, y, (byte)v1);
	        			}
	        		}
	        	}else{
	        		ip_byte.putPixelValue(x, y, (byte)v1);
	        	}

	        }   
	    }
	   imRes.setImage(imRes);
	   
	   return  imRes;	
			
	}

	/** 
	 * This method is for setting the background pixels to a specified value
	 * 
	 * @param inputMask
	 * @param setValue
	 * @return
	 */
	public static ImagePlus setBKGValues(ImagePlus inputMask, int setValue){
		//sanity check
		if(inputMask == null){
			System.err.println("ERROR: input image should not be null");
			return null;
		}
		ImageProcessor ip1 = inputMask.getProcessor();
				
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		
		ByteProcessor ip_byte = new ByteProcessor(width, height);
		ImagePlus imRes = new ImagePlus("Result", ip_byte);
		ip_byte.set(0);
		
		int v1, v2, v3;
		for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	v1 = ip1.getPixel(x,y);//inputMask
	        	if (v1 == 0){
        			ip_byte.putPixelValue(x, y, (byte)setValue);
	        	}else{
	        		ip_byte.putPixelValue(x, y, (byte)v1);
	        	}
	        }   
	    }
	   imRes.setImage(imRes);
	   
	   return  imRes;	
			
	}
	
	public static int [] histogram(ImagePlus grayMask, int maxUINT_binValue){
		//sanity check
		if(grayMask == null){
			System.err.println("ERROR: input image for histogram should not be null");
			return null;
		}
		ImageProcessor ip1 = grayMask.getProcessor();
		
/*		if(!ip1.isBinary()){
			System.err.println("ERROR: input image for histogram should be binary");
			return null;			
		}*/
		
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		
		int [] hist = new int[maxUINT_binValue + 1];
		for(int idx=0;idx<hist.length;idx++)
			hist[idx] = 0;
		
		int v1;
		for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	v1 = ip1.getPixel(x,y);//inputMask
	        	if (v1 >= 0 && v1 <= maxUINT_binValue ){
	        		hist[v1] ++;
	        	}
	        }   
	    }
   
	   return  hist;		
	}

	/**
	 * This method places a set of mask layers onto the final mask
	 * used in a concrete project where the order is defined based on our confidence in detecting 
	 * labels in each mask layer as denoted in the input list of arguments
	 * 
	 * @param rawFileOne
	 * @param damageFileOne
	 * @param dissolutionFileOne
	 * @param volcanicFileOne
	 * @param voidsFileOne
	 * @param quartziteFileOne
	 * @param feldsparFileOne
	 * @param IRON_OXIDEFileOne
	 * @param outFileOne
	 * @return
	 */
	public static ImagePlus compose(String rawFileOne,String damageFileOne, String dissolutionFileOne,  String volcanicFileOne, String voidsFileOne, String quartziteFileOne, String feldsparFileOne, String augiteFileOne, String iron_oxideFileOne, String volcanic_damageFileOne, String outFileOne ){
		long start_time = System.currentTimeMillis();

		System.out.println("INFO: loading file:"+ rawFileOne);
		ImagePlus imgRaw = IJ.openImage(rawFileOne);
		if(imgRaw == null){
			System.err.println("ERROR in compose: could not open "+ rawFileOne);
			return null;
		}
		imgRaw.getProcessor().set(0);
		
		int setValue;
		boolean writeOver;
		ImagePlus res = null;

		if(new File(damageFileOne).exists()){
			System.out.println("INFO: loading file:"+ damageFileOne);
			ImagePlus imgDamage = IJ.openImage(damageFileOne);
			setValue = ConcreteMaskColorMap.CRACKS;
			writeOver = false;
			if(imgDamage != null){
				res = MaskOper.setValues(imgRaw, imgDamage, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ damageFileOne);
			}
		}
		/////////////////////////////////////////////////
		if(new File(dissolutionFileOne).exists()){
			System.out.println("INFO: loading file:"+ dissolutionFileOne);
			ImagePlus imgDissolution = IJ.openImage(dissolutionFileOne);
			setValue = ConcreteMaskColorMap.DISSOLUTION;
			writeOver = true;
			if(imgDissolution != null){
				res = MaskOper.setValues(res, imgDissolution, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ dissolutionFileOne);
			}
		}
		/////////////////////////////////////////////////
		if(new File(volcanicFileOne).exists()){
			System.out.println("INFO: loading file:"+ volcanicFileOne);
			ImagePlus imgVolcanic = IJ.openImage(volcanicFileOne);
			setValue = ConcreteMaskColorMap.VOLCANIC_GLASS;
			writeOver = true;
			if(imgVolcanic != null){
				res = MaskOper.setValues(res, imgVolcanic, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ volcanicFileOne);
			}
		}
		/////////////////////////////////////////////////
		if(new File(volcanic_damageFileOne).exists()){
			System.out.println("INFO: loading file:"+ volcanic_damageFileOne);
			ImagePlus imgVolcanicDamage = IJ.openImage(volcanic_damageFileOne);
			setValue = ConcreteMaskColorMap.VOLCANIC_DAMAGE;
			writeOver = true;
			if(imgVolcanicDamage != null){
				res = MaskOper.setValues(res, imgVolcanicDamage, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ volcanic_damageFileOne);
			}
		}		
		/////////////////////////////////////////////////
		if(new File(quartziteFileOne).exists()){
			System.out.println("INFO: loading file:"+ quartziteFileOne);
			ImagePlus imgQuartzite = IJ.openImage(quartziteFileOne);
			setValue = ConcreteMaskColorMap.QUARTZITE;
			writeOver = true;
			if(imgQuartzite != null){
				res = MaskOper.setValues(res, imgQuartzite, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ quartziteFileOne);
			}
		}
		/////////////////////////////////////////////////
		if(new File(feldsparFileOne).exists()){
			System.out.println("INFO: loading file:"+ feldsparFileOne);
			ImagePlus imgFeldspar = IJ.openImage(feldsparFileOne);
			setValue = ConcreteMaskColorMap.FELDSPAR;
			writeOver = true;
			if(imgFeldspar != null){
				res = MaskOper.setValues(res, imgFeldspar, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ feldsparFileOne);
			}	
		}
		/////////////////////////////////////////////////
		if(new File(augiteFileOne).exists()){
			System.out.println("INFO: loading file:"+ augiteFileOne);
			ImagePlus imgAugite = IJ.openImage(augiteFileOne);
			setValue = ConcreteMaskColorMap.AUGITE;
			writeOver = true;
			if(imgAugite != null){
				res = MaskOper.setValues(res, imgAugite, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ augiteFileOne);
			}	
		}
		/////////////////////////////////////////////////
		if(new File(iron_oxideFileOne).exists()){
			System.out.println("INFO: loading file:"+ iron_oxideFileOne);
			ImagePlus imgIRON_OXIDE = IJ.openImage(iron_oxideFileOne);
			setValue = ConcreteMaskColorMap.IRON_OXIDE;
			writeOver = true;
			if(imgIRON_OXIDE != null){
				res = MaskOper.setValues(res, imgIRON_OXIDE, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ iron_oxideFileOne);
			}	
		}
		/////////////////////////////////////////////////
		if(new File(voidsFileOne).exists()){
			System.out.println("INFO: loading file:"+ voidsFileOne);
			ImagePlus imgVoids = IJ.openImage(voidsFileOne);
			setValue = ConcreteMaskColorMap.AIR_VOIDS;
			writeOver = true;
			if(imgVoids != null){
				res = MaskOper.setValues(res, imgVoids, setValue, writeOver);
			}else{
				System.out.println("INFO in compose: missing file "+ voidsFileOne);
			}	
		}
			
		/////////////////////////////////////////////////
		// assign paste label to all background pixels
		setValue = ConcreteMaskColorMap.PASTE;
		res = MaskOper.setBKGValues(res, setValue);
		
		//////////////////////////////////
		// save the mask as grayscale TIF and color PNG
		File output = new File(outFileOne);
		String fileName = new String(output.getName());
		String filePath = output.getAbsolutePath();
		// create the output folder if it does not exist
		File dir = new File(filePath.substring(0, filePath.length() - fileName.length()));
		if(!dir.exists()){
			dir.mkdir();
		}else{
			if(!dir.canWrite()){
				System.err.println("ERROR: cannot write to the output folder = " + filePath);
				return null;
			}
		}

		
		FileSaver	fs = new FileSaver(res);
		fs.saveAsTiff(outFileOne);

		ConcreteMaskColorMap mapping = new ConcreteMaskColorMap();
		ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(res,  mapping) ;
			
		FileSaver fs2 = new FileSaver(colorRes);
		String PNG_outputName = new String(outFileOne);
		//PNG_outputName = PNG_outputName.substring(0, PNG_outputName.length()-4);
		int idxDot = PNG_outputName.lastIndexOf(".");
		PNG_outputName = PNG_outputName.substring(0, idxDot);
		PNG_outputName = PNG_outputName + ".png";
		fs2.saveAsPng(PNG_outputName);			
				
		final long load_time = System.currentTimeMillis();
		System.out.println("Loading 3 images took " + Long.toString(load_time - start_time) + " milliseconds");	
		
		return res;
	}
	
	/**
	 * THis method automates composition of masks from layers over a collection of images
	 */
	public static void composeBatch(){

		// TODO Auto-generated method stub
		String fileFolderRoot = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\");		
		String rawFileNameFolder = new String(fileFolderRoot + "rawFOV\\");

		///////////////////////////////////////////////////////////////
		// getting annotated images to process
		Collection<String> dirMaskFiles = FileOper.readFileDirectory(rawFileNameFolder);
		// select images with the right suffix
		String suffixTIF = new String(".tif");
		Collection<String> dirSelectMaskFiles = FileOper.selectFileType(dirMaskFiles,suffixTIF );	
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectMaskFiles,	FileOper.SORT_ASCENDING);
		
		if(sortedImagesInFolder.size() < 1){
			System.err.println("ERROR: did not find any files in " + rawFileNameFolder);
			return;				
		}
		////////////////////////////////////////////////////////////////
		
		String fileNameAll = new String("Sample_(4,23)");
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			fileNameAll = k.next();
			String fileName = (new File(fileNameAll)).getName();
			fileName = fileName.substring(0, fileName.length()-4);
			System.out.println("INFO: name raw = " +  fileName );
			

			String rawFileOne = new String(fileFolderRoot+"rawFOV\\"+fileName+".tif");
			
			String damageFileFolder = new String(fileFolderRoot+"binaryReferenceDamage\\optimalAverage_t61_s247\\");
			String damageFileOne = new String(damageFileFolder+fileName+"_bySize_t61_s247.tif");
			
			String dissolutionFileFolder = new String(fileFolderRoot+"optimalDissolutionDetection\\optimalAverage\\");
			String dissolutionFileOne = new String(dissolutionFileFolder+fileName+".tif");

			String volcanicFileFolder = new String(fileFolderRoot+"optimalVolcanicDetection\\optimalVolcanic_run3\\");
			String volcanicFileOne = new String(volcanicFileFolder+fileName+".tif");
			
			String volcanicDamageFileFolder = new String(fileFolderRoot+"optimalVolcanicDetection\\optimalVolcanic_run3\\");
			String volcanicDamageFileOne = new String(volcanicFileFolder+fileName+"_VolcanicDamage.tif");
			
			String voidsFileFolder = new String(fileFolderRoot+"optimalVoidsDetection\\optimalAverage_run3\\");
			String voidsFileOne = new String(voidsFileFolder+fileName+".tif");		
			
			String quartziteFileFolder = new String(fileFolderRoot+"optimalAggregateDetection\\averageValues_run2\\");
			String quartziteFileOne = new String(quartziteFileFolder+fileName+"_Quartzite.tif");
			
			String feldsparFileFolder = new String(fileFolderRoot+"optimalAggregateDetection\\averageValues_run2\\");
			String feldsparFileOne = new String(feldsparFileFolder+fileName+"_Feldspar.tif");

			String augiteFileFolder = new String(fileFolderRoot+"optimalAggregateDetection\\averageValues_run2\\");
			String augiteFileOne = new String(feldsparFileFolder+fileName+"_Augite.tif");
			
			String iron_oxideFileFolder = new String(fileFolderRoot+"optimalAggregateDetection\\averageValues_run2\\");
			String iron_oxideFileOne = new String(iron_oxideFileFolder+fileName+"_ironOxide.tif");
			
			String outputFileFolder = new String(fileFolderRoot+"composedGrayMasks\\");
			String outFileOne = new String(outputFileFolder+fileName+".tif");
			
			
			MaskOper.compose(rawFileOne,damageFileOne, dissolutionFileOne, volcanicFileOne, voidsFileOne, quartziteFileOne, feldsparFileOne, augiteFileOne, iron_oxideFileOne, volcanicFileOne, outFileOne);
			
		}
	}

	/**
	 * This method will keep in the raw image only the pixels that are inside of a mask region
	 * all other pixels will be set to setValue
	 * @param inputRaw - input raw image
	 * @param inputMask - input mask image with non-zero value for a foreground mask
	 * @param setValue - integer value to be assigeed to all background pixels (outside of non-zero mask region)
	 * 
	 * @return resulting ImagePlus object
	 */
	public static ImagePlus applyMask(ImagePlus inputRaw, ImagePlus inputMask, int setValue){
		//sanity check
		if(inputRaw == null){
			System.err.println("ERROR: input raw image should not be null");
			return null;
		}
		if(inputMask == null){
			System.err.println("ERROR: input mask image should not be null");
			return null;
		}
		ImageProcessor ip_raw = inputRaw.getProcessor();
		ImageProcessor ip_mask = inputMask.getProcessor();
					
		int width = ip_raw.getWidth();
		int height = ip_raw.getHeight();
		if(width != ip_mask.getWidth() || height != ip_mask.getHeight()) {
			System.err.println("ERROR: input raw and mask images have different dimensions: raw w=" + width + ", h=" + height + " mask w=" + ip_mask.getWidth() + ", h=" + ip_mask.getHeight() );
			return null;			
		}
	
		ImageProcessor ip_result = (ImageProcessor) ip_raw.clone();
		
		//ByteProcessor ip_byte = new ByteProcessor(width, height);
		//ImagePlus imRes = new ImagePlus("Result", ip_byte);
		//ip_byte.set(setValue);
		ImagePlus imRes = new ImagePlus("Result", ip_result);

		
		int v1, v2;
		for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	v1 = ip_mask.getPixel(x,y);//inputMask
				/*
				 * if (v1 != 0){ v2 = ip_raw.getPixel(x,y); ip_result.putPixelValue(x, y, v2); }
				 */
	        	if (v1 == 0){
	        		ip_result.putPixelValue(x, y, 0);
	        	}
	        }   
	    }
	   imRes.setImage(imRes);
	   
	   return  imRes;	
	}
	
	public static boolean applyMask_batch(String inputRawFileFolder, String maskFileFolder, String outputFileFolder) {
		if (inputRawFileFolder == null || maskFileFolder == null || outputFileFolder == null) {
			System.err.println(
					"ERROR: missing one of the input locations for raw or mask file folder or output location ");
			return false;
		}

		////////////////////////////////////////////////////////
		Collection<String> dirfiles = FileOper.readFileDirectory(inputRawFileFolder);
		System.out.println("Directory Collection Size=" + dirfiles.size());
		// FileOper.printCollection(dirfiles);
		System.out.println();
		System.out.println();
		// select TIFF files with the right suffix
		String suffixTIFF = new String(".tif");
		Collection<String> onlyimages = FileOper.selectFileType(dirfiles, suffixTIFF);
		if (onlyimages.size() == 0) {
			System.err.println("Raw Directory List Collection size is zero for TIF images");
			return false;
		}
		// sort images to process since the renaming and folder placement depend on the
		// time stamp
		Collection<String> sortedRawInFolder = FileOper.sort(onlyimages, FileOper.SORT_ASCENDING);

		System.out.println("filtered and sorted Collection Size=" + onlyimages.size());
		FileOper.printCollection(sortedRawInFolder);
		/////////////////////////////////////////////////////////////////////////
		Collection<String> dirfiles_mask = FileOper.readFileDirectory(maskFileFolder);
		System.out.println("Mask Directory Collection Size=" + dirfiles.size());
		System.out.println();
		System.out.println();
		// select TIFF files with the right suffix
		//String suffixTIFF = new String(".tif");
		Collection<String> onlyMasks = FileOper.selectFileType(dirfiles_mask, suffixTIFF);
		if (onlyMasks.size() == 0) {
			System.err.println(" Mask Directory List Collection size is zero for TIF images");
			return false;
		}
		// sort images to process since the renaming and folder placement depend on the
		// time stamp
		Collection<String> sortedMaskInFolder = FileOper.sort(onlyMasks, FileOper.SORT_ASCENDING);

		System.out.println("filtered and sorted Collection Size=" + onlyMasks.size());
		FileOper.printCollection(sortedMaskInFolder);
		/////////////////////////////////////////////////////////////////////////
		// create the output folder if it does not exist
		//int delta = (int) deltas[0];
		//String maskOutput = new String(OutFileRootName + "mask_" + delta + File.separator);
		String maskOutput = new String(outputFileFolder);
		File directory = new File(maskOutput);
		if (!directory.exists()) {
			directory.mkdir();
			System.out.println("output Directory was created: " + maskOutput);
		}
		/////////////////////////////////////////////////////////
		String rawFileName = null;
		String nameTIFF = null;
		int lastDot;
		
		for (Iterator<String> k = sortedMaskInFolder.iterator(); k.hasNext();) {
			String maskFileName = k.next();
			File maskFile = new File(maskFileName); 
			lastDot = maskFile.getName().lastIndexOf(".");
			//String onlyMaskFileName = maskFile.getName().substring(17, lastDot);//for cell regions: rgb_128_255_128__B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
			//String onlyMaskFileName = maskFile.getName().substring(13, lastDot);// for ROI: Light_Yellow_B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif		
			// find matching rawFileName
			String onlyMaskFileName = maskFile.getName().substring(0, lastDot);
			boolean foundMatch = false;
			for (Iterator<String> r = sortedRawInFolder.iterator(); !foundMatch && r.hasNext();) {
				rawFileName = r.next();
				File rawFile = new File(rawFileName);				// 
				lastDot = rawFile.getName().lastIndexOf(".");
				nameTIFF = rawFile.getName().substring(0, lastDot);// for raw images: B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
				//nameTIFF = rawFile.getName().substring(10, lastDot);// for ROI from vesicleDetected: Segmented_B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
				if (onlyMaskFileName.equalsIgnoreCase(nameTIFF)) {
					foundMatch = true;
				}
			}
			if (!foundMatch) {
				System.err.println("ERROR: could not find a matching raw TIFF image to the maskTIFF  file");
				continue;
			}
			System.out.println("INFO: matching pair: mask TIFF = " + onlyMaskFileName + " TIFF = " + rawFileName);

			////////////////////////////////////////////
			// apply the mask as a filter
			System.out.println("INFO: load raw image = " + rawFileName);		
			ImagePlus inputRaw = IJ.openImage(rawFileName);
			System.out.println("INFO: load mask image = " + maskFileName);		
			ImagePlus inputMask = IJ.openImage(maskFileName);

			ImagePlus resImage = applyMask(inputRaw, inputMask, 0);
			
			//////////////////////////////////
			// save the resulting image as grayscale TIF 
			
			String outFileName = new String(outputFileFolder);
			File dir = new File(outputFileFolder);
			if(!dir.exists()){
				dir.mkdir();
			}else{
				if(!dir.canWrite()){
					System.err.println("ERROR: cannot write to the output folder = " + outputFileFolder);
					return false;
				}
			}
			outFileName += (new File(rawFileName)).getName();
			System.out.println("Saving " + outFileName);
			FileSaver	fs = new FileSaver(resImage);
			fs.saveAsTiff(outFileName);
		}
		
		return true;		
	}

		
	public static void main(String[] args) throws IOException {
		//////////////////////////////////////////////////////////
		// test histogram
	/*	String fileName = new String("Sample__9_35_.ome");
				
		String fileFolderRoot = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\DryRun\\");
		String maskContextFileOne = new String(fileFolderRoot+"finalContextMasks\\"+fileName+"_context.tif");
		String maskDamageFileOne = new String(fileFolderRoot+"finalDamageMasks\\"+fileName+"_damage.tif");
		
		
		String outputPath = new String(fileFolderRoot); //new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\composedGrayMasks\\");
		System.out.println("INFO: load image = " + maskContextFileOne);		
		ImagePlus finalContextMask = IJ.openImage(maskContextFileOne);
		int [] grayCount = MaskOper.histogram(finalContextMask, ConcreteMaskColorMap.NUM_OF_CLASSES);
		System.out.println("INFO: load image = " + maskDamageFileOne);		
		ImagePlus finalDamageMask = IJ.openImage(maskDamageFileOne);
		int [] grayCountDamage = MaskOper.histogram(finalDamageMask, ConcreteMaskColorMap.NUM_OF_CLASSES);	
		
		//boolean withHeader = true;
		// test reportPixelCountPerClass
		//String pixeCount = ConcreteMaskColorMap.reportPixelCountPerClass(grayCount, withHeader);
		//System.out.println("Pixel count" + pixeCount);
			
		//////////////////////////////////////////////
		// test compose
		// convert a folder of per category masks to color and gray combined masks
		//MaskOper.composeBatch();
		
		//////////////////////////////////////////////////
		// 7. compute pixel counts/statistics
		System.out.println("7. report pixel statistics");	
		// this section was disabled since the count of AIR_VOIDS in contextual masks is different from the count in damage masks due to neighborhood re-assignment 
		// copy  the no-damage and ConcreteMaskColorMap.AGGREGATE_DAMAGE and ConcreteMaskColorMap.PASTE_DAMAGE counts since these three classes were modified by 
		// neighborhoodAssignment and context2damageAssignment methods
		//grayCount[0] = grayCountDamage[0];
		//grayCount[ConcreteMaskColorMap.AGGREGATE_DAMAGE] = grayCountDamage[ConcreteMaskColorMap.AGGREGATE_DAMAGE];
		//grayCount[ConcreteMaskColorMap.PASTE_DAMAGE] = grayCountDamage[ConcreteMaskColorMap.PASTE_DAMAGE];
		
		boolean withHeader = false;
		String pixelCount = ConcreteMaskColorMap.reportPixelCountPerClass(grayCount, withHeader);
		String pixelCountDamage = ConcreteMaskColorMap.reportPixelCountPerClass(grayCountDamage, withHeader);

		////////////////////////////////////////////
		// 8. save statistics and log information
		//System.out.println("Pixel count: " + pixelCount);
		int millis = -1; // random number
		String ret = new String(fileName + ", " + pixelCount + Long.toString(millis));
		String retDamage = new String(fileName + ", " + pixelCountDamage + Long.toString(millis));
		
		String statsFile = new String(outputPath + "pixelStats.csv");
		String statsFileDamage = new String(outputPath + "pixelStatsDamage.csv");
		
		// this is for including the header only for the first file in  the batch
		boolean headerInclude = true;
		
		if(headerInclude){
			withHeader = true;
			String header  = ConcreteMaskColorMap.reportPixelCountPerClass(null, withHeader);
			withHeader = false;
			header = "file name, " + header + " Compute time [ms]";
			if(!CSV_IOsupport.SaveAppendArray(header, statsFile)){
				System.err.println("ERROR in : failed to append the string =" + statsFile);
				//return null;
			}
			if(!CSV_IOsupport.SaveAppendArray(header, statsFileDamage)){
				System.err.println("ERROR in : damage - failed to append the string =" + statsFileDamage);
				//return null;
			}
		}
		if(!CSV_IOsupport.SaveAppendArray(ret, statsFile)){
			System.err.println("ERROR in : failed to append the string =" + statsFile);
			//return null;
		}
		if(!CSV_IOsupport.SaveAppendArray(retDamage, statsFileDamage)){
			System.err.println("ERROR in : damage - failed to append the string =" + statsFileDamage);
			//return null;
		}		
*/
	
		/*
		 // organelle project
		String inputRawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\images8BPP\\");
		String maskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\cellRegionMask\\interpolate\\");
		String outputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\cellFiltered\\");

		MaskOper.applyMask_batch(inputRawFileFolder, maskFileFolder, outputFileFolder);
				
		String inputRawFileFolder2 = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\images8BPP\\");
		String maskFileFolder2 = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\focusedROIMask\\interpolate\\");				
		String outputFileFolder2 = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\roiFiltered\\");
			
		MaskOper.applyMask_batch(inputRawFileFolder2, maskFileFolder2, outputFileFolder2);
*/
		
		// ncnr project
		//String inputRawFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\2400_Projections_Reconstructed_SNR\\");
		//String maskFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\masks\\roiMasks\\interpolate\\");				
		//String outputFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\masks\\roiFiltered\\");
			
		String inputRawFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\80-Projections_Reconstructed-20200212T124205Z\\80_Projections_Reconstructed_SNR\\");
		String maskFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\80-Projections_Reconstructed-20200212T124205Z\\masks\\roiMasks\\interpolate\\");				
		String outputFileFolder2 = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\80-Projections_Reconstructed-20200212T124205Z\\masks\\roiFiltered\\");
	
		MaskOper.applyMask_batch(inputRawFileFolder2, maskFileFolder2, outputFileFolder2);

	}

}
