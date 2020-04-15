package maskgen;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import io.AnnotationLoader;
import loci.formats.FormatException;
import util.FileOper;
import util.MathOper;

/**
 * This class is designed to accommodate the WIPP based annotations that are based on an overlay of model-based binary mask
 * and the raw image in order to assess whether the model is correct
 * Use in the concrete project
 * 
 * @author pnb
 *
 */
public class MaskJoin {

	public boolean MaskBooleanJoin(String AnnotMaskFileFolder, String ModelMaskFileFolder, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (AnnotMaskFileFolder == null || ModelMaskFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null AnnotMaskFileFolder, ModelMaskFileFolder or outFileFolder ");
			return false;
		}

		/*Check directory if exist*/
		File directory=new File(AnnotMaskFileFolder);
		if(!directory.exists()){
			System.out.println("Input annotation mask Directory does not exist: " + AnnotMaskFileFolder);
			return false;
		}
		directory=new File(ModelMaskFileFolder);
		if(!directory.exists()){
			System.out.println("Input model mask Directory does not exist: " + ModelMaskFileFolder);
			return false;
		}
		directory=new File(outFileFolder);
		if(!directory.exists()){
			System.out.println("output Directory does not exist: " + outFileFolder);
			return false;
		}
		///////////////////////////////////////////////////////////
		// getting JSON files to process
		Collection<String> dirAnnotFiles = FileOper.readFileDirectory(AnnotMaskFileFolder);
		if (dirAnnotFiles.isEmpty()){
			System.err.println("dirAnnotFiles Directory is empty: " + dirAnnotFiles);
			return false;
		}
		
		// select JSON files with the right suffix
		String suffixTIFF = new String(".tif");
		System.out.println("TEST: dirAnnotFiles= " + dirAnnotFiles);
		Collection<String> dirSelectAnnotFiles = FileOper.selectFileType(dirAnnotFiles,suffixTIFF );	
		
		// sort stacks to process
		Collection<String> sortedAnnotInFolder = FileOper.sort(dirSelectAnnotFiles,
				FileOper.SORT_ASCENDING);
		///////////////////////////////////////////////////////////////
		// getting images to process
		Collection<String> dirModelFiles = FileOper.readFileDirectory(ModelMaskFileFolder);

		// select images with the right suffix
		//String suffixTIFF = new String(".tif");
		Collection<String> dirSelectModelFiles = FileOper.selectFileType(dirModelFiles,suffixTIFF );	
		if (dirSelectModelFiles.isEmpty()){
			System.err.println("dirSelectModelFiles Directory is empty: " + dirSelectModelFiles);
			return false;
		}
		
		// sort stacks to process
		Collection<String> sortedModelsInFolder = FileOper.sort(dirSelectModelFiles,
				FileOper.SORT_ASCENDING);
			
		//////////////////////////////////////////////////////
		AnnotationLoader annotClass = new AnnotationLoader();
		
		String annotFileName = new String();	
		String modelFileName = new String();
		String outFileName = new String(outFileFolder);
		if(!outFileFolder.endsWith(File.separator)) {
            outFileName += File.separator;
		}
			
		// store metadata about the execution
		//ArrayList<String> strSaveMapping = new ArrayList<String>();
		
		boolean foundMatch = false;
		for (Iterator<String> k = sortedAnnotInFolder.iterator(); k.hasNext();) {
			annotFileName = k.next(); //  looks like: mask_Sample__3_28_.ome.tif or Sample__13_39_.ome.tif
			String nameAnnot = (new File(annotFileName)).getName();
			// TODO check what to remove, this case is mask_ and  .ome.tif (mask_Sample__3_28_.ome.tif)
			// this is for the case: mask_Sample__3_28_.ome.tif
			//nameAnnot = nameAnnot.substring(5, nameAnnot.length()-8); 
			// this is for the case Sample__13_39_.ome.tif
			nameAnnot = nameAnnot.substring(0, nameAnnot.length()-8); 
						
			// find matching rawFileName
			foundMatch = false;
			for(Iterator<String> r = sortedModelsInFolder.iterator(); !foundMatch && r.hasNext(); ){
				modelFileName = r.next(); // looks like: Sample__3_28__bySize_t68_s198.ome.tif or Sample_(1,19)_bySize_t68_s198.tif
				String nameTIFF = (new File(modelFileName)).getName();
				if(nameTIFF.contains("(") || nameTIFF.contains(")") || nameTIFF.contains(",") ){
					//nameTIFF = nameTIFF.replaceAll("\\([^()]*\\)", "_");
					nameTIFF = nameTIFF.replaceAll("\\(", "_");
					nameTIFF = nameTIFF.replaceAll("\\)", "_");
					nameTIFF = nameTIFF.replaceAll(",", "_");
					nameTIFF = nameTIFF.substring(0, nameTIFF.length()-20); // TODO check what to remove Sample__3_28__bySize_t68_s198.tif
					System.out.println("modified nameTIFF = " + nameTIFF);
				}else{
					nameTIFF = nameTIFF.substring(0, nameTIFF.length()-24); // TODO check what to remove Sample__3_28__bySize_t68_s198.ome.tif
				}
				if(nameAnnot.equalsIgnoreCase(nameTIFF)){
					
					foundMatch = true;
				}
			}
			if(!foundMatch){
				System.err.println("ERROR: could not find a matching annotated TIFF image to the model TIFF image");
				continue;
			}
			System.out.println("INFO: matching pair: annotated image = " +  annotFileName + " model binary image = " + modelFileName);
			
			//ArrayList<Annotation> annotations =  annotClass.readJSONfromWIPP(JSONfileName);
			//AnnotationLoader.printArrayListAnnot(annotations);

			//MaskFromAnnotations myClass = new MaskFromAnnotations();
			// construct the output file name
			//String name = new String(nameAnnot);//(new File(JSONfileName)).getName();
			//name = name.substring(0, name.length()-5) + ".tif";
			
			ImagePlus annotImage = IJ.openImage(annotFileName);
			//ImageProcessor ipAnnot = annotImage.getProcessor();

			ImagePlus modelImage = IJ.openImage(modelFileName);
			//ImageProcessor ipModel = modelImage.getProcessor();
			ImagePlus res =  AnnotModelJoin(annotImage,  modelImage);
			

			FileSaver fs = new FileSaver(res);
			String OutputName = new String(outFileName + nameAnnot + ".tif" );	
				
			fs.saveAsTiff(OutputName);
		}
		
		return true;
	}
	
