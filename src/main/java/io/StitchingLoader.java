/**
 * 
 */
package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import com.csvreader.CsvWriter;

import datatype.Annotation;
import loci.formats.FormatException;
import maskgen.MaskFromAnnotations;
import util.FileOper;

/**
 * @author pnb
 * @author Mohamed Ouladi <mohamed.ouladi at nist.gov>
 *
 */
public class StitchingLoader {

	/**
	 * This method opens a stitching vector file (vector1.txt) and returns the first name of a tile file name in the stitching vector file
	 * this method is used for associating the annotation JSON files with the raw tif images
	 *  
	 * @param StitchingfileName
	 * @return string with the first name of a tile file name
	 * 
	 * @throws IOException
	 */
	public static String readFilenamefromStitch(String StitchingfileName) throws IOException{
		FileReader fr = new FileReader(StitchingfileName);
		BufferedReader reader = new BufferedReader(fr);
		String line = null;

		String delim = new String(";");
		
		boolean found = false;
		String result = null;
		line = reader.readLine();
		while (!found && line != null && line.length() != 0) {
		
			//int numTokens = line.split(delim).length;
			String[] tokens = null;
			
			tokens = line.split(delim);
			found = false;
			for(int i=0; !found && i<tokens.length;i++){
				if(tokens[i].substring(0, 5).equalsIgnoreCase("file:")){
					found = true;
					result = new String(tokens[i].substring(6, tokens[i].length()));
				}
			}			
			line = reader.readLine();
		}
		reader.close();
		
		return result;		
	}
	
	/**
	 * This is a helper method for the 
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
	 * This method is extracting the index from the file name
	 * 
	 * @param StitchingfileName - input path + file name that contains the index, for example, vector10.txt or annotation123.json
	 * @param root - input root string of the file name
	 * @return
	 */
	public static int getFilenameIndex(String StitchingfileName, String root){
		// sanity check
		if(StitchingfileName== null || root == null ){
			System.err.println("missing inputs for  getFilenameIndex");
			return -1;		
		}
		File name = new File(StitchingfileName);
		String fileName = name.getName();
		int dot = fileName.lastIndexOf(".");
		
		//this update is to make file name matching more adaptive to the fact that the file name root can be in the middle of the string
		int rootLastIndex = fileName.lastIndexOf(root);
		// sanity check
		if( rootLastIndex+root.length() >= dot ) {
			System.err.println("StitchingfileName="+StitchingfileName+" is shorter than expected root="+root);
			return -1;				
		}
		String indexStr = fileName.substring(rootLastIndex+root.length(), dot);
		
		//String indexStr2 = fileName.substring(root.length(), dot);
		//System.out.println("index string = " + indexStr);
		int res = -1;
		try {
		  res = Integer.valueOf(indexStr);
		}catch(NumberFormatException e) {
			return res;
		}
		return res;		
	}
	/**
	 * This method is for renaming JSON annotation files according to the raw file name stored in a matching stitching vector
	 * the matching of JSON and stitching vector files is performed based on the index
	 * 
	 * @param inputJSONFileFolder
	 * @param inputStitchingFileFolder
	 * @param outFileFolder
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public static boolean renameAnnotationFilenames(String inputJSONFileFolder, String inputStitchingFileFolder, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (inputJSONFileFolder == null || inputStitchingFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null inputFileFolder, inputStitchingFileFolder or outFileFolder ");
			return false;
		}
		/*Check directory if exist*/
		File directory=new File(inputJSONFileFolder);
		if(!directory.exists()){
			System.out.println("Input JSON Directory does not exist: " + inputJSONFileFolder);
			return false;
		}
		directory=new File(inputStitchingFileFolder);
		if(!directory.exists()){
			System.out.println("Input Directory with stitching vectors does not exist: " + inputStitchingFileFolder);
			return false;
		}
		directory=new File(outFileFolder);
		if(!directory.exists()){
			System.out.println("output Directory does not exist: " + outFileFolder);
			return false;
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
		///////////////////////////////////////////////////////////////
		// getting images to process
		Collection<String> dirStitchFiles = FileOper.readFileDirectory(inputStitchingFileFolder);

		// select images with the right suffix
		String suffixTXT = new String(".txt");
		Collection<String> dirSelectStitchFiles = FileOper.selectFileType(dirStitchFiles,suffixTXT );	
		
		// sort stacks to process
		Collection<String> sortedStitchInFolder = FileOper.sort(dirSelectStitchFiles,
				FileOper.SORT_ASCENDING);
			
		//////////////////////////////////////////////////////
		//AnnotationLoader annotClass = new AnnotationLoader();
		
		String JSONfileName = new String();	
		String stitchFileName = new String();
		//String outFileName = new String();
		
		String rootJSON = new String("annotations-frame");
		String rootJSON2 = new String("test-");
		String rootTXT = new String("stitching-vector");
		String rootTXT2 = new String("-img-global-positions-");
		
			
		// store metadata about the execution
		//ArrayList<String> strSaveMapping = new ArrayList<String>();
		
		boolean success = true;
		boolean foundMatch = false;
		for (Iterator<String> k = sortedJSONInFolder.iterator(); k.hasNext();) {
			JSONfileName = k.next();
			String nameJSON = (new File(JSONfileName)).getName();
			int indexJSON = StitchingLoader.getFilenameIndex(nameJSON, rootJSON);//nameJSON.substring(0, nameJSON.length()-5);
			if(indexJSON == -1) {
				// if the stitching vector file name follows a different naming convention then try the rootTXT2
				indexJSON = StitchingLoader.getFilenameIndex(nameJSON, rootJSON2);
			}
			if(indexJSON == -1) {
				System.err.println("ERROR: the JSON file name " + nameJSON + " does not follow the convention: file name should start with "+ rootJSON + " or " + rootJSON2);
				continue;
			}
			// find matching stitchFileName
			foundMatch = false;
			for(Iterator<String> r = sortedStitchInFolder.iterator(); !foundMatch && r.hasNext(); ){
				stitchFileName = r.next();
				String nameTXT = (new File(stitchFileName)).getName();
				int indexTXT = StitchingLoader.getFilenameIndex(nameTXT, rootTXT2);
				if(indexTXT == -1) {
					// if the stitching vector file name follows a different naming convention then try the rootTXT2
					indexTXT = StitchingLoader.getFilenameIndex(nameTXT, rootTXT);
				}
				
				if(indexJSON == indexTXT){				
					foundMatch = true;
				}
			}
			if(!foundMatch){
				System.err.println("ERROR: could not find a matching stitching vector TXT file to the JSON file");
				continue;
			}
			
			//ArrayList<Annotation> annotations =  annotClass.readJSONfromWIPP(JSONfileName);
			//AnnotationLoader.printArrayListAnnot(annotations);

			//MaskFromAnnotations myClass = new MaskFromAnnotations();
			// retrieve the file name for the renamed annotation JSON file to construct the output file name
			
			
			
			String name = readFilenamefromStitch(stitchFileName);//(new File(JSONfileName)).getName();
			name = name.substring(0, name.length()-4) + ".json";
			System.out.println("INFO: matching pair: JSON = " +  JSONfileName + " TXT = " + stitchFileName + " IMAGE = " + name);
			
			
	        File file = new File(JSONfileName);           
	        String outFilenameJSON = new String(outFileFolder);
	        if(!outFileFolder.endsWith(File.separator)){
	        	outFilenameJSON += File.separator;
	        }
	        outFilenameJSON += name;
	        
	        // copying the JSON annotation file after renaming the file 
	        FileUtils.copyFile(file, new File(outFilenameJSON));

	        /*
	        file = new File(stitchFileName);           
	        String outFilenameTXT = new String(outFileFolder);
	        if(!outFileFolder.endsWith("\\") && !outFileFolder.endsWith("/")){
	        	outFilenameTXT += "\\";
	        }
	        outFilenameTXT += name;
	        
	        // copying the TXT stitching file that matches the  renamed JSON file 
	        FileUtils.copyFile(file, new File(outFilenameTXT));
	        */
	        
	        
	     // renaming the JSON annotation file and moving it to a new location 
/*	        if(file.renameTo(new File(outFilename))) { 
	            // if file copied successfully then delete the original file 
	            //file.delete(); 
	            System.out.println("File moved successfully " + name); 
	        } else { 
	            System.out.println("Failed to move the file = " + name); 
	            success = false;
	        } 		*/
		}
		
		return success;
	}
	
