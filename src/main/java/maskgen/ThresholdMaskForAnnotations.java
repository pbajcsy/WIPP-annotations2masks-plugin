/**
 * 
 */
package maskgen;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.util.ImageProcessorSource;
import util.FileOper;

/**
 * This method is for creating a thresholded mask 
 * The thresholded mask pre-classified pixels as background (black) and foreground (white)
 * In the context of the concrete project, the background corresponds to 
 * cracks + dissolution + air voids
 * 
 * The selection of a threshold value is optimized based on annotated images by Steve Feldman
 * 
 * @author pnb
 *
 */
public class ThresholdMaskForAnnotations {

	///////////////////////////////
	/**
	 * This method will generated a thresholded binary image
	 * 
	 * @param thresh
	 * @param imageFolder
	 * @param binImageOutputFolder
	 * @return
	 */
	public static boolean binarizeFolderWithImages(int thresh,  boolean FRGaboveThresh, String imageFolder, String binImageOutputFolder) {

		// identify all tiles in a manual mask folder
		Collection<String> dirfiles = FileOper.readFileDirectory(imageFolder);
		System.out.println("Directory Collection Size=" + dirfiles.size());
		Collection<String> maskImg = FileOper.selectFileType(dirfiles, ".tif");

		System.out.println("filtered Collection Size=" + maskImg.size());
		FileOper.printCollection(maskImg);

		// loop over all tiles
		int row, col;
		ImagePlus imgPlus = null;

		Color black = new Color(0, 0, 0);
		Color white = new Color(255, 255, 255);
		Color red = new Color(255, 0, 0);
		int bkg = 0;
		int frg = ((white.getRed() << 16) + (white.getGreen() << 8) + white.getBlue());

		for (Iterator<String> k = maskImg.iterator(); k.hasNext();) {
			String fileName = k.next();

			imgPlus = IJ.openImage(fileName);
			imgPlus = ThresholdMaskForAnnotations.binarizeImage(imgPlus, thresh, FRGaboveThresh);
			/*
			imgPlus = IJ.openImage(fileName);

			ImageConverter myConvert = new ImageConverter(imgPlus);
			myConvert.convertToGray8();

			ImageProcessor imgProcessor = imgPlus.getProcessor();
			// below thresh is set to 0
			// above thresh is set to 255
			imgProcessor.threshold(thresh);
			*/

			File outName = new File(fileName);
			String outFileNameNoPath = outName.getName().substring(0, (outName.getName().length() - 4));
			String outFileName = binImageOutputFolder + File.separator + outFileNameNoPath + ".tif";
			// FileSaver fs = new FileSaver(new ImagePlus("test",
			// bufferedImage));
			if (createDirectory(new File(binImageOutputFolder))) {
				FileSaver fs = new FileSaver(imgPlus);
				fs.saveAsTiff(outFileName);
				// fs.saveAsPng(outFileName);
			}
		}

		return true;
	}

	public static ImagePlus binarizeImage(ImagePlus imgPlus, int thresh, boolean FRGaboveThresh){
		

		ImageConverter myConvert = new ImageConverter(imgPlus);
		myConvert.convertToGray8();

		ImageProcessor imgProcessor = imgPlus.getProcessor();
		// below thresh is set to 0
		// above thresh is set to 255
		imgProcessor.threshold(thresh);
		if(!FRGaboveThresh){
			// below thresh is set to 255
			// above thresh is set to 0		
			imgProcessor.invert();
		}
		return imgPlus;
	}
	
