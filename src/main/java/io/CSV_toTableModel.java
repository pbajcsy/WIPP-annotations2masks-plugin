package io;

import java.io.*;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import jxl.read.biff.BiffException;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

/**
 * This class is for loading csv files to Java TableModel
 * The Java TableModel can be found at 
 * http://docs.oracle.com/javase/1.4
 * .2/docs/api/javax/swing/table/TableModel.html
 * 
 * @author Qi Li, Ryo Kondo, Peter Bajcsy
 * 
 */
public class CSV_toTableModel {

	private TableModel _table = null;

	private String[] _headerArray;

	public static final String DELIMITER_PIPE = "\\|";
	public static final String DELIMITER_COMMA = ",";

	public CSV_toTableModel() {

	}

	public CSV_toTableModel(String inputFile) throws Exception {
		_table = readTable(inputFile);
	}

	public CSV_toTableModel(String inputFile, String delim) throws Exception {
		_table = readTable(inputFile, delim, true);
	}

	/**
	 * Creates new CSV_toTableModel object and invokes readTable method if
	 * "colName" is true, "headerRows" should be at least 1
	 * 
	 * @param inputFile - input file name
	 * @param delim - input delimiter
	 * @param colName - input flag whether the first n row contains column headings
	 * @param headerRows - input n rows that contain column headings
	 * 
	 * @throws Exception
	 */
	public CSV_toTableModel(String inputFile, String delim, boolean colName,
			int headerRows) throws Exception {
		_table = readTable(inputFile, delim, colName, headerRows);
	}

