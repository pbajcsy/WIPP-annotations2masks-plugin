package workflows;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import datatype.ConcreteMaskColorMap;
import datatype.ConcreteMaskLabelMap;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import io.CSV_IOsupport;
import io.StitchingLoader;
import loci.formats.FormatException;
import maskgen.MaskFromAnnotations;
import maskgen.MaskFromColorImage;
import maskgen.MaskJoin;
import maskgen.MaskOper;
import maskgen.MaskToTiles;
import util.FileOper;

/**
 * 
 * This class is designed to combine all steps in the pre-processing
 * of concrete annotations and raw intensity fields of view (FOVs)
 * 
 * @author pnb
 *
 */
public class concreteWF {

	/**
	 * This method is for converting the mosaic-based annotation from Steve to a grayscale mask image
	 * 
	 * 
	 * @param inputJSONFileFolder
	 * @param inputStitchingFileFolder
	 * @param inputRawFileFolder
	 * @param modelMaskFileFolder
	 * @param outFileFolder
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean stitchedImageToMasks(String inputJSONFileFolder, String inputStitchingFileFolder, String inputRawFileFolder, String modelMaskFileFolder, String outFileFolder) throws IOException, FormatException
	{
		
		MaskFromAnnotations myClass = new MaskFromAnnotations();
		// Steve's annotations over the mosaicked image
		int uniqueType = MaskFromAnnotations.UNIQUE_TYPE_COLOR;
		boolean combineAllUnique = true;
		
		System.out.println("inputJSONFileFolder="+inputJSONFileFolder);
		System.out.println("uniqueType=" +  uniqueType);
		System.out.println("combineAllUnique=" + combineAllUnique); 
		System.out.println("inputRawFileFolder=" + inputRawFileFolder);
		System.out.println("outFileFolder="+outFileFolder);
		
		boolean isMappingFixed = false;
		ConcreteMaskColorMap concreteColor = new ConcreteMaskColorMap();		
		myClass.setColor2grayMapping(concreteColor.getColor2grayMapping());

		ConcreteMaskLabelMap concreteLabel = new ConcreteMaskLabelMap(2);		
		myClass.setLabel2grayMapping(concreteLabel.getLabel2grayMapping());
		
		isMappingFixed = true;
		boolean ret = true;
		ret = myClass.CMDlaunch(inputJSONFileFolder, uniqueType, combineAllUnique, isMappingFixed, inputRawFileFolder, outFileFolder );
		
		// the tiling is hard coded based on the FOV provided by Steve 
		String rawFileFolder = new String(inputRawFileFolder);
		String maskFileFolder = new String(outFileFolder);// null;//new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedAnnotModel\\");
		String outFileFolderTiles = new String(outFileFolder);	
		

		MaskToTiles myClassTiles = new MaskToTiles();
		myClassTiles.setTileHeight(712);
		myClassTiles.setTileWidth(950);
		myClassTiles.setTileHeightOverlap(0);
		myClassTiles.setTileWidthOverlap(0);

		ret = myClassTiles.batchCreateTiles(rawFileFolder, maskFileFolder, outFileFolderTiles);
		
		
		return ret;
		
	}
	/**
	 * This method is the workflow for converting one JSON annotation per FOV into a mask image and boolean joining the mask with the underlying
	 * damage maks denoted as modelMaskFileFolder.
	 * 
	 * @param inputJSONFileFolder
	 * @param inputStitchingFileFolder
	 * @param inputRawFileFolder
	 * @param modelMaskFileFolder
	 * @param outFileFolder
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean annotationToTilesWF(String inputJSONFileFolder, String inputStitchingFileFolder, String inputRawFileFolder, String modelMaskFileFolder, String outFileFolder) throws IOException, FormatException{

		// step 1
		// rename JSON annotations according to stitching vectors to match the raw intensity file names
		System.out.println("Step 1: rename JSON annotations according to stitching vectors");
		/*		
		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018");
		String inputStitchingFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\StitchingVectors");	
		String renamedJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018renamed\\");
		*/
		File directory=new File(outFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output Directory was created: " + outFileFolder);
			}else{
				System.err.println("failed to create output Directory: " + outFileFolder);
				return false;
			}
		}
		String renamedJSONFileFolder = new String(outFileFolder + File.separator + "renamedJSON"); 
		directory=new File(renamedJSONFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output renamedJSONFileFolder Directory was created: " + renamedJSONFileFolder);
			}else{
				System.err.println("failed to create output renamedJSONFileFolder Directory: " + renamedJSONFileFolder);
				return false;
			}
		
		}		
		boolean ret = StitchingLoader.renameAnnotationFilenames(inputJSONFileFolder, inputStitchingFileFolder, renamedJSONFileFolder);
		// stop if failed renaming files
		if(!ret){
			System.err.println("failed to rename annotations in " + inputJSONFileFolder);
			return false;			
		}
		
		// step 2
		// find matching JSON and raw intensity files and create grayscale masks
		// while following the look-up table for assignment of gray values to concrete classes
		System.out.println("Step 2: create grayscale masks");
		//String inputJSONFileFolder = renamedJSONFileFolder;	
		int uniqueType = 1; //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; //Integer.parseInt(args[1]);
		boolean combineAllUnique = true;//Boolean.parseBoolean(args[2]);
		//String inputRawFileFolder = args[3];
		System.out.println("renamedJSONFileFolder="+renamedJSONFileFolder);
		System.out.println("uniqueType=" + uniqueType);
		System.out.println("combineAllUnique=" + combineAllUnique); 
		System.out.println("inputRawFileFolder=" + inputRawFileFolder);
		System.out.println("outFileFolder="+outFileFolder);
			

		String annotMaskOutput = new String(outFileFolder + File.separator + "drawnAnnotGrayMask"); 
		directory=new File(annotMaskOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output annotMask Directory was created: " + annotMaskOutput);
		}		
		MaskFromAnnotations myClass = new MaskFromAnnotations();
		
		boolean isMappingFixed = false;
		ConcreteMaskColorMap concreteColor = new ConcreteMaskColorMap();		
		myClass.setColor2grayMapping(concreteColor.getColor2grayMapping());

		// based on uniqueType, we are using labels
		ConcreteMaskLabelMap concreteLabel = new ConcreteMaskLabelMap(2);		
		myClass.setLabel2grayMapping(concreteLabel.getLabel2grayMapping());		
		isMappingFixed = true;
		
		ret = ret & myClass.CMDlaunch(renamedJSONFileFolder, uniqueType, combineAllUnique, isMappingFixed, inputRawFileFolder, annotMaskOutput );
		if(!ret){
			System.err.println("failed to create mask images in " + annotMaskOutput);
			return false;			
		}

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
	
	public static boolean convertMasksAndStats(String maskImageFolder, String outputPath) throws IOException{
		
		
		ConcreteMaskColorMap mapping = new ConcreteMaskColorMap();
		//HashMap<Color, Integer> convert_mapping = mapping.getColor2grayMapping();
		//boolean ret = MaskFromColorImage.convertMaskImage2ColorBatch(grayMaskImageFolder,mapping, outputPath);

		///////////////////////////////////////////////////////////////
		// getting annotated images to process
		Collection<String> dirMaskFiles = FileOper.readFileDirectory(maskImageFolder);

		// select images with the right suffix
		String suffixTIF = new String(".tif");
		Collection<String> dirSelectMaskFiles = FileOper.selectFileType(dirMaskFiles,suffixTIF );	
		
		// sort stacks to process
		Collection<String> sortedImagesInFolder = FileOper.sort(dirSelectMaskFiles,	FileOper.SORT_ASCENDING);
			
		
		String maskFileName = new String();
		boolean headerInclude = true;
		for (Iterator<String> k = sortedImagesInFolder.iterator(); k.hasNext();) {
			maskFileName = k.next();
			String nameMask = (new File(maskFileName)).getName();
			nameMask = nameMask.substring(0, nameMask.length()-4);
			System.out.println("INFO: name mask = " +  nameMask );
			
			System.out.println("INFO: loading file:"+ maskFileName);
			ImagePlus imgMask = IJ.openImage(maskFileName);
			ImagePlus imgColor = MaskFromColorImage.convertMaskImage2Color(imgMask, mapping);

			FileSaver fs = new FileSaver(imgColor);			
			if(!outputPath.endsWith(File.separator)){
				outputPath += File.separator;
			}
			// color-coded mask
			fs.saveAsPng(outputPath + nameMask + ".png");

			/////////////////////////////////////////////////
			// compute stats
			//////////////////////////////////////////////////
			// compute pixel counts/statistics
			System.out.println("report pixel statistics");		
			int [] grayCount_damage = MaskOper.histogram(imgMask, ConcreteMaskColorMap.NUM_OF_CLASSES);
			
			boolean withHeader = false;
			String pixelCount_damage = ConcreteMaskColorMap.reportPixelCountPerClass(grayCount_damage, withHeader);
		
			////////////////////////////////////////////
			// save statistics 
			String ret_damage = new String(nameMask + ", " + pixelCount_damage );
			
			String statsFile_damage = new String(outputPath + "pixelStats_damage.csv");
			if(headerInclude){
				headerInclude = false;
				withHeader = true;
				String header  = ConcreteMaskColorMap.reportPixelCountPerClass(null, withHeader);
				withHeader = false;
				header = "file name, " + header ;

				if(!CSV_IOsupport.SaveAppendArray(header, statsFile_damage)){
					System.err.println("ERROR in : failed to append the string =" + statsFile_damage);
					//return null;
				}		
			}		
			if(!CSV_IOsupport.SaveAppendArray(ret_damage, statsFile_damage)){
				System.err.println("ERROR in : failed to append the string =" + statsFile_damage);
				//return null;
			}
		}
		return true;
	
			
	}
	
	/**
	 * @param args
	 * @throws FormatException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, FormatException {
		
		// to go from JSON annotations to image masks
/*		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\JSON_orig");
		String inputStitchingFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\StitchingVectors");	
		//String renamedJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018renamed\\");
		String rawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\rawFOV");
		String modelMaskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\darkThreshVolcanics");	
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\June2019\\output");
*/		
		
		String inputJSONFileFolder = new String("Y:\\June2019\\JSON_orig");
		String inputStitchingFileFolder = new String("Y:\\June2019\\StitchingVectors");	
		String rawFileFolder = new String("Y:\\Core_M3_180day_AllScans\\wippModifiedCor");
		String modelMaskFileFolder = new String("Y:\\Core_M3_180day_AllScans\\darkThreshVolcanics");	
		String outFileFolder = new String("Y:\\June2019\\temp");
	
		concreteWF myClass = new concreteWF();
		boolean ret = myClass.annotationToTilesWF(inputJSONFileFolder,inputStitchingFileFolder, rawFileFolder, modelMaskFileFolder,outFileFolder);
		System.out.println("Completed with success flag = " + ret);

		//////////////////
		// to process the mosaic-based annotations