	/**
	 * This method takes two grayscale masks and merges the non-zero labels in the two images
	 * 
	 * @param annotImage - mask 1
	 * @param modelImage - mask 2
	 * @return ImagePlus of the merged mask
	 */
	public ImagePlus AnnotModelJoin(ImagePlus annotImage, ImagePlus modelImage){

		ImageProcessor ipAnnot = annotImage.getProcessor();
		ImageProcessor ipModel = modelImage.getProcessor();
		
        int width = ipAnnot.getWidth();
        int height = ipAnnot.getHeight();
        int nbElements = width*height;
        byte[] pix = new byte[nbElements];

        for(int k = 0; k < nbElements; k++) {
        	if(ipAnnot.get(k) != 0 && ipModel.get(k) != 0){
        		pix[k] = (byte)ipAnnot.get(k);
        	}else{
        		pix[k] = 0;
        	}
        }
        
        
        ImageProcessor ip = new ByteProcessor(width, height, pix);
        ImagePlus result = new ImagePlus("AnnotModelJoin", ip);
        return result;
       
	}
	
	/**
	 * This method is used for merging two sets of binary masks 
	 * 
	 * @param Mask1FileFolder
	 * @param Mask2FileFolder
	 * @param outFileFolder
	 * @return
	 * @throws IOException
	 * @throws FormatException
	 */
	public boolean BinaryMaskBooleanJoinBatch(String Mask1FileFolder, String Mask2FileFolder, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (Mask1FileFolder == null || Mask2FileFolder == null || outFileFolder == null) {
			System.err.println("Error: null Mask1FileFolder, Mask2FileFolder or outFileFolder ");
			return false;
		}

		/*Check directory if exist*/
		File directory=new File(Mask1FileFolder);
		if(!directory.exists()){
			System.out.println("Input mask1 Directory does not exist: " + Mask1FileFolder);
			return false;
		}
		directory=new File(Mask2FileFolder);
		if(!directory.exists()){
			System.out.println("Input mask2 Directory does not exist: " + Mask2FileFolder);
			return false;
		}
		directory=new File(outFileFolder);
		if(!directory.exists()){
			System.out.println("output Directory does not exist: " + outFileFolder);
			return false;
		}
		///////////////////////////////////////////////////////////
		// getting mask files to process
		Collection<String> dirMask1Files = FileOper.readFileDirectory(Mask1FileFolder);
		if (dirMask1Files.isEmpty()){
			System.err.println("dirMask1Files Directory is empty: " + dirMask1Files);
			return false;
		}
		
		// select image files with the right suffix
		String suffixTIFF = new String(".tif");
		System.out.println("TEST: dirMask1Files= " + dirMask1Files);
		Collection<String> dirSelectMask1Files = FileOper.selectFileType(dirMask1Files,suffixTIFF );	
		
		// sort stacks to process
		Collection<String> sortedMask1InFolder = FileOper.sort(dirSelectMask1Files,
				FileOper.SORT_ASCENDING);
		///////////////////////////////////////////////////////////////
		// getting images to process
		Collection<String> dirMask2Files = FileOper.readFileDirectory(Mask2FileFolder);

		// select images with the right suffix
		//String suffixTIFF = new String(".tif");
		Collection<String> dirSelectMask2Files = FileOper.selectFileType(dirMask2Files,suffixTIFF );	
		if (dirSelectMask2Files.isEmpty()){
			System.err.println("dirSelectMask2Files Directory is empty: " + dirSelectMask2Files);
			return false;
		}
		
		// sort stacks to process
		Collection<String> sortedMask2InFolder = FileOper.sort(dirSelectMask2Files,
				FileOper.SORT_ASCENDING);
			
		//////////////////////////////////////////////////////	
		String mask1FileName = new String();	
		String mask2FileName = new String();
			
		boolean foundMatch = false;
		for (Iterator<String> k = sortedMask2InFolder.iterator(); k.hasNext();) {
			mask2FileName = k.next(); //  looks like: binary_mask_Sample__3_33_.tif
			String nameMask2 = (new File(mask2FileName)).getName();
			nameMask2 = nameMask2.substring(0, nameMask2.length()-4); 
			// find matching rawFileName
			foundMatch = false;
			for(Iterator<String> r = sortedMask1InFolder.iterator(); !foundMatch && r.hasNext(); ){
				mask1FileName = r.next(); // looks like: Sample__3_28__bySize_t68_s198.ome.tif or Sample_(1,19)_bySize_t68_s198.tif
				String nameMask1 = (new File(mask1FileName)).getName();
				nameMask1 = nameMask1.substring(0, nameMask1.length()-4); 
				
				if(nameMask2.equalsIgnoreCase(nameMask1)){				
					foundMatch = true;
				}
			}
			if(!foundMatch){
				System.err.println("ERROR: could not find a matching mask1 image to the mask 2 image");
				continue;
			}
			System.out.println("INFO: matching pair: matching mask1 image = " +  mask1FileName + " , mask 2 image = " + mask2FileName);
			
			String outFileName = new String(outFileFolder);
			if(!outFileFolder.endsWith(File.separator)) {
				outFileName += File.separator;
			}
			String OutputName = new String(outFileName + nameMask2 + ".tif" );	


			BinaryMaskBooleanJoin(mask1FileName, mask2FileName, OutputName);
				
		}
		
		return true;
	}

	
	/**
	 * This method is for joining two binary masks using MathOper boolean OR operation
	 *  
	 * @param mask1_filename
	 * @param mask2_filename
	 * @param out_filename
	 */
	public void BinaryMaskBooleanJoin(String mask1_filename, String mask2_filename, String out_filename ){
				
		ImagePlus mask1 = IJ.openImage(mask1_filename);
		ImagePlus mask2 = IJ.openImage(mask2_filename);
		ImagePlus res  = MathOper.booleanOR(mask1,  mask2);
		
		FileSaver fs = new FileSaver(res);
		//String OutputName = new String(outFileName + nameAnnot + ".tif" );	
			
		fs.saveAsPng(out_filename);
	}
	
	

