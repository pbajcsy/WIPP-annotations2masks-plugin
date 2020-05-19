package workflows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import datatype.Annotation;
import datatype.ConcreteMaskColorMap;
import datatype.ConcreteMaskLabelMap;
import datatype.OrganelleMaskColorMap;
import datatype.OrganelleMaskLabelMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import io.AnnotationLoader;
import io.RenameAnnotations;
import loci.formats.FormatException;
import maskgen.MaskFromAnnotations;
import maskgen.MaskJoin;
import util.FileOper;

/**
 * 
 * This is the workflow for organelle project. The goal is to load a set of annotations with rectangles
 * defining ROIs. These ROIs are thresholded, labeled and tracked
 * 
 * @author pnb
 *
 */
public class organelleWF {

	/**
	 * This method extract all circle centers as they indicate the seeds for region growing algorithm
	 * to detect organelles/vesicles
	 * 
	 * @param JSONfileName
	 * @param outFileName
	 * @throws IOException
	 */
	public static void extractCircleCenters(String JSONfileName, String outFileName) throws IOException {
		
		AnnotationLoader annotClass = new AnnotationLoader();
		ArrayList<Annotation> annotations =  annotClass.readJSONfromWIPP(JSONfileName);
		System.out.println("before clean up");
		AnnotationLoader.printArrayListAnnot(annotations);
		// clean up annotations
		boolean ret =  AnnotationLoader.cleanupLabels(annotations);
		System.out.println("after clean up");
		AnnotationLoader.printArrayListAnnot(annotations);

		// construct the output file name
		//String name = (new File(JSONfileName)).getName();
		//name = name.substring(0, name.length()-5) + ".tif";

		AnnotationLoader.saveCircleCenters( annotations, outFileName);
		return;
			
	}
	
	/**
	 * This method is for batch processing of JSON files with annotations
	 * and extracting circle centers from them into TXT files
	 * 
	 * @param inputJSONFileFolder - input WIPP annotations in JSON file format
	 * @param outFileFolder - output file folder
	 * @return boolean whether the operation was succesful 
	 * 
	 * @throws IOException
	 */
			