/*		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2017\\JSON_orig");
		String inputRawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2017\\raw");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2017\\output");	
		String inputStitchingFileFolder = null; // for now
		String modelMaskFileFolder = null;
		concreteWF myClass = new concreteWF();
		boolean ret = myClass.stitchedImageToMasks(inputJSONFileFolder, inputStitchingFileFolder, inputRawFileFolder, modelMaskFileFolder, outFileFolder);
		System.out.println("Completed with success flag = " + ret);*/
		
		/////////////////////////////
		// to convert AI-based masks and compute stats
		//String maskImageFolder = new String("Y:\\segnet-inference-20190510T133909-0");
		//String outputPath = new String("Y:\\segnet-inference-20190510T133909-0_PNG" + File.separator);

	/*	String maskImageFolder = new String("Y:\\wippAssist\\unet-inference-20190510T135213-0");
		String outputPath = new String("Y:\\wippAssist\\unet-inference-20190510T135213-0_PNG" + File.separator);
*/
		/*String maskImageFolder = new String("X:\\contextAssist\\segnet-inference");
		String outputPath = new String("X:\\contextAssist\\segnet-inference_PNG"+File.separator);
	

		concreteWF.convertMasksAndStats(maskImageFolder,outputPath );
*/
		
		
	}

}
