package util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;


/**
Disclaimer:  IMPORTANT:  This software was developed at the National Institute of Standards and Technology by employees of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the United States Code this software is not subject to copyright protection and is in the public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties, and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic. We would appreciate acknowledgement if the software is used. This software can be redistributed and/or modified freely provided that any derivative works bear some notice that they are derived from it, and any modified versions bear some notice that they have been modified.
*/


/**
 * This class is for the organelle project the input is a folder with sparsely
 * annotated mask images delineating cell regions Option 1: copy the first mask
 * file in a sequence till the index of the second annotated mask then start
 * copying the second annotated mask till the index of the next annotated mask
 * 
 * Option 2: move the cell region mask location based on its centroid location
 * interpolated from the two consecutive indices of annotated masks
 * 
 * Option 3: the same as Option 2 but use the principal axes to place the cell
 * region between two consecutive indices of annotated masks
 * 
 * @author pnb
 *
 */

public class maskInterpolation {

	public static boolean copyFirstMask(String inputDir, String prefixIndexString, String postfixIndexString, int maxTimeFrame, String outputDir) throws IOException {

		Collection<String> dirfiles = FileOper.readFileDirectory(inputDir);

		System.out.println("Directory Collection Size=" + dirfiles.size());
		// FileOper.printCollection(dirfiles);
		System.out.println();
		System.out.println();
		// select TIFF files with the right suffix
		String suffixTIFF = new String(".tif");
		Collection<String> onlyimages = FileOper.selectFileType(dirfiles, suffixTIFF);

		if (onlyimages.size() == 0) {
			System.err.println(" Directory List Collection size is zero for TIF images");
			return false;
		}

		// sort images to process since the renaming and folder placement depend on the
		// time stamp
		Collection<String> sortedRawInFolder = FileOper.sort(onlyimages, FileOper.SORT_ASCENDING);

		System.out.println("filtered and sorted Collection Size=" + onlyimages.size());
		FileOper.printCollection(sortedRawInFolder);

		// create the output folder if it does not exist
		String maskOutput = new String(outputDir + "interpolate");
		File directory = new File(maskOutput);
		if (!directory.exists()) {
			directory.mkdir();
			System.out.println("output annotMask Directory was created: " + maskOutput);
		}

		////////////////////////////////////////////////////////////////////
		// find the index of each file
		// example name: Light_Yellow_B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif

		// int numTimePoints = sortedRawInFolder.size();
		int timePadding = String.valueOf(maxTimeFrame).length();
		System.out.println("maxTimeFrame=" + maxTimeFrame + ", timePadding = " + timePadding);

		File firstFile = null;
		File secondFile = null;
		File annotatedFile = null;
		int count = 0;
		int lastDot = 0;
		int idxTime1 = -1;
		int idxTime2 = -1;

		String tempString = new String();
		
		for (Iterator<String> k = sortedRawInFolder.iterator(); k.hasNext();) {
			String fileName = k.next();
			annotatedFile = new File(fileName);
			//lastDot = annotatedFile.getName().lastIndexOf(".");
			String onlyFileName = annotatedFile.getName();//.substring(0, lastDot);
			count++;

			if (idxTime1 == -1) {
				idxTime1 = maskInterpolation.getFilenameIndex(onlyFileName, prefixIndexString, postfixIndexString);
				if (idxTime1 == -1) {
					System.err.println("ERROR: the mask file name " + onlyFileName
							+ " does not contain before index " + prefixIndexString + " and after index" + postfixIndexString);
					continue;
				} else {
					System.out.println("INFO: found the first mask file nam index " + idxTime1);
					firstFile = annotatedFile;
					
					String outFilename1 = new String(maskOutput + File.separator  + onlyFileName );
					System.out.println("INFO: copying into new file name " + outFilename1);
					FileUtils.copyFile(firstFile, new File(outFilename1));
					continue;
				}

			}
			if (idxTime2 == -1) {
				idxTime2 = maskInterpolation.getFilenameIndex(onlyFileName, prefixIndexString, postfixIndexString);
				if (idxTime2 == -1) {
					System.err.println("ERROR: the mask file name " + onlyFileName
							+ " does not contain before index " + prefixIndexString + " and after index" + postfixIndexString);
					continue;
				}
				secondFile = annotatedFile;
				System.out.println("INFO: found the second mask file nam index " + idxTime2);
				String outFilename1 = new String(maskOutput + File.separator  + onlyFileName );
				System.out.println("INFO: copying into new file name " + outFilename1);
				FileUtils.copyFile(firstFile, new File(outFilename1));
			}

			// File file = new File(fileName);

			for (int newIdx = idxTime1+1; newIdx < idxTime2; newIdx++) {
				tempString = new String(firstFile.getName());//.substring(0, idxTime1));
				// take care of padding
				// 1. identify the length of the original index string to define timePadding
				int pre = tempString.lastIndexOf(prefixIndexString);
				int post = tempString.lastIndexOf(postfixIndexString);
				String indexStr = fileName.substring(pre+prefixIndexString.length(), post);
				timePadding = indexStr.length();
				// 2. create a string with the number and prepend zeros to meet the timePadding
				// length requirements
				String idxTimeString = new String(Integer.toString(newIdx));
				int len = String.valueOf(newIdx).length();
				if (len < timePadding) {
					for (int i = 0; i < timePadding - len; i++) {
						idxTimeString = "0" + idxTimeString;
					}
				}
				
				String outFilename = new String(maskOutput);
			    outFilename	+= File.separator + firstFile.getName().substring(0, pre+prefixIndexString.length());
				
				outFilename += idxTimeString;// newIdx;
				outFilename += postfixIndexString;//firstFile.getName().substring(lastDot, (int) firstFile.length());

				System.out.println("INFO: copying into new file name " + outFilename);
				FileUtils.copyFile(firstFile, new File(outFilename));
			}
			if (count < sortedRawInFolder.size() - 1) {
				idxTime1 = idxTime2;
				firstFile = secondFile;
				idxTime2 = -1;
				secondFile = null;
			} else {
				// the last frame
				idxTime1 = idxTime2;
				firstFile = secondFile;
				idxTime2 = maxTimeFrame;
				secondFile = null;
			}
			/////////////////////////////////////////////////
			// take care of padding the indices for time

		}

		// copy the final mask to the mask
		// System.arraycopy(imgMaskFinal.getData(), 0, imgMaskFinal.getData(), 0,
		// (int)imgMaskFinal.getSize());

		return true;

	}

