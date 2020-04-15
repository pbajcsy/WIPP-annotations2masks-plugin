package io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.csvreader.CsvWriter;
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

public class CsvMyWriter {

	/**
	 * This method saves a 2D table
	 * 
	 * @param table
	 *            - input table as a double array of doubles
	 * @param OutFileName
	 *            - output file name
	 * @return - boolean true if the operation was succesful
	 * @throws IOException
	 */
	public static void SaveTable(String[] header, double[][] table,
			String outputFile) {

		// sanity check
		if (table == null) {
			System.err.println("ERROR: missing input table");
			return;
		}
		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(outputFile).exists();

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(
					new FileWriter(outputFile, true), ',');

			// if the file didn't already exist then we need to write out the
			// header line
			if (!alreadyExists) {
				if (header != null) {
					for (int i = 0; i < header.length; i++) {
						csvOutput.write(header[i]);
					}
					csvOutput.endRecord();
				}
			}
			// else assume that the file already has the correct header line
			int i, j;
			for (i = 0; i < table.length; i++) {
				for (j = 0; j < table[i].length; j++) {
					csvOutput.write(Double.toString(table[i][j]));
				}
				csvOutput.endRecord();
			}
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void SaveArrayListString(ArrayList<String> arr, String outputFile) {

		// sanity check
		if (arr == null || arr.isEmpty()) {
			System.err.println("ERROR: missing input array");
			return;
		}
		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(outputFile).exists();
		
		if (!alreadyExists) {
		 //TODO	- what should be implemented here?
		}
		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(
					new FileWriter(outputFile, true), ',');

			for (int i = 0; i < arr.size(); i++) {
				csvOutput.write(arr.get(i));
				csvOutput.endRecord();
			}
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
