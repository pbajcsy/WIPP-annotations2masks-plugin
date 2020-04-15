package datatype;

import java.util.ArrayList;


/**
 * This class is specific to the organelle project with Ronit Schilling
 * It contains the definition of mask gray according to the Textual labels in the annotations
 * 
 * @author pnb
 *
 */
public class OrganelleMaskLabelMap extends MaskLabelMap	{

	// damage labels
	public final static String ORGANELLE = new String("Organelle_location");
	public final static String CELL = new String("Cell_region");
	
	public final static String ORGANELLE_TRACK = new String("Organelle_track");

	public final static int NUM_OF_CLASSES = 3;
	
	/**
	 * The constructor will force the specific color to gray mapping 
	 * for the organelle project
	 *  
	 * @param annotationSet - this parameter helps with going between multiple 
	 * annotation assignments as the colors and labels changed over time in Ronit's annotations
	 *  
	 */
	public OrganelleMaskLabelMap(int annotationSet) {
		ArrayList<String> uniqueLabels = new ArrayList<String>();
	
		String uniqueLabel = null;
		
		if(annotationSet == 1 ){
			////////////////////////
			// this option corresponds to the annotations from Ronit while at NIST
			
			//red circles --> organelle locations
			// yellow polygons --> cell region
			
			//red circle --> organelle
			uniqueLabel = new String(ORGANELLE); 
			uniqueLabels.add(uniqueLabel);
			
			// yellow --> cell region
			uniqueLabel = new String(CELL);
			uniqueLabels.add(uniqueLabel);
			

		}else{
			////////////////////////
			// this option corresponds to the WIPP annotations
			// from Ronit from Georgia Tech
			
			//red circle --> organelle
			uniqueLabel = new String(ORGANELLE); 
			uniqueLabels.add(uniqueLabel);
			
			// yellow --> cell region
			uniqueLabel = new String(CELL);
			uniqueLabels.add(uniqueLabel);
					}


	
		MaskLabelMapInit(uniqueLabels);
	}
	/**
	 * Implementation of the abstract method 
	 */
	public void MaskLabelMapInit(ArrayList<String> uniqueLabels){
		initMaskLabelMap(uniqueLabels);
	}
	

		

}
