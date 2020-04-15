package io;

import java.io.File;
import java.io.IOException;

import loci.formats.FormatException;

/**
 * This class is for renaming JSON annotations collected from WDZT to have the same name as the pyramid frame in a collection of input images
 * The renamed files will be stored in the outFileFolder
 * 
 * WARNING: the current code does not support the case when the stitching vector contains multiple names of tiles being stitched
 * TODO: to support this case
 * 
 * @author peter bajcsy
 *
 */
public class RenameAnnotations {

	/**
	 * This is a method for command line execution
	 * @param inputJSONFileFolder - input folder with JSON annotations
	 * @param inputStitchingFileFolder - input folder with TXT stitching vecotr files
	 * @param outFileFolder - output folder for renamed JSON annotation files
	 * @return boolean based on success
	 * @throws IOException
	 * @throws FormatException
	 */
	public static boolean CMD_renameAnnotJSONbasedOnStitching(String inputJSONFileFolder, String inputStitchingFileFolder, String outFileFolder) throws IOException, FormatException {
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

		System.out.println("rename JSON files according to the Stitching TXT files");
		String renamedJSONFileFolder = new String(outFileFolder);
		if(!outFileFolder.endsWith(File.separator)) {
			renamedJSONFileFolder += File.separator;
		}
		directory=new File(renamedJSONFileFolder); if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output renamedJSONFileFolder Directory was created: " +
						renamedJSONFileFolder); }else{ System.err.
							println("failed to create output renamedJSONFileFolder Directory: " +
									renamedJSONFileFolder); return false; }

		}
		boolean ret = StitchingLoader.renameAnnotationFilenames(inputJSONFileFolder, inputStitchingFileFolder, renamedJSONFileFolder);
		// stop if failed renaming files
		if(!ret){
			System.err.println("failed to rename annotations in " + inputJSONFileFolder);
			return false;			
		}
		return true;
	}

	/**
	 * This is the main for a command line execution
	 * @param args - String inputJSONFileFolder, String inputStitchingFileFolder, String outFileFolder
	 * @throws IOException
	 * @throws FormatException
	 */
	public static void main(String[] args) throws IOException, FormatException {
		if(args == null || args.length < 3) {
			System.err.println("ERROR: input arguments are String inputJSONFileFolder, String inputStitchingFileFolder, String outFileFolder");
			return;
		}
		System.out.println("argument length=" + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		String inputJSONFileFolder = new String(args[0]);
		String inputStitchingFileFolder = new String(args[1]);
		String outFileFolder = new String(args[2]);
		if(!RenameAnnotations.CMD_renameAnnotJSONbasedOnStitching(inputJSONFileFolder, inputStitchingFileFolder, outFileFolder)) {
			System.err.println("ERROR: failed renaming "+ inputJSONFileFolder + " based on "+ inputStitchingFileFolder + " into " + outFileFolder);
			return;
		}

	}

}
