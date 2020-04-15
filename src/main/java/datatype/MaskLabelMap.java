package datatype;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This is a container for pre-defined mapping between mask gray values and 
 * labeled annotations in WIPP defining an image region class
 * The image mask is the inputs to the CNN training algorithm.
 * 
 * @author pnb
 *
 */

abstract public class MaskLabelMap {

	private HashMap<String, Integer> _label2grayMapping = new HashMap<String, Integer>();

	abstract public void MaskLabelMapInit(ArrayList<String> uniqueLabels);
	
	/**
	 * initialize the label to gray mapping
	 * @param uniqueLabels
	 */
	public void initMaskLabelMap(ArrayList<String> uniqueLabels){
	
		int gray = 1;
		for (Iterator<String> k = uniqueLabels.iterator(); k.hasNext();) {
			String uniqueString = k.next();
			_label2grayMapping.put(uniqueString, gray);
			gray++;
		}
	}
	/////////////////////////////////////////////////////
	public HashMap<String, Integer> getLabel2grayMapping(){
		return _label2grayMapping;
	}
	public void resetLabel2grayMapping(){
		_label2grayMapping.clear();
	}	
	public String printLabel2grayMapping(){
		if(_label2grayMapping.isEmpty()){
			System.out.println("INFO: there is no Label 2 gray mapping ");
			return "NA";
		}
		System.out.println("label 2 gray mapping: " + _label2grayMapping.toString());	
		return ("label 2 gray mapping, " + _label2grayMapping.toString());
	}
	
}