	/**
	 * This method will create a binary image with FRG=255 assigned to pixels
	 * between the lower and upper thresh values (including the values) if FRGaboveThresh = true
	 * If FRGaboveThresh = false then it it will invert the assignment between FRG and BKG.
	 *  
	 * @param imgPlus - input image
	 * @param thresh_lower -- lower thresh val
	 * @param thresh_upper - upper thresh val
	 * @param FRGaboveThresh - boolean flag defining whether FRG is between or outside the thresh values 
	 * @return ---resulting ImagePlus
	 */
	public static ImagePlus binarizeImageWithinRange(ImagePlus imgPlus, int thresh_lower, int thresh_upper, boolean FRGaboveThresh){
		//sanity check
		if(imgPlus == null){
			System.err.println("ERROR: input imgPlus is null");
			return null;
		}
		// prepare the resulting images
		ByteProcessor ip_byte = new ByteProcessor(imgPlus.getWidth(), imgPlus.getHeight());
		ImagePlus res = new ImagePlus("binaryRange", ip_byte);
		ip_byte.set(0);
		byte[] pix = (byte[]) ip_byte.getPixels();
		
/*		ImageConverter myConvert = new ImageConverter(imgPlus);
		myConvert.convertToGray8();
		ImageProcessor imgProcessor = imgPlus.getProcessor().convertToByte(false);
*/		
		// prepare the input image
		ImageProcessor ip = imgPlus.getProcessor();
		final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;
		int type;   
	    if (ip instanceof ByteProcessor)
            type = BYTE;
        else if (ip instanceof ShortProcessor)
            type = SHORT;
        else if (ip instanceof FloatProcessor)
            type = FLOAT;
        else
            type = RGB;
	    
	    if(type == RGB){
	    	System.err.println("ERROR: no support for RGB images to be thresholded within a range");
	    	return null;
	    }
        
        float val = 0;
		// inside thresh is set to 255
		// outside thresh is set to 0
        for(int k = 0; k < pix.length; k++){
        	switch(type){
        	case BYTE:
        		val  = ip.get(k) & 0xFF;
        		break;
        	case SHORT:
        		val  = ip.get(k) & 0xFFFF;
        		break;
        	case FLOAT:
        		val  = ip.getf(k);
        		break;
        	default:
        		System.err.println("ERROR: type other than BYTE, SHORT, and FLOAT is not supported: type = " + type);
        		break;
        	}
        	//val = ip.get(k);
        	//val  = pix[k] & 0xFF;
        	/*if( (int)(pix[k]) < 0) 
        		val = val+Byte.MAX_VALUE;
        	else
        		val = (int)pix[k];*/
        	
        	if(val>=thresh_lower && val <= thresh_upper){
        		pix[k] = (byte)255;
        	}else{
        		pix[k] = 0;
        	}
        }



		if(!FRGaboveThresh){
			// inside thresh is set to 0
			// outside thresh is set to 255		
			ip_byte.invert();
		}
		return res;
	}
	static boolean createDirectory(File dir) {

		if (dir.exists()) {
			System.out.println("directory exists");
			return true;
		}
		// attempt to create the directory here
		boolean successful = dir.mkdir();
		if (successful) {
			// creating the directory succeeded
			System.out.println("directory was created successfully");
		} else {
			// creating the directory failed
			System.err.println("failed trying to create the directory");
			return false;
		}
		return true;
	}

	///////////////////////////////////////////////////////////
	// This method is thresholding using the bioimage format library loader
	// Testing threshold values
	public static void thresholdImage(String inputRawImageFileName, int threshValue, String outputFileName)
			throws FormatException, IOException {
		// Initialize image reader and writer
		ImageReader reader = new ImageReader();
		IMetadata meta;

		// Set up essential elements to convert to TIFF
		try {
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			meta = service.createOMEXMLMetadata();
		} catch (DependencyException exc) {
			// Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		} catch (ServiceException exc) {
			// Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}

		reader.setMetadataStore(meta);

		// Read TIFF image for background and declare name and location of final
		// TIFF file
		reader.setId(inputRawImageFileName);
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);
		ip3.threshold(threshValue);
		ImagePlus imp = new ImagePlus("Temp", ip3);
		FileSaver cropSave = new FileSaver(imp);
		cropSave.saveAsTiff(outputFileName);
		System.out.println("Done!");
		reader.close();
	}

	
	/**
	 * @param args
	 * @throws IOException
	 * @throws FormatException
	 */
	public static void main(String[] args) throws FormatException, IOException {

		// sanity check
		if (args == null || args.length < 2) {
			System.err.println(
					"expected two arguments: inputGrayImageFileName, unannotatedPath and output Mask Filename");
			return;
		}

		String inputRawImageFileName = args[0];
		String outputFileName = args[1];
		// String outFileFolder = args[2];
		System.out.println("args[0] inputRawImageFileName=" + inputRawImageFileName);
		System.out.println("args[1] outputFileName=" + outputFileName);
		// System.out.println("args[2] outFileFolder="+outFileFolder);

		int threshValue = 58;
		boolean FRGaboveThresh = false;
		//ThresholdMaskForAnnotations.thresholdImage(inputRawImageFileName, threshValue, outputFileName);
		
/*		String imageFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\rawFOV");
		String binImageOutputFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\testOutput");		
		boolean ret = ThresholdMaskForAnnotations.binarizeFolderWithImages( threshValue, FRGaboveThresh, imageFolder,  binImageOutputFolder);*/
		

		
		
	}

}
