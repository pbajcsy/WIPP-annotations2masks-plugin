package util;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import maskgen.ThresholdMaskForAnnotations;

/**
 * This class contains methods for performing math operations on images
 * 
 * @author pnb
 *
 */
public class MathOper {

	/**
	 * This method performs boolean AND using two input images and
	 * outputs the results in the second image
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public static ImagePlus booleanAND(ImagePlus im1, ImagePlus im2){
		// sanity check
		if(im1 == null || im2== null){
			System.err.println("Math:Oper - booleanAND: one of the input images is null");
			return null;
		}
		ImageProcessor ip1 = im1.getProcessor();
		ImageProcessor ip2 = im2.getProcessor();
				
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		
		ImagePlus imRes = new ImagePlus("Result", ip2);
		ImageProcessor ip3 = imRes.getProcessor();
		
		boolean v1, v2;
		int v3;
	   for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	
	        	if (ip1.getPixelValue(x,y) == 0) 
	        		v1 = false;
	        	else
	        		v1 = true;
	        	if (ip2.getPixelValue(x,y) == 0) 
	        		v2 = false;
	        	else
	        		v2= true;

	        	if (v1 && v2) 
	        		v3 = 255;
	        	else
	        		v3 = 0;

/*	        	if(ip1.getPixelValue(x,y) != 0 || ip2.getPixelValue(x,y) != 0 ){
		        	System.out.println("pix1=" + ip1.getPixelValue(x,y));
		        	System.out.println("pix2=" + ip2.getPixelValue(x,y));
		        	
		        	System.out.println("v1=" + v1 + ", v2=" + v2 + ", v3 =" + v3);
	        	}*/
	        	
			     // Write the result
			     ip3.putPixelValue(x, y, v3);
	        }   
	    }
	   //imRes.setImage(imRes);
	   
	   return  imRes;
	}

	/**
	 * This method performs boolean OR using two input images and
	 * outputs the results in the first image
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public static ImagePlus booleanOR(ImagePlus im1, ImagePlus im2){
		//sanity check
		if(im1 == null || im2 == null){
			System.err.println("ERROR: input image(s) should not be null");
			return null;
		}
		ImageProcessor ip1 = im1.getProcessor();
		ImageProcessor ip2 = im2.getProcessor();
				
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		if(width != ip2.getWidth() || height != ip2.getHeight()){
			System.err.println("ERROR: input images do not have the same size");
			return null;			
		}	
		
		ImagePlus imRes = new ImagePlus("Result", ip2);
		ImageProcessor ip3 = imRes.getProcessor();
		
		boolean v1, v2;
		int v3;
	   for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	
	        	if (ip1.getPixelValue(x,y) == 0) 
	        		v1 = false;
	        	else
	        		v1 = true;
	        	if (ip2.getPixelValue(x,y) == 0) 
	        		v2 = false;
	        	else
	        		v2= true;

	        	if (v1 || v2) 
	        		v3 = 255;
	        	else
	        		v3 = 0;

/*	        	if(ip1.getPixelValue(x,y) != 0 || ip2.getPixelValue(x,y) != 0 ){
		        	System.out.println("pix1=" + ip1.getPixelValue(x,y));
		        	System.out.println("pix2=" + ip2.getPixelValue(x,y));
		        	
		        	System.out.println("v1=" + v1 + ", v2=" + v2 + ", v3 =" + v3);
	        	}*/
	        	
			     // Write the result
			     ip3.putPixelValue(x, y, v3);
	        }   
	    }
	   imRes.setImage(imRes);
	   
	   return  imRes;
	}

	/**
	 * This method subtracts im2 from im1 and returns the result
	 * 
	 * Minuend − Subtrahend = Difference. 
	 * Minuend: The number that is to be subtracted from.
	 * Subtrahend: The number that is to be subtracted.
	 * Difference: The result of subtracting one number from another

	 * @param im1 - Minuend
	 * @param im2 - Subtrahend
	 * @return - difference as a Float image type
	 */
	public static ImagePlus subtract(ImagePlus im1, ImagePlus im2){
		//sanity check
		if(im1 == null || im2 == null){
			System.err.println("ERROR: input image(s) should not be null");
			return null;
		}
		ImageProcessor ip1 = im1.getProcessor();
		ImageProcessor ip2 = im2.getProcessor();
				
		int width = ip1.getWidth();
		int height = ip1.getHeight();
		if(width != ip2.getWidth() || height != ip2.getHeight()){
			System.err.println("ERROR: input images do not have the same size");
			return null;			
		}	
		
		FloatProcessor ip_float = new FloatProcessor(width, height);
		ImagePlus imRes = new ImagePlus("Result", ip_float);
		ip_float.set(0);
		
		float v3;
		for (int x=0; x<width; x++) {
	        for (int y=0; y<height; y++) {
	        	v3 = ip1.getPixelValue(x,y) - ip2.getPixelValue(x,y);
	        	ip_float.putPixelValue(x, y, v3);
	        }   
	    }
	   imRes.setImage(imRes);
	   
	   return  imRes;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// step 1: threshold the image
		String imageName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\rawFOV\\Sample_(4,23).tif");
		String binImageOutputName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\testOutput\\andTest.tif");	
	
		ImagePlus imgPlus = IJ.openImage(imageName);
		ImageProcessor ip = imgPlus.getProcessor();
		int nBins = 256;
        ip.setHistogramSize(nBins);
        ip.setHistogramRange(0.0, 0.0);
        
        int threshValue = 55;
		ij.process.ImageStatistics stat = ImageStatistics.getStatistics(ip, threshValue, null);
		System.out.println( "stats: " + stat.toString());
		
		long [] hist = stat.getHistogram();
		for(int k = 0;k< hist.length;k++){
			System.out.print("h["+k+"]="+hist[k]+", ");
		}
		System.out.println( );

		int mode = stat.mode;
		System.out.print("mmode="+mode);
		
/*		ImagePlus thrImage = ThresholdMaskForAnnotations.binarizeImage(imgPlus, mode-5, true);

		binImageOutputName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\testOutput\\threshAboveTest.tif");	
		FileSaver fs = new FileSaver(thrImage);
		fs.saveAsTiff(binImageOutputName);
		
		
		ImagePlus thrImage2 = ThresholdMaskForAnnotations.binarizeImage(imgPlus, mode+5, true);

		binImageOutputName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\testOutput\\threshBelowTest.tif");	
		fs = new FileSaver(thrImage2);
		fs.saveAsTiff(binImageOutputName);*/
		
		ImagePlus res = ThresholdMaskForAnnotations.binarizeImageWithinRange(imgPlus, mode-5, mode+5, true);
		
		//ImagePlus res = MathOper.booleanAND(thrImage,  thrImage2);
		
		binImageOutputName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\TrainingImageMasks\\AnnotatedImages2018-07-19\\testOutput\\andTest.tif");	
		FileSaver fs = new FileSaver(res);
		fs.saveAsTiff(binImageOutputName);

	}

}
