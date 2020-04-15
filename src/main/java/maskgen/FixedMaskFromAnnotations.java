package maskgen;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import datatype.Annotation;
import datatype.ConcreteMaskColorMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import loci.formats.FormatException;

/**
 * 
 * This class is designed for the Concrete project
 * The methods are similar to those in MaskFromAnnotations 
 * but they perform the conversion of annotations to gray mask 
 * using a fixed pre-defined set of gray values
 * 
 * @author pnb
 *
 */
public class FixedMaskFromAnnotations extends MaskFromAnnotations{

	ConcreteMaskColorMap _concrete = new ConcreteMaskColorMap();
	
	public FixedMaskFromAnnotations() {
		// TODO Auto-generated constructor stub
	}

	public boolean convertUniqueColorsToMask(ArrayList<Annotation> annotations, ArrayList<Color> uniqueColors, boolean isMappingFixed, String rawImageName, String outFileName) throws IOException, FormatException {
		// sanity check
		if(annotations == null || annotations.size() < 1){
			System.err.println("ERROR: missing annotations");
			return false;
		}
		if(uniqueColors == null || uniqueColors.size() < 1){
			System.err.println("ERROR: missing uniqueColors");
			return false;
		}
		// set the mapping to the Concrete specific 
		_color2grayMapping = _concrete.getColor2grayMapping();
		// reset the color2graymapping
		//resetColor2grayMapping();
		
		//Initialize image reader and writer
		ImagePlus imgPlus = IJ.openImage(rawImageName);
		ImageProcessor ip3 = imgPlus.getProcessor();


		for(int row = 0; row < ip3.getHeight(); row++){
			for(int col = 0; col < ip3.getWidth(); col++){
				
			}
		}

			// reset values and min and max values
		ip3.set(0);
		ip3.resetMinAndMax();

		boolean grayColor = true;
		int grayValue = 1;
		for(Annotation a : annotations){
			
			boolean signal = false;
			for(Color unique :  uniqueColors){
				
				if (!signal && a.getFillColor().equals(unique)){
					signal = true;
					ip3.draw(a.shape);
					if(!grayColor){
						ip3.setColor(a.shape.getFillColor());
					}else{
						// for each unique shape fill color, find a unique gray scale value for the mask
						grayValue = findGrayColorForUniqueRGBColor(a.shape.getFillColor(), isMappingFixed);
						ip3.setColor(new Color(grayValue,grayValue,grayValue));
					}
					ip3.fill(a.shape);
										
				}
			}
		}

		//Define ROI 
		ShapeRoi compositeRoi = new ShapeRoi(annotations.get(0).shape);

		//Select everything that wasn't drawn and fill in black
		for (int i = 1; i < annotations.size(); i++) {
			boolean signal = false;
			for(Color unique :  uniqueColors){
				
				if (!signal && annotations.get(i).shape.getFillColor().equals(unique)){
					signal = true;	
					compositeRoi = compositeRoi.or(new ShapeRoi(annotations.get(i).shape));

				}
			}
		}
		compositeRoi = compositeRoi.xor(new ShapeRoi(new Roi(0,0,ip3.getWidth(),ip3.getHeight())));
		ip3.setRoi(compositeRoi);
		//	ip3.draw(compositeRoi);
		ip3.setColor(Color.BLACK);
		ip3.fill(compositeRoi);

		// For short and float images, recalculates the min and max image values needed to correctly display the image. For ByteProcessors, resets the LUT.
		ip3.resetMinAndMax();		
		System.out.println("min = "+ ip3.getMin() +", max="+ ip3.getMax() );
		
		ImagePlus imp = new ImagePlus("Result", ip3);
		FileSaver tiffSave = new FileSaver(imp);
		tiffSave.saveAsTiff(outFileName);

		//Close reader.
		//reader.close();
		
		//createTiles(ip3);
		// TODO - separate function call: createSegmentTiles(ip3);
		//IJ.run(result, "peter", "");
		System.out.println("Done!");

		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