	public static boolean extractCircleCenter_batch(String inputJSONFileFolder, String outFileFolder) throws IOException {
			// sanity check
			if (inputJSONFileFolder == null || outFileFolder == null) {
				System.err.println("Error: null inputFileFolder or outFileFolder ");
				return false;
			}
			/*Check directory if exist*/
			File directory=new File(inputJSONFileFolder);
			if(!directory.exists()){
				System.out.println("Input JSON Directory does not exist: " + inputJSONFileFolder);
				return false;
			}


			// create output folder
			directory=new File(outFileFolder);
			if(!directory.exists()){
				if(directory.mkdir()){
					System.out.println("output Directory was created: " + outFileFolder);
				}else{
					System.err.println("failed to create output Directory: " + outFileFolder);
					return false;
				}
			}

			///////////////////////////////////////////////////////////
			// getting JSON files to process
			Collection<String> dirJSONFiles = FileOper.readFileDirectory(inputJSONFileFolder);

			// select JSON files with the right suffix
			String suffixJSON = new String(".json");
			Collection<String> dirSelectJSONFiles = FileOper.selectFileType(dirJSONFiles,suffixJSON );	
			
			// sort stacks to process
			Collection<String> sortedJSONInFolder = FileOper.sort(dirSelectJSONFiles,
					FileOper.SORT_ASCENDING);
					
			//////////////////////////////////////////////////////
			
			String JSONfileName = new String();	
			String temp = null;
			
			for (Iterator<String> k = sortedJSONInFolder.iterator(); k.hasNext();) {
				JSONfileName = k.next();
				String nameJSON = (new File(JSONfileName)).getName();
				
				temp = nameJSON.substring(0, nameJSON.length() - suffixJSON.length() );
				String outFileName = new String(outFileFolder);
				outFileName += File.separator + temp + ".txt";
				organelleWF.extractCircleCenters(JSONfileName, outFileName);
			}
		
			return true;
	}
	public void cropRectangles(String inputRectCSVFileFolder, String inputImageFileFolder, String outFileFolder) throws Exception {
		
		// sanity check
		if (inputRectCSVFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null inputRectCSVFileFolder or outFileFolder ");
			return;
		}
		if(inputImageFileFolder == null){
			System.err.println("Error: null inputImageFileFolder  ");
			return;			
		}
		//////////////////////////////////////////////////////////////////////
		// getting CSV files to process
		Collection<String> dirCSVFiles = FileOper.readFileDirectory(inputRectCSVFileFolder);
		if(dirCSVFiles.isEmpty()){
			System.err.println("Error: inputRectCSVFileFolder is empty "  + inputRectCSVFileFolder);
			return;				
		}
		// select images with the right suffix
		String suffixCSVFrom = new String(".csv");
		Collection<String> dirCSVSelectFiles = FileOper.selectFileType(dirCSVFiles,suffixCSVFrom );	
		
		// sort stacks to process
		Collection<String> sortedRectCSVInFolder = FileOper.sort(dirCSVSelectFiles,
				FileOper.SORT_ASCENDING);
	

		////////////////////////////////////////////////////////////////
		// find the enclosing bounding box over all boxes 
		String CSVfileName = new String();	
		String delim = new String(",");
		//String outFileName = new String();
		
		double maxWidth = 0.0;
		double maxHeight = 0.0;
		for (Iterator<String> k = sortedRectCSVInFolder.iterator(); k.hasNext();) {
			CSVfileName = k.next();
			Roi rectInfo = this.readRectangle(CSVfileName, delim);
			
			// find max dimensions
			if(maxWidth < rectInfo.getFloatWidth()){
				maxWidth = rectInfo.getFloatWidth();
			}
			if(maxHeight < rectInfo.getFloatHeight()){
				maxHeight = rectInfo.getFloatHeight();
			}						
		}
		System.out.println("maxWidth = " + maxWidth + ", maxHeight = " + maxHeight);
		/////////////////////////////////////////////////////////////////
		// adjust the rectangular regions to max dimensions and crop the raw images
		
		// getting raw images to process
		Collection<String> dirFiles = FileOper.readFileDirectory(inputImageFileFolder);
		if(dirFiles.isEmpty()){
			System.err.println("Error: inputImageFileFolder is empty "  + inputImageFileFolder);
			return;				
		}
		// select images with the right suffix
		String suffixFrom = new String(".tif");
		Collection<String> dirSelectFiles = FileOper.selectFileType(dirFiles,suffixFrom );	
		
		// sort stacks to process
		Collection<String> sortedImageInFolder = FileOper.sort(dirSelectFiles,	FileOper.SORT_ASCENDING);
		//////////////////////////////////////////////////////////////////////////
		// these values should be used to check the adjust bbox to be inside of images
		int maxWidthFOV = 1224;
		int maxHeightFOV = 904;
		int offset = 10;
		int index = 1;
		// adjust the rectangular regions 
		ArrayList<Roi> rectList = new ArrayList<Roi>();
		ArrayList<Integer> rectIdxList = new ArrayList<Integer>();
		
		for (Iterator<String> k = sortedRectCSVInFolder.iterator(); k.hasNext();) {
			CSVfileName = k.next(); // Rect_annotations-frame200.csv
			Roi rectInfo = this.readRectangle(CSVfileName, delim);
			Roi rectAdjust = new Roi(rectInfo.getXBase()-offset, rectInfo.getYBase()-offset, maxWidth+2*offset, maxHeight+2*offset);
			
			rectList.add(rectAdjust);	
			
			String nameCSV = (new File(CSVfileName)).getName();
			int beginIndex = nameCSV.indexOf("frame");
			int endIndex = nameCSV.indexOf(".csv");
			String temp = (String) nameCSV.subSequence(beginIndex+5, endIndex);
			index = Integer.valueOf(temp);
			
			rectIdxList.add(index);
			
		}
		System.out.println("INFO: finished processing CSV files with rect info");
		for(int idx = 0; idx < rectIdxList.size(); idx++){
			System.out.print("rectIdxList["+idx+"]="+rectIdxList.get(idx) + " , " );
		}
		System.out.println();
		
		// initialize variables
		String rawFileName = new String();	
		index = 0;
		Roi curROI = rectList.get(index);
		int annotIdx = 0;

		for (Iterator<String> k = sortedImageInFolder.iterator(); k.hasNext();) {
			rawFileName = k.next(); // A3_02_c1_p1Z00_BrightField_t001_maxXY.tif
			String nameRaw = (new File(rawFileName)).getName();
			nameRaw = nameRaw.substring(nameRaw.length()-13, nameRaw.length()-10); // remove A3_02_c1_p1Z00_BrightField_t and _maxXY.tif
			int indexRaw = Integer.valueOf(nameRaw);
			
			System.out.println("INFO: processing raw = " + indexRaw + " using CSV = " + index);
			
			ImagePlus rawImage = IJ.openImage(rawFileName);
			ImageProcessor ip_crop = rawImage.getProcessor();

			// decide on the crop area
			if(index < rectIdxList.size()){			
				annotIdx = rectIdxList.get(index);
				if(annotIdx < indexRaw){
					curROI = rectList.get(index);
					System.out.println("TEST: using the ROI index = " + index);
					System.out.println("TEST:  ROI [" + annotIdx + "] = " + curROI.toString());
					index++;
				}
			}
			
			ip_crop.setRoi(curROI);
			ImageProcessor croppedRaw = ip_crop.crop();
			ImagePlus rawCropped = new ImagePlus("Raw Cropped", croppedRaw);
		
			FileSaver fs = new FileSaver(rawCropped);
			nameRaw = (new File(rawFileName)).getName();			
			String OutputName = new String(outFileFolder + nameRaw );					
			fs.saveAsTiff(OutputName);
	
		}

	}