	/**
	 * This method is extracting the index from the file name
	 * 
	 * @param StitchingfileName - input path + file name that contains the index,
	 *                          for example, vector10.txt or annotation123.json
	 * @param root              - input root string of the file name
	 * @return
	 */
	public static int getFilenameIndex(String StitchingfileName, String prefix, String postfix) {
		// sanity check
		if (StitchingfileName == null || prefix == null || postfix == null) {
			System.err.println("missing inputs for  getFilenameIndex");
			return -1;
		}
		File name = new File(StitchingfileName);
		String fileName = name.getName();
		int pre = fileName.lastIndexOf(prefix);
		int post = fileName.lastIndexOf(postfix);
		if(pre < 0 ) {
			System.err.println("ERROR: could not find prefix "+prefix);
			return -1;			
		}
		if(post < 0) {
			System.err.println("ERROR: could not find postfix "+postfix);
			return -1;			
		}		// this update is to make file name matching more adaptive to the fact that the
		// file name root can be in the middle of the string
		//int rootLastIndex = fileName.lastIndexOf(root);
		String indexStr = fileName.substring(pre+prefix.length(), post);

		// String indexStr2 = fileName.substring(root.length(), dot);
		// System.out.println("index string = " + indexStr);
		int res = -1;
		try {
			res = Integer.valueOf(indexStr);
		} catch (NumberFormatException e) {
			return res;
		}
		// System.out.println("index = " + res);
		return res;
	}

	public static void main(String[] args) throws IOException {
		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}

