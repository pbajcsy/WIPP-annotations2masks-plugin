package io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.csvreader.CsvWriter;

/*import simulator.param.ImPts;
*/
public class CSV_IOsupport {

	public CSV_IOsupport() {

	}

	/**
	 * this method saves out an array of numbers into a file
	 * 
	 * @param arr
	 *            - input array
	 * @param OutFileName
	 *            - output filename
	 * @return true if the operation succeeded
	 * @throws IOException
	 */
	public static boolean SaveArray(double[] arr, String OutFileName)
			throws IOException {
		// sanity check
		if (arr == null || OutFileName == null) {
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);
		for (int i = 0; i < arr.length - 1; i++) {
			out.write(arr[i] + ", ");
		}
		out.write(arr[arr.length - 1] + "\n");
		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}
	
	public static <T> boolean SaveArrayGeneric( List<T> arr, String OutFileName, boolean isValuePerLine)
			throws IOException {
		// sanity check
		if (arr == null || OutFileName == null) {
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}

		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);
		
		int idx = 0;
		if(isValuePerLine){
			for (Iterator<T> k = arr.iterator(); k.hasNext();) {
				T value = k.next();
				out.write(idx + ", "+ value + "\n");
				idx++;
			}			
		}else{
			for (Iterator<T> k = arr.iterator(); k.hasNext();) {
				T value = k.next();
				if(idx < arr.size() - 1){
					out.write(value + ", ");
				}else{
					out.write(value + "\n");
				}
				idx++;
			}
		}
		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}
	
	/**
	 * This method is for appending a string (one row) to an existing file
	 * 
	 * @param saveString
	 * @param OutFileName
	 * @return
	 * @throws IOException
	 */
	public static boolean SaveAppendArray(String saveString, String OutFileName) throws IOException {

		// sanity check
		if (saveString == null || OutFileName == null) {
			System.err.println("input string is null or output file name is null" );
			return false;
		}

		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(OutFileName).exists();

		try {
			CsvWriter csvOutput = new CsvWriter(new FileWriter(OutFileName, true), ',');

			csvOutput.write(saveString);
			csvOutput.endRecord();
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	
	public static boolean SaveArray(String[] header, double[] arr,
			String OutFileName) throws IOException {

		// sanity check
		if (arr == null || OutFileName == null) {
			return false;
		}

		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(OutFileName).exists();

		try {
			CsvWriter csvOutput = new CsvWriter(

			new FileWriter(OutFileName, true), ',');

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
			for (i = 0; i < arr.length; i++) {
				csvOutput.write(Double.toString(arr[i]));
			}
			csvOutput.endRecord();
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

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

	/**
	 * This method is useful for saving image path, image name and double attributes per image
	 * into one row (or one record)
	 * 
	 * @param header -if first row should contain the headers
	 * @param strTable - string entries
	 * @param table - double entries
	 * @param outputFile - output file name
	 */
	public static void SaveTable(String[] header, String [][] strTable, double[][] table,
			String outputFile) {

		// sanity check
		if (table == null) {
			System.err.println("ERROR: missing input table of doubles");
			return;
		}
		if (strTable == null) {
			System.err.println("ERROR: missing input table of strings");
			return;
		}
		if(strTable.length != table.length){
			System.err.println("ERROR: mismatched lengths of input tables of doubles and strings");
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
				// write the string entries 
				for (j = 0; j < strTable[i].length; j++) {
					csvOutput.write(strTable[i][j]);
				}
				// write the double entries
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

	/** 
	 * This method was added to support printing mutual distance between any two pairs of clusters
	 * 
	 * @param header - any text
	 * @param table - ArrayList<Double> of the upper triangular entries of a matrix
	 * representing pair-wise distances
	 * @param OutFileName - output file name
	 * @return - boolean true if successful 
	 * @throws IOException
	 */
	public static boolean saveUpperTriangleMatrix(String[] header,
			ArrayList<Double> table, String OutFileName) throws IOException {
		// sanity check
		if (table == null || table.size() == 0) {
			System.err.println("ERROR: table is null or empty");
			return false;
		}
		if (OutFileName == null) {
			System.err.println("ERROR: OutFileName is null");
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}

		// before we open the file check to see if it already exists
		boolean alreadyExists = new File(OutFileName).exists();

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(OutFileName,
					true), ',');

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

			// compute the number of clusters based on the array size which
			// corresponds to the upper triangular entries of the pair-wise
			// mutual
			// distance matrix
			double numClusters = (1 + Math.sqrt(1 + 8 * table.size())) * 0.5;

			int i, j, index = 0;
			for (i = 0; i < numClusters; i++) {
				for (j = 0; j < numClusters; j++) {
					if (j >= i + 1) {
						csvOutput.write(Double.toString(table.get(index)));
						index++;
					} else {
						csvOutput.write(",");
					}
				
				}
				csvOutput.endRecord();
			}
			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * This method allows to save a hash map keys and values in a cvs file
	 * 
	 * @param hash
	 *            -input hash map
	 * @param OutFileName
	 *            - output csv file name
	 * @return - output boolean outcome
	 * @throws IOException
	 */
	public static boolean SaveHashMap(HashMap<?, ?> hash, String OutFileName)
			throws IOException {
		// sanity check
		if (hash == null || hash.size() < 1 || OutFileName == null) {
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);

		/*		System.out.println("Retrieving all keys from the HashMap");
				Iterator<?> iterator = hash.keySet().iterator();
				String temp  = null;
				while (iterator.hasNext()) {
					temp = iterator.toString();
					out.write(temp + ", ");
					iterator.next();
				}
				out.write( "\n");
		*/
		System.out.println("Retrieving all values from the HashMap");
		Iterator<?> iterator = hash.entrySet().iterator();
		String split[] = null;
		Object temp1 = null;
		while (iterator.hasNext()) {
			temp1 = iterator.next();

			split = (temp1.toString()).split("=");
			out.write(split[0] + ", " + split[1] + "\n");
			System.out.println(temp1);

		}
		out.write("\n");

		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}

	public static boolean AppendArray(double[] arr, String OutFileName)
			throws IOException {
		// sanity check
		if (arr == null || OutFileName == null) {
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);

		// FileWriter writer = new FileWriter(output);
		for (int i = 0; i < arr.length - 1; i++) {
			out.append(arr[i] + ", ");
		}
		out.append(arr[arr.length - 1] + "\n");
		// flush out the buffer.
		out.flush();
		out.close();
		return true;
	}

	/**
	 * This method saves two double arrays with their header to an output file
	 * 
	 * @param header
	 *            - input string containing the headings for the two arrays
	 * @param arr1
	 *            - input array 1
	 * @param arr2
	 *            - input array 2
	 * @param outputFile
	 *            - input parameter specifying the output file name
	 */
	public static void Save(String[] header, double[] arr1, double[] arr2,
			String outputFile) {

		// sanity check
		if (arr1 == null || arr2 == null) {
			System.err.println("ERROR: missing arrays");
			return;
		}
		if (arr1.length != arr2.length) {
			System.err.println("ERROR: length has to be the same");
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
				for (int i = 0; i < header.length; i++) {
					csvOutput.write(header[i]);
				}
				csvOutput.endRecord();
			}
			// else assume that the file already has the correct header line

			for (int i = 0; i < arr1.length - 1; i++) {
				csvOutput.write(Double.toString(arr1[i]));
				csvOutput.write(Double.toString(arr2[i]));
				csvOutput.endRecord();
			}

			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

/*	public static boolean SaveImPts(ImPts[] arr, String OutFileName)
			throws IOException {
		// sanity check
		if (arr == null || OutFileName == null) {
			return false;
		}
		// open the file into which the output will be written.
		String output = new String(OutFileName);
		if (OutFileName.endsWith(".csv")) {
			output = OutFileName;
		} else {
			output += ".csv";
		}
		System.out.println("INFO: File Name = " + output);
		FileOutputStream fileOut = new FileOutputStream(output);
		OutputStreamWriter out = new OutputStreamWriter(fileOut);
		out.write("Row" + ", " + "Col" + "\n");
		for (int i = 0; i < arr.length - 1; i++) {
			out.write(arr[i].getRow() + ", " + arr[i].getCol() + "\n");
		}
		// out.write(arr[arr.length - 1] + "\n");
		// flush out the buffer.
		out.flush();
		return true;
	}
*/
	public static double[] getDoubleNumbers(int[] numbers) {
		double[] newNumbers = new double[numbers.length];

		for (int index = 0; index < numbers.length; index++)
			newNumbers[index] = (double) numbers[index];

		return newNumbers;
	}

	/**
	 * This method appends a column to a table
	 * 
	 * @param existingArr
	 *            - input table
	 * @param newcolumn
	 *            - input column
	 * @return - output table with the appended column
	 */
	public static double[][] appendColumn(double[][] existingArr,
			double[] newcolumn) {
		// sanity check
		if (existingArr == null || newcolumn == null) {
			System.err.println("ERROR: input arrays are null");
			return null;
		}
		if (existingArr.length != newcolumn.length) {
			System.err
					.println("ERROR: input arrays do macth in their number of rows");
			return null;
		}

		double[][] retArr = new double[existingArr.length][existingArr[0].length + 1];
		int index, index2;
		for (index2 = 0; index2 < retArr[0].length - 1; index2++) {
			for (index = 0; index < retArr.length; index++) {
				retArr[index][index2] = existingArr[index][index2];
			}
		}
		index2 = retArr[0].length - 1;
		for (index = 0; index < retArr.length; index++) {
			retArr[index][index2] = newcolumn[index];
		}

		return retArr;
	}

	public static double[][] appendManyColumns(double[][] existingArr,
			double[][] newcolumns) {
		// sanity check
		if (existingArr == null || newcolumns == null) {
			System.err.println("ERROR: input arrays are null");
			return null;
		}
		if (existingArr.length != newcolumns.length) {
			System.err
					.println("ERROR: input arrays do match in their number of rows");
			return null;
		}

		double[][] retArr = new double[existingArr.length][existingArr[0].length
				+ newcolumns[0].length];
		int index, index2;
		for (index2 = 0; index2 < existingArr[0].length; index2++) {
			for (index = 0; index < retArr.length; index++) {
				retArr[index][index2] = existingArr[index][index2];
			}
		}
		for (index2 = existingArr[0].length; index2 < retArr[0].length; index2++) {
			for (index = 0; index < retArr.length; index++) {
				retArr[index][index2] = newcolumns[index][index2
						- existingArr[0].length];
			}
		}
		return retArr;
	}

	/**
	 * This method merges three columns into a table
	 * 
	 * @param arr1
	 *            - first input column
	 * @param arr2
	 *            - second input column
	 * @param arr3
	 *            - third input column
	 * @return - output table
	 */
	public static double[][] merge3ColumnsToTable(double[] arr1, double[] arr2,
			double[] arr3) {
		// sanity check
		if (arr1 == null || arr2 == null || arr3 == null) {
			System.err.println("ERROR: input arrays are null");
			return null;
		}
		if (arr1.length != arr2.length || arr3.length != arr2.length) {
			System.err
					.println("ERROR: input arrays do match in their number of rows");
			return null;
		}

		double[][] retArr = new double[arr1.length][3];
		int index, index2;
		index2 = 0;
		for (index = 0; index < retArr.length; index++) {
			retArr[index][index2] = arr1[index];
		}
		index2 = 1;
		for (index = 0; index < retArr.length; index++) {
			retArr[index][index2] = arr2[index];
		}
		index2 = 2;
		for (index = 0; index < retArr.length; index++) {
			retArr[index][index2] = arr3[index];
		}
		return retArr;
	}

	/**
	 * 
	 * @param args
	 *            - args[0] is output file name without suffix of the synthetic
	 *            image
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		CSV_IOsupport myTest = new CSV_IOsupport();

		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 1)) {
			System.out.println("Please, specify the output name");
			System.out.println("arg = Output_ImageName");
			return;
		}

		String OutFileName;
		Boolean ret = true;

		OutFileName = args[0];
		System.out.println(OutFileName);

		double[] arr = new double[50];
		for (i = 0; i < arr.length; i++)
			arr[i] = i;

		//CSV_IOsupport.SaveArray(arr, OutFileName);
		//https://stackoverflow.com/questions/529085/how-to-create-a-generic-array-in-java
		//Set<Double> ofDouble =  new Set<>(Double[]::new);  
		ArrayList<Double> arr2 = new ArrayList<Double>();
		for (i = 0; i < arr.length; i++)
			arr2.add(new Double(arr[i]));
		
		boolean isValuePerLine = false;
		CSV_IOsupport.SaveArrayGeneric(arr2, OutFileName, isValuePerLine);
		


	}
}