	public Roi readRectangle(String _CSVFile, String delim) throws Exception {
		// System.out.println("Start readTable");

		FileReader fr = new FileReader(_CSVFile);
		BufferedReader reader = new BufferedReader(fr);
		String line = null;

		// skip the header
		line = reader.readLine();

		Roi result = null;
		float LeftOriginX, LeftOriginY, Width, Height;
		// read the rectagle line
		line = reader.readLine();

		String[] tokens = null;
		if (delim.equals("\\|")) {
			tokens = lineToArray(line);
		} else {
			if (delim.equals("\",\"") && line.charAt(0) == '"' && line.charAt(line.length() - 1) == '"') {
				line = line.substring(1, line.length() - 1);
				tokens = line.split(delim);
			} else {
				tokens = line.split(delim);
			}
		}
		LeftOriginX = Float.valueOf(tokens[0]);
		LeftOriginY = Float.valueOf(tokens[1]);
		Width = Float.valueOf(tokens[2]);
		Height = Float.valueOf(tokens[3]);
		result = new Roi(LeftOriginX, LeftOriginY, Width, Height);

		reader.close();
		return result;
	}
	/**
	 * Takes in a String and parses it into a String[] while taking care of
	 * anomalies.
	 * 
	 * 
	 * TODO: Be able to handle a lot more exceptions
	 * 
	 * @param line
	 * @return
	 */
	public String[] lineToArray(String line) {
		String[] tempArray1 = null;

		if (line == null)
			return tempArray1;

		/*
		This portion takes care of consecutive '|'s at the end of a line.
		Consecutive '|'s are read as empty white space and not considered to be 
		values.  
		*/
		int a = 1;

		if (line.charAt(line.length() - a) == '|') {
			while (line.length() - (a + 1) >= 0
					&& line.charAt(line.length() - (a + 1)) == '|')
				a++;
			String[] tempArray2 = line.split("\\|");
			if (line.length() - (a + 1) == -1)
				tempArray1 = new String[a + 1];
			else
				tempArray1 = new String[tempArray2.length + a];
			for (int i = 0; i < tempArray2.length; i++)
				tempArray1[i] = tempArray2[i];
			// for(int i=0; i<tempArray1.length; i++)
			// if(tempArray1[i]==null)
			// tempArray1[i] = "";
		} else {
			tempArray1 = line.split("\\|");
		}
		return tempArray1;
	}