	public TableModel readTable(String _CSVFile, boolean colName) {
		TableModel table = new DefaultTableModel();
		try {
			table = readTable(_CSVFile, ",", colName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return table;
	}

	public TableModel readTable(String _CSVFile) throws Exception {
		TableModel table = new DefaultTableModel();
		table = readTable(_CSVFile, ",", true);
		return table;
	}

	public TableModel readTablePDL(String pdlFile) throws Exception {
		TableModel table = new DefaultTableModel();
		table = readTable(pdlFile, "\\|", true);
		return table;
	}

	public TableModel readTable(String _CSVFile, String delim, boolean colName)
			throws Exception {
		TableModel table = new DefaultTableModel();
		table = readTable(_CSVFile, delim, colName, 1);
		return table;
	}

	/**
	 * Reads a separated values file and integrates it into a DefaultTableModel.
	 * Skips first "headerRows" rows if colName is true. If "colName" is true,
	 * headerRows should be at least 1
	 * 
	 * @param _CSVFile
	 * @param delim
	 * @param colName
	 * @param headerRows
	 * @return
	 * @throws Exception
	 */
	public TableModel readTable(String _CSVFile, String delim, boolean colName,
			int headerRows) throws Exception {
		// System.out.println("Start readTable");

		FileReader fr = new FileReader(_CSVFile);
		BufferedReader reader = new BufferedReader(fr);
		String line = null;

		int numRows = headerRows;
		boolean firstLine;
		if (colName) {
			firstLine = true;
		} else {
			firstLine = false;
		}
		int numLines = 0;
		int numColumns = 0;

		line = reader.readLine();
		while (line != null && line.length() != 0) {
			if (firstLine) {
				if (numRows == 1)
					firstLine = false;
				numRows--;
			} else {
				numLines++;
			}

			int numTokens = line.split(delim).length;
			if (numColumns < numTokens) {
				numColumns = numTokens;
			}
			line = reader.readLine();
		}

		// now we know the number of lines.
		fr = new FileReader(_CSVFile);
		reader = new BufferedReader(fr);
		int curRow = 0;

		TableModel dataset = new DefaultTableModel(numLines, 0);
		char defaultname = 'A';
		if (colName) {
			firstLine = true;
			numRows = headerRows;
			while ((line = reader.readLine()) != null && line.length() != 0) {
				String[] tokens = null;
				if (delim.equals("\\|"))
					// if(line != "")
					tokens = lineToArray(line);
				else if (delim.equals("\",\"") && line.charAt(0) == '"'
						&& line.charAt(line.length() - 1) == '"') {
					line = line.substring(1, line.length() - 1);
					tokens = line.split(delim);
				} else
					tokens = line.split(delim);
				if (firstLine) {
					if (numRows == 1) {
						for (int i = 0; i < numColumns; i++) {
							if (i < tokens.length) {
								((DefaultTableModel) dataset)
										.addColumn(tokens[i]);
							} else {
								((DefaultTableModel) dataset).addColumn(String
										.valueOf(defaultname++));
							}
						}
						_headerArray = tokens;
						// for(int i=0; i<headerArray.length; i++)
						// System.out.print(headerArray[i]+" -- ");
						firstLine = false;
					}
					numRows--;
				}

				else {
					int curCol = 0;
					for (int i = 0; i < tokens.length; i++) {
						dataset.setValueAt(tokens[i], curRow, curCol);
						curCol++;
					}
					curRow++;
				}
			}

		} else {
			String[] tokens;
			for (int i = 0; i < numColumns; i++) {
				((DefaultTableModel) dataset).addColumn(String
						.valueOf(defaultname++));
			}
			fr = new FileReader(_CSVFile);
			reader = new BufferedReader(fr);
			line = reader.readLine();
			while (line != null && line.length() != 0) {
				if (delim.equals("\\|"))
					tokens = lineToArray(line);
				else if (delim.equals("\",\"") && line.charAt(0) == '"'
						&& line.charAt(line.length() - 1) == '"') {
					line = line.substring(1, line.length() - 1);
					tokens = line.split(delim);
				} else
					tokens = line.split(delim);
				/*
				if(curRow==0){
					for(int col=0;col<tokens.length;col++){
						dataset.setValueAt(col+1,0,col);
					}
					curRow++;
				}*/
				int curCol = 0;
				for (int i = 0; i < tokens.length; i++) {
					dataset.setValueAt(tokens[i], curRow, curCol);
					curCol++;
				}

				for (; curCol < numColumns; curCol++) {
					dataset.setValueAt("", curRow, curCol);
				}

				curRow++;
				line = reader.readLine();

			}
		}
		// System.out.println("End of readTable");
		for (int i = 0; i < dataset.getRowCount(); i++) {
			for (int j = 0; j < dataset.getColumnCount(); j++) {
				if (dataset.getValueAt(i, j) == null)
					dataset.setValueAt("", i, j);
			}
		}
		reader.close();
		return dataset;
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

	public void writeTable(TableModel t, String file, boolean colName)
			throws Exception {
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);

		int numrow = t.getRowCount();
		int numcol = t.getColumnCount();

		if (colName) {
			for (int i = 0; i < numcol; i++) {
				bw.write(t.getColumnName(i));
				if (i < numcol - 1)
					bw.write(",");
			}
			bw.write("\n");
		}
		for (int row = 0; row < numrow; row++) {
			for (int col = 0; col < numcol; col++) {
				Object value = new Object();
				try {
					value = t.getValueAt(row, col);
					bw.write(value.toString());
				} catch (NullPointerException e) {

				}

				if (col < numcol - 1) {
					bw.write(",");
				}
			}
			bw.write("\n");
		}

		bw.flush();
		bw.close();
	}


	/////////////////////////////////////////////////////////////
	// getters
	///////////////////////////////////////////////////////////
	public TableModel getTable() {
		return _table;
	}

	public String[] getHeaderArray() {
		return _headerArray;
	}

	public void setHeaderArray(String[] headerArray) {
		this._headerArray = headerArray;
	}

	////////////////////////////////////////////////////
	public void print() {
		System.out.println("row is " + this._table.getRowCount());
		System.out.println("col is " + this._table.getColumnCount());
		for (int i = 0; i < this._table.getRowCount(); i++) {
			for (int j = 0; j < (this._table.getColumnCount()); j++) {
				System.out.print(this._table.getValueAt(i, j) + " | ");
			}
			System.out.println();
		}
	}

	/**
	 * This method is useful when cXML file has to be created and the tags
	 * have to specify the type of each column
	 * @return
	 */
	public String [] decideFeatureType(){
		//sanity check
		if(_table == null || _table.getColumnCount() <1){
			System.err.println("table  is empty");
			return null;
		}
		String [] featureType = new String[_table.getColumnCount()];
		String tmpStr = new String();
		int tmpInt = 0;
		double tmpD =0.0;
		
		for(int columnIndex = 0; columnIndex < featureType.length;columnIndex++){
			tmpStr = new String(_table.getValueAt(0, columnIndex).toString());
			 featureType[columnIndex] = new String("String");
			 try{
				 tmpInt = Integer.parseInt(tmpStr);
				 featureType[columnIndex] = new String("Number");
			 }catch(NumberFormatException e) {
					//e.printStackTrace();
				 	System.out.println("INFO: "+tmpStr +" is not integer");
			 }
			 try{
				 tmpD = Double.parseDouble(tmpStr);
				 featureType[columnIndex] = new String("Number");
			 }catch(NumberFormatException e) {
					//e.printStackTrace();
				 System.out.println("INFO: "+tmpStr +" is not double");
			 }
				
		}
		
		return featureType;
	}
	/**
	 * This is the main method for testing
	 * @param args
	 */
	public static void main(String[] args) {
		int i;
		System.out.println("argument length=" + args.length);
		for (i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "]:" + args[i]);
		}
		if ((args == null) || (args.length < 1)) {
			System.out.println("Please, specify the input csv table name");
			System.out.println("arg = Input table Name");
			return;
		}

		String InFileName;

		InFileName = args[0];
		System.out.println(InFileName);

		CSV_toTableModel csvInstance = new CSV_toTableModel();
		try {
			// csvInstance = new CSV_toTableModel(InFileName, CSV_toTableModel.DELIMITER_PIPE, true, 2);
			csvInstance = new CSV_toTableModel(InFileName, CSV_toTableModel.DELIMITER_COMMA, true, 2);

			csvInstance.print();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		csvInstance.print();
	}

}
