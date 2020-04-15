package datatype;

import java.util.ArrayList;


/**
 * This class is specific to the concrete project with Steve Feldman
 * It contains the definition of mask gray according to the Textual labels in the annotations
 * 
 * @author pnb
 *
 */
public class ConcreteMaskLabelMap extends MaskLabelMap	{

	// damage labels
	public final static String PASTE_DAMAGE = new String("Paste_damage");
	public final static String AGGREGATE_DAMAGE = new String("Aggregate_damage");
	public final static String AIR_VOIDS = new String("Air_void");

	// contextual labels
	public final static String PASTE = new String("Paste");
	public final static String FELDSPAR = new String("Feldspar");
	public final static String QUARTZITE = new String("Quartzite");
	public final static String VOLCANIC_GLASS = new String("Volcanic_glass");
	public final static String CRACKS = new String("Cracks");
	public final static String DISSOLUTION = new String("Dissolution");
	public final static String IRON_OXIDE = new String("Iron_oxide");
	public final static String AUGITE = new String("Augite");
	// added for reporting purposes
	public final static String VOLCANIC_DAMAGE = new String("Volcanic_damage");
	
	public final static int NUM_OF_CLASSES = 12;
	
	/**
	 * The constructor will force the specific color to gray mapping 
	 * for the concrete project
	 *  
	 * @param annotationSet - this parameter helps with going between multiple 
	 * annotation assignments as the colors and labels changed over time in Steve's annotations
	 *  
	 */
	public ConcreteMaskLabelMap(int annotationSet) {
		ArrayList<String> uniqueLabels = new ArrayList<String>();
	
		String uniqueLabel = null;
		
		if(annotationSet == 1 ){
			////////////////////////
			// this option corresponds to the pixel-level annotations
			// from Steve using ImageJ/Fiji tool
			
			//dark yellow --> voids
			uniqueLabel = new String(AIR_VOIDS); 
			uniqueLabels.add(uniqueLabel);
			
			// green --> paste
			uniqueLabel = new String(PASTE);
			uniqueLabels.add(uniqueLabel);
			
			// blue --> Feldspar
			uniqueLabel = new String(FELDSPAR); 
			uniqueLabels.add(uniqueLabel);
			
			// magenta --> quartzite
			uniqueLabel = new String(QUARTZITE);
			uniqueLabels.add(uniqueLabel);
			
			// cyan --> VOLCANIC_GLASS
			uniqueLabel = new String(VOLCANIC_GLASS); 
			uniqueLabels.add(uniqueLabel);
			
			// red --> cracks
			uniqueLabel = new String(CRACKS);
			uniqueLabels.add(uniqueLabel);
			
			// yellow --> dissolution
			uniqueLabel = new String(DISSOLUTION);
			uniqueLabels.add(uniqueLabel);
			
			// red in separate mask --> iron oxide
			uniqueLabel = new String(IRON_OXIDE);
			uniqueLabels.add(uniqueLabel);
			
			// green in separate mask--> augite
			uniqueLabel = new String(AUGITE);
			uniqueLabels.add(uniqueLabel);

			// yellow shade  --> volcanic damage
			uniqueLabel = new String(VOLCANIC_DAMAGE);
			uniqueLabels.add(uniqueLabel);
			
		}else{
			////////////////////////
			// this option corresponds to the WIPP annotations
			// from Steve created by overlapping raw intensity FOVs 
			// and model-based damage binary images
			
			// green --> PASTE_DAMAGE (0,191,0) or (53,173,39)
			uniqueLabel = new String(PASTE_DAMAGE);
			uniqueLabels.add(uniqueLabel);
			
			// cyan --> AGGREGATE_DAMAGE
			uniqueLabel = new String(AGGREGATE_DAMAGE); 
			uniqueLabels.add(uniqueLabel);

			// orange --> AIR_VOIDS
			uniqueLabel = new String(AIR_VOIDS);
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