	/**
	 * This method is the workflow for converting one JSON annotation per FOV into one or multiple mask images
	 * 
	 * @param inputJSONFileFolder
	 * @param inputStitchingFileFolder
	 * @param inputRawFileFolder
	 * @param outFileFolder
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean annotationToMasks(String inputJSONFileFolder, String inputStitchingFileFolder, String inputRawFileFolder, String outFileFolder) throws IOException, FormatException{

		//sanity checks
		if(inputJSONFileFolder == null || inputStitchingFileFolder==null ) {
			System.err.println("ERROR: input directories for JSON annotations or  TXT Stitching vectors are null");
			return false;
		}
		// assumption: annotation JSON files have an index that matches the input raw file name index

		// create output folder
		File directory=new File(outFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output Directory was created: " + outFileFolder);
			}else{
				System.err.println("failed to create output Directory: " + outFileFolder);
				return false;
			}
		}

		System.out.println("Step 1: rename JSON files according to the Stitching TXT files");
		String renamedJSONFileFolder = new String(outFileFolder + File.separator + "renamedJSON"  + File.separator);
		directory=new File(renamedJSONFileFolder); if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output renamedJSONFileFolder Directory was created: " +
						renamedJSONFileFolder); }else{ System.err.
							println("failed to create output renamedJSONFileFolder Directory: " +
									renamedJSONFileFolder); return false; }

		}

		if(!RenameAnnotations.CMD_renameAnnotJSONbasedOnStitching(inputJSONFileFolder, inputStitchingFileFolder, renamedJSONFileFolder)) {
			System.err.println("ERROR: failed renaming "+ inputJSONFileFolder + " based on "+ inputStitchingFileFolder + " into " + renamedJSONFileFolder);
			return false;
		}
		
		// step 2
		// find matching JSON and raw intensity files and create grayscale masks
		// while following the look-up table for assignment of gray values to concrete classes
		System.out.println("Step 2: create grayscale masks");
		//String inputJSONFileFolder = renamedJSONFileFolder;
		// we are using color of Ronit's annotation - there is not textual label !!!
		int uniqueType = 2; //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; //Integer.parseInt(args[1]);
		// we are splitting masks based on color !!!
		boolean combineAllUnique = false;//true;//Boolean.parseBoolean(args[2]);
		//String inputRawFileFolder = args[3];
		System.out.println("renamedJSONFileFolder="+renamedJSONFileFolder);
		System.out.println("uniqueType=" + uniqueType);
		System.out.println("combineAllUnique=" + combineAllUnique); 
		System.out.println("inputRawFileFolder=" + inputRawFileFolder);
		System.out.println("outFileFolder="+outFileFolder);


		String annotMaskOutput = new String(outFileFolder + File.separator + "annotMasks"); 
		directory=new File(annotMaskOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output annotMask Directory was created: " + annotMaskOutput);
		}		
		
		String metadataOutput = new String(outFileFolder + File.separator + "metadata_files"); 
		directory=new File(metadataOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output annotMask Directory was created: " + metadataOutput);
		}
		MaskFromAnnotations myClass = new MaskFromAnnotations();

		boolean isMappingFixed = false;

		// based on uniqueType, we are using labels
		// this is for concrete !!!!
		//ConcreteMaskColorMap concreteColor = new ConcreteMaskColorMap();		
		//myClass.setColor2grayMapping(concreteColor.getColor2grayMapping());

		//ConcreteMaskLabelMap concreteLabel = new ConcreteMaskLabelMap(2);		
		//myClass.setLabel2grayMapping(concreteLabel.getLabel2grayMapping());		

		// this is for organele
		OrganelleMaskColorMap organelleColor = new OrganelleMaskColorMap();		
		myClass.setColor2grayMapping(organelleColor.getColor2grayMapping());

		OrganelleMaskLabelMap organelleLabel = new OrganelleMaskLabelMap(2);		
		myClass.setLabel2grayMapping(organelleLabel.getLabel2grayMapping());
		// for the case of combineAllUnique = false when multiple masks are generated from each annotated image
		// this flag decides whether the individual masks should have the global mask value
		// or should become binary with the value of 255	
		isMappingFixed = false;//true;

		boolean ret = myClass.CMDlaunch(renamedJSONFileFolder, uniqueType, combineAllUnique, isMappingFixed, inputRawFileFolder, annotMaskOutput, metadataOutput);
		if(!ret){
			System.err.println("failed to create mask images in " + annotMaskOutput);
			return false;			
		}

		/*
		// step 3
		// join the annotation-based mask with the damage mask
		System.out.println("Step 3: join the annotation-based mask with the damage mask");
		//annotMaskOutput --> String AnnotMaskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\annotMask\\");
		//String modelMaskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\darkThreshVolcanics\\");	
		//String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\output\\");
		String joinMaskOutput = new String(outFileFolder + "\\processedAnnotGrayMask"); 
		directory=new File(joinMaskOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output processedAnnotModel Directory was created: " + joinMaskOutput);
		}		
		
		MaskJoin myClassJoin = new MaskJoin();
		ret = ret & myClassJoin.MaskBooleanJoin(annotMaskOutput, modelMaskFileFolder, joinMaskOutput);
		if(!ret){
			System.err.println("failed to join mask images with model damage images in " + joinMaskOutput);
			return false;			
		}		
		*/
		/*
		// step 4	
		// convert gray scale masks to color masks
		String colorMaskFileFolder = new String(outFileFolder + "\\processedAnnotColorMask"); 
		directory=new File(colorMaskFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output colorMaskFileFolder Directory was created: " + colorMaskFileFolder);
			}else{
				System.err.println("failed to create output colorMaskFileFolder Directory: " + colorMaskFileFolder);
				return false;
			}
		
		}	
		if( !convertMasksAndStats(joinMaskOutput,colorMaskFileFolder ) ){
			System.err.println("failed to convert masks to color in colorMaskFileFolder Directory: " + colorMaskFileFolder);
			return false;			
		}
		System.out.println("converted masks to color in colorMaskFileFolder Directory: " + colorMaskFileFolder);
*/
		