	/**
	 * This method is for processing two color masks using ColorMaskJoin method
	 * 
	 * @param mask1_filename
	 * @param mask2_filename
	 * @param out_filename
	 */
	public void ColorMaskJoinBatch(String mask1_filename, String mask2_filename, String out_filename ){
		
	
		ImagePlus mask1 = IJ.openImage(mask1_filename);
		ImagePlus mask2 = IJ.openImage(mask2_filename);
		ImagePlus res =  ColorMaskJoin(mask1,  mask2);
		
		FileSaver fs = new FileSaver(res);
		//String OutputName = new String(outFileName + nameAnnot + ".tif" );	
			
		fs.saveAsPng(out_filename);
	}

	/**
	 * This method takes two color masks and merges the non-grayscale labels in the two images
	 * 
	 * @param modelImage - mask 1
	 * @param annotImage - mask 2
	 * @return ImagePlus of the merged mask
	 */
	public ImagePlus ColorMaskJoin( ImagePlus mask1, ImagePlus mask2){

		// sanity check
		if(mask1 == null || mask2 == null){
			System.err.println("ERROR: input images are null");
			return null;			
		}

		ImageProcessor ipModel = mask1.getProcessor();
		ImageProcessor ipAnnot = mask2.getProcessor();
		
		ColorProcessor colorIpAnnot = (ColorProcessor) ipAnnot.convertToRGB();
		ColorProcessor colorIpModel = (ColorProcessor) ipModel.convertToRGB();
		if(colorIpAnnot.getWidth() != colorIpModel.getWidth() || colorIpAnnot.getHeight() != colorIpModel.getHeight()){
			System.err.println("ERROR: dimensions of the two images do not match");
			return null;
		}
	
		// red --> IRON_OXIDE
		Color iron_oxideColor = new Color(255, 0, 0);
		// green --> AUGITE		
		Color augiteColor = new Color(0, 255, 0);

		// white --> IRON_OXIDE
		Color iron_oxideColorNew = new Color(255, 255, 200);
		// purple --> AUGITE		
		Color augiteColorNew = new Color(255, 200, 255);
		
		// dark green manually assigned to fix Steve's annotation = 34, 177, 76 since red was used in the same image for cracks and iron oxide
		// must be replaced by white --> IRON_OXIDE
		Color iron_oxideColor_bad = new Color(34,177,76);
		
		for (int x = 0; x < colorIpAnnot.getWidth(); x++) {
			for (int y = 0; y < colorIpAnnot.getHeight(); y++) {
				Color pixelColor = colorIpAnnot.getColor(x, y);
				if(pixelColor.getRed() == pixelColor.getGreen() && pixelColor.getGreen() == pixelColor.getBlue()){		
				    // skip 
				}else{
					// overwrite the pixel
					//Color mask1_Color = colorIpAnnot.getColor(x, y);
/*					if(pixelColor.getRed() == iron_oxideColor.getRed() && pixelColor.getGreen() == iron_oxideColor.getGreen() && pixelColor.getBlue() == iron_oxideColor.getBlue()){		
					  // overwrite the pixel with the new color to avoid already assigned color to other classes
						colorIpModel.setColor(iron_oxideColorNew);
						colorIpModel.drawPixel(x,y);	
					}else{
						if(pixelColor.getRed() == augiteColor.getRed() && pixelColor.getGreen() == augiteColor.getGreen() && pixelColor.getBlue() == augiteColor.getBlue()){		
							  // overwrite the pixel with the new color to avoid already assigned color to other classes
								colorIpModel.setColor(augiteColorNew);
								colorIpModel.drawPixel(x,y);
						}else{
							colorIpModel.setColor(pixelColor);
							colorIpModel.drawPixel(x,y);
						}
					}*/
					if(pixelColor.getRed() == iron_oxideColor_bad.getRed() && pixelColor.getGreen() == iron_oxideColor_bad.getGreen() && pixelColor.getBlue() == iron_oxideColor_bad.getBlue()){		
						  // overwrite the pixel with the new color to avoid already assigned color to other classes
							colorIpModel.setColor(iron_oxideColorNew);
							colorIpModel.drawPixel(x,y);	
					}
		
				}

			}
		}
        return mask1;
	}
	
		
	/**
	 * @param args
	 * @throws FormatException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, FormatException {
		
/*		String AnnotMaskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\annotMask\\");
		String ModelMaskFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\darkThreshVolcanics\\");	
	
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotationWIPP2018-10-28\\output\\");

		MaskJoin myClass = new MaskJoin();
		myClass.MaskBooleanJoin(AnnotMaskFileFolder, ModelMaskFileFolder, outFileFolder);
		
*/