	/**
	 * This method creates a script for downloading stitching vectors from wipp 2.3
	 * since we do not have a zip file option
	 * @param lineText - array of text forming the call to the REST API of wipp 2.3
	 * @param begIndex - indices of stitching vector: first index
	 * @param endIndex - indices of stitching vector: highest index
	 * @param outFilename - output file name for the script
	 * @return boolean
	 */
	public static boolean createScriptForDownloadingStitchingVectors(String [] lineText, int begIndex, int endIndex, String outFilename) {
		//sanity check
		if(lineText == null || lineText.length < 3) {
			System.out.println("ERROR: missing input string array or array length < 3");
			return false;
		}
		if(begIndex < 0 || (begIndex > endIndex)) {
			System.out.println("ERROR: wrong indices: begIndex="+ begIndex + ", endIndex="+endIndex);
			return false;			
		}
		File filename = new File(outFilename);
		if(filename.exists()){
			System.out.println("output outFilename: " + outFilename + " already exists");
			return false;
		}		

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(
					new FileWriter(outFilename, true), ',');

			String result = new String();
			String indexText = null;
			for(int i = begIndex; i< endIndex + 1; i++) {
				if(i < 10 ) {
					indexText = new String("00"+i);
				}else {
					if(i < 100) {
						indexText = new String("0"+i);
					}else {
						indexText = new String(""+ i);
					}				
				}
				result = lineText[0] + indexText + lineText[1] + i + lineText[2] + "\n";
				System.out.println(result);

				csvOutput.write(result);
				csvOutput.endRecord();
			}

			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FormatException 
	 */
	public static void main(String[] args) throws IOException, FormatException {
		// sanity check
		if(args == null || args.length < 2){
			System.err.println("expected one argumant: Stitching vector file from WIPP ");
			return;			
		}

		String StitchingfileName = args[0];
		String rootName = args[1];
		System.out.println("StitchingfileName="+StitchingfileName);
		
/*		// create a script for downloading stitching vectors
 		String [] lineText = new String[3];
		String os_dependency = new String("/");
		// 190414_212412_FYVE session annotations-frame: C:/PeterB/Utilities/wget/wget.exe -O stitching-vector-5d9cd14f45704b00fd186419-img-global-positions-001.txt   http://129.6.58.6/api/stitchingVectors/5e6186c3adbe1d0075cb6597/timeSlices/1/globalPositions/
		// Ronit_190415_152618_IMM_Session_XYprojection_STITCH: stitching-vector-5e628b01adbe1d0075cb67e5-img-global-positions-001.txt		
		// Ronit_180611_162344_IMM_Session_STITCH: 5e62cec8adbe1d0075cb6ce3
		// Ronit_01_190310_041016_IMM_Session_01_XYprojection_6324_STITCH: 5e62dd72adbe1d0075cb6fad
		// Ronit_02_190310_041016_IMM_Session_02_XYprojection_3076_STITCH: 5e62dd7badbe1d0075cb6fae
		lineText[0] = new String(os_dependency + "c" + os_dependency + "PeterB"+ os_dependency + "Utilities"+ os_dependency + "wget" + os_dependency + "wget.exe -O stitching-vector-5e62dd7badbe1d0075cb6fae-img-global-positions-"); 
		lineText[1] = new String(".txt   http:"+ os_dependency + os_dependency + "129.6.58.6" + os_dependency + "api" + os_dependency + "stitchingVectors" + os_dependency + "5e62dd7badbe1d0075cb6fae"+os_dependency+"timeSlices"+os_dependency);
		lineText[2] = new String(os_dependency+"globalPositions" + os_dependency);
		String outputScriptfileName = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\stitching\\script.sh");
		int begIndex = 1;
		int endIndex = 302;//402;//865;//583;//294;	
		StitchingLoader.createScriptForDownloadingStitchingVectors(lineText, begIndex, endIndex, outputScriptfileName); 
*/		
		
/*		// create a script for downloading stitching vectors
		// NCNR project
 		String [] lineText = new String[3];
		String os_dependency = new String("/");
		// http://129.6.58.6/api/stitchingVectors/5e617714adbe1d0075cb6468/timeSlices/1/globalPositions		
		// C:/PeterB/Utilities/wget/wget.exe -O stitching-vector-5e617714adbe1d0075cb6468-img-global-positions-001.txt   http://129.6.58.6/api/stitchingVectors/5e617714adbe1d0075cb6468/timeSlices/1/globalPositions/
		lineText[0] = new String(os_dependency + "c" + os_dependency + "PeterB"+ os_dependency + "Utilities"+ os_dependency + "wget" + os_dependency + "wget.exe -O stitching-vector-5e617714adbe1d0075cb6468-img-global-positions-"); 
		lineText[1] = new String(".txt   http:"+ os_dependency + os_dependency + "129.6.58.6" + os_dependency + "api" + os_dependency + "stitchingVectors" + os_dependency + "5e617714adbe1d0075cb6468"+os_dependency+"timeSlices"+os_dependency);
		lineText[2] = new String(os_dependency+"globalPositions" + os_dependency);
		String outputScriptfileName = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\stitching\\script.sh");
		int begIndex = 1;
		int endIndex = 1124;	
		StitchingLoader.createScriptForDownloadingStitchingVectors(lineText, begIndex, endIndex, outputScriptfileName); 
*/
		//StitchingLoader myClass = new StitchingLoader();
		//String res = StitchingLoader.readFilenamefromStitch(StitchingfileName);
		//System.out.println("file name from stitching vector = " + res);
		//int index = StitchingLoader.getFilenameIndex(StitchingfileName, rootName);
		//System.out.println("file name index = " + index);

/*		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018");
		String inputStitchingFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\StitchingVectors");		
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018renamed\\");
*/

/*		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\annotations");
		String inputStitchingFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\stitching_1");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\stitching");
		
		StitchingLoader.renameAnnotationFilenames(inputJSONFileFolder, inputStitchingFileFolder, outFileFolder);
*/	
		
     //		ArrayList<Annotation> res = myClass.parseJSONfromWIPP(JSONfileName);
		
	/*	String outFileName = args[1];
		System.out.println("outFileName="+outFileName);
		//AnnotationLoader.saveCircleCenters(res, outFileName); 
		//String inputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations");
		//String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations\\testOutput");
		String inputFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-09-24\\version2");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\Annotations2018-09-24\\version2\\testOutput");
	*/	
		// ncnr dataset
		String inputJSONFileFolder = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\annotations");
		String inputStitchingFileFolder = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\stitching");
		String outFileFolder = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\masks\\renamedJSON");
		
		StitchingLoader.renameAnnotationFilenames(inputJSONFileFolder, inputStitchingFileFolder, outFileFolder);
		
	}

}