		// step 5
		// create tiles from all processed annotation + model masks
/*		System.out.println("Step 4: create tiles from all processed annotation + model masks");
		MaskToTiles myClassTiles = new MaskToTiles();
		myClassTiles.setTileHeight(256);
		myClassTiles.setTileWidth(256);
		// inputRawFileFolder --> String rawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\rawFOV\\");
		// joinMaskOutput --> String maskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedAnnotModel\\");
		// tileMaskOutput --> String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedTiles\\");	
	
		String tileMaskOutput = new String(outFileFolder + "\\processedTiles"); 
		directory=new File(tileMaskOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output tileMaskOutput Directory was created: " + tileMaskOutput);
		}			
		ret = ret &  myClassTiles.batchCreateTiles(inputRawFileFolder, joinMaskOutput, tileMaskOutput);
		if(!ret){
			System.err.println("failed to create tile images in " + tileMaskOutput);
			return false;			
		}		*/
		
		return ret;
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	
		organelleWF myClass = new organelleWF();
		/*
		// X --> \\itlnas\irb-projects\Proj-006-Tracking-Ronit\180611_170819_A3 60X OIL PL APO
		String inputRectCSVFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-10-14\\extractedRect");
		String inputImageFileFolder = new String("X:\\maxXYProjected");
		String outFileFolder = new String("X:\\Annotations2018-10-14\\cropped\\");
		
		myClass.cropRectangles(inputRectCSVFileFolder,  inputImageFileFolder,  outFileFolder);
		*/
		
		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\annotations");
		String inputStitchFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\stitching");
		String inputRawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\images8BPP");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks");
		
		myClass.annotationToMasks(inputJSONFileFolder,inputStitchFileFolder, inputRawFileFolder,  outFileFolder) ;

		// this assumes that the previous step renamed the original JSON files to match the tiff file names
		String inputJSONRenamedFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\renamedJSON");
		
		String outCircleCenterFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\circleCenter");

		organelleWF.extractCircleCenter_batch(inputJSONRenamedFileFolder, outCircleCenterFileFolder );

	}

}