		MaskJoin myClass = new MaskJoin();
		// merge 4, 23
		//String mask1_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_23_First_NEW.png");
		//String mask2_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_23_Fe-oxide_Augite.png");	
	
		// merge 4, 31
/*		String mask1_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_31_Fe-oxide_Augite_Volcanic_NEW.png");
		String mask2_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_31_Fe-oxide_Augite_Volcanic.png");			
		String out_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\grayMasks\\Sample_4_31.png");
*/

/*		// fix bad labels for fe_oxide in Steve's annotations
		// Sample__4_33_, Sample__4_34_, Sample__4_36_
		String mask1_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_33_.png");
		String mask2_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\Sample__4_33_.png");			
		String out_filename = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\annotatedFOV\\fix_Sample__4_33_.png");

		myClass.ColorMaskJoinBatch( mask1_filename,  mask2_filename,  out_filename );
*/	

		// this is step 3: to generate masks for the class aggregate = quartzite + feldspar + augite + iron oxide
/*		String mask1_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\binaryReferenceQuartzite");
		String mask2_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\binaryReferenceFeldspar");			
		String out_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\tempOut");*/

/*		String mask1_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\binaryReferenceAugite");
		String mask2_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\binaryReferenceIron_oxide");			
		String out_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\tempOut2");*/

	
 		String mask1_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\tempOut");
		String mask2_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\tempOut2");			
		String out_folder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\April2019\\binaryReferenceAggregate");
		
		myClass.BinaryMaskBooleanJoinBatch(mask1_folder, mask2_folder, out_folder );
			
	
		System.out.println("DONE");
	}

}