		/*
		int maxTimeFrame = 1000;

		//String inputDir = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\masks\\annotMasks\\");
		//String outDir = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\2400-Projections_Reconstructed-20200206T143154Z\\masks\\roiMasks\\");
		String inputDir = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\80-Projections_Reconstructed-20200212T124205Z\\masks\\annotMasks\\");
		String outDir = new String("C:\\PeterB\\Projects\\Infer\\data2020-02-07\\80-Projections_Reconstructed-20200212T124205Z\\masks\\roiMasks\\");
		
		//polySamples_00880_00500.tif
		String prefixIndexString = new String("polySamples_0");
		String postfixIndexString = new String("_00500.tif");

		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
		*/
		
/*
		// dataset 190414_212412_FYVE session annotations-frame1
		//C:\PeterB\Projects\TestData\Ronit_zstacks\GeorgiaTech_2019-10-08\masks\annotMasks\color3	
		// the last frame index (+ 1) to be generated
		int maxTimeFrame = 583;//294;

		String inputDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\annotMasks\\color2\\");
		String outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\cellRegionMask\\");
		//rgb_128_255_128__A2_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
		// A2_02_c1_p1Z0_BrightField_t517_maxXY.ome.tif
		String prefixIndexString = new String("A2_02_c1_p1Z0_BrightField_t");
		String postfixIndexString = new String("_maxXY.ome.tif");

		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);

		//  Light_Yellow_A2_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
		inputDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\annotMasks\\color1\\");
		outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\focusedROIMask\\");
		//prefixIndexString = new String("Light_Yellow_A2_02_c1_p1Z0_BrightField_t");
		prefixIndexString = new String("A2_02_c1_p1Z0_BrightField_t");
		
		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
	*/	
		
/*		
 		// dataset from GeorgiaTech_2019-10-08
 		int maxTimeFrame = 662;//28;
 		String inputDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\cellRegionMask\\");
		String outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\cellRegionMask\\");
		//rgb_128_255_128__B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
		String prefixIndexString = new String("rgb_128_255_128__B1_02_c1_p1Z0_BrightField_t");
		String postfixIndexString = new String("_maxXY.ome.tif");

		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
		
		inputDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\focusedROIMask\\");
		outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\focusedROIMask\\");
		prefixIndexString = new String("Light_Yellow_B1_02_c1_p1Z0_BrightField_t");
		
		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
		
		inputDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\vesicleMask\\");
		outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2019-10-08\\masks\\vesicleMask\\");
		prefixIndexString = new String("Red_B1_02_c1_p1Z0_BrightField_t");
		
		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
	*/
		
		// cell region mask : dataset from GeorgiaTech_2020-05-24
 		int maxTimeFrame;
 		maxTimeFrame = 807;
 		System.out.println("maxTimeFrame=" + maxTimeFrame);
 		
// 		String inputDir = new String(
//				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2020-05-24\\masks\\annotMasks\\color2\\");
//		String outDir = new String(
//				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\GeorgiaTech_2020-05-24\\masks\\annotMasks\\color2\\");
//		//rgb_128_255_128__B1_02_c1_p1Z0_BrightField_t001_maxXY.ome.tif
		//C:\PeterB\Projects\TestData\Ronit_zstacks\GeorgiaTech_2020-05-24\masks\annotMasks\color2
	
		String inputDir = new String("C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\2021-05-03\\masks\\annotMasks\\color0\\");
		String outDir = new String(
				"C:\\PeterB\\Projects\\TestData\\Ronit_zstacks\\2021-05-03\\masks\\annotMaskInterpolate\\");
	
		// A3_02_c1_p1Z0_BrightField_t002_maxXY.ome.tif
		String prefixIndexString = new String("A3_02_c1_p1Z0_BrightField_t");
		String postfixIndexString = new String("_maxXY.ome.tif");

		maskInterpolation.copyFirstMask(inputDir, prefixIndexString, postfixIndexString, maxTimeFrame, outDir);
		
		
	}

}
