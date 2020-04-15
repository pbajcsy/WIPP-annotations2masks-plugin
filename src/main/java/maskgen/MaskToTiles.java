package maskgen;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import datatype.Annotation;
import datatype.ConcreteMaskColorMap;
import datatype.ConcreteMaskLabelMap;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import io.AnnotationLoader;
import io.CsvMyWriter;
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
 * This class chops images and image masks into smaller tiles to be fed to AI-model training
 * on GPU cards with limited RAM.
 *  
 * @author pnb
 *
 */
public class MaskToTiles {

	private int _numTiles = 0;
	private int _numRawTiles = 0;
	
	private int _tileWidth = 256;
	private int _tileHeight = 256;
	
	private int _tileWidthOverlap = 16;
	private int _tileHeightOverlap = 16;
		
	public MaskToTiles(){
		
	}

	////////////////////////////////////////////
	// setters and getters
	public boolean setTileWidth(int val){
		if(val <= 0 ){
			return false;
		}
		_tileWidth = val;
		return true;		
	}
	public int getTileWidth(){
		return _tileWidth;
	}
	public boolean setTileHeight(int val){
		if(val <= 0 ){
			return false;
		}
		_tileHeight = val;
		return true;		
	}
	public int getTileHeight(){
		return _tileHeight;
	}
	
	public boolean setTileWidthOverlap(int val){
		if(val <= 0 ){
			return false;
		}
		_tileWidthOverlap = val;
		return true;		
	}
	public int getTileWidthOverlap(){
		return _tileWidthOverlap;
	}
	public boolean setTileHeightOverlap(int val){
		if(val <= 0 ){
			return false;
		}
		_tileHeightOverlap = val;
		return true;		
	}
	public int getTileHeightOverlap(){
		return _tileHeightOverlap;
	}
	
	
	
	public int getNumTiles(){
		return _numTiles;
	}
	public int getNumRawTiles(){
		return _numRawTiles;
	}
	
	public boolean setNumTiles(int val){
		_numTiles = val;
		return true;
	}
	public boolean setNumRawTiles(int val){
		_numRawTiles = val;
		return true;
	}
	
	/**
	 * This method takes a pair of raw intensity image and mask image
	 * and tiles the images into image tiles defined by the internal width and height 
	 * variables
	 * 
	 * @param rawImageName - input string for path to a raw file
	 * @param maskImageName - input string for path to a mask file
	 * @param outputFolder - output location for tiled images
	 * 
	 * @return boolean true/false about the succcess of this method
	 * @throws FormatException
	 * @throws IOException
	 */
	public boolean createTiles(String rawImageName, String maskImageName, String outputFolder) throws FormatException, IOException {
		// sanity check
		if(rawImageName == null){
			System.err.println("ERROR: rawImageName is null");
			return false;
		}
/*		if(maskImageName == null){
			System.err.println("ERROR: maskImageName is null");
			return false;
		}*/
		if(outputFolder == null){
			System.err.println("ERROR: outputFolder is null");
			return false;
		}
		//////////////////////
		// load images
		ImagePlus imgPlusRaw = IJ.openImage(rawImageName);
		ImageProcessor ip_raw = imgPlusRaw.getProcessor();
		
		ImagePlus imgPlusMask = null;
		ImageProcessor ip_mask  = null;
		if(maskImageName != null){
			imgPlusMask = IJ.openImage(maskImageName);
			ip_mask = imgPlusMask.getProcessor();
			
			if(ip_raw.getHeight() != ip_mask.getHeight() || ip_raw.getWidth() != ip_mask.getWidth()){
				System.err.println("ERROR: image and mask do not have the same dimensions ");
				return false;			
			}
		}
		///////////////////////
		// prepare output directories
		String outFileName = new String(outputFolder);
		if(!outFileName.endsWith("\\") && !outFileName.endsWith("/") ){
			outFileName += "\\";
		}		
		File directory=new File(outFileName);	
		if(!directory.exists()){
			directory.mkdirs();
			System.out.println("created outputFolder: " + outFileName);
		}
		File rawDirectory=new File(outFileName + "raw\\");	
		if(!rawDirectory.exists()){
			rawDirectory.mkdirs();
			System.out.println("created raw directory: " + outFileName  + "raw\\");
		}	
		if(maskImageName != null){
			File maskDirectory=new File(outFileName + "mask\\");	
			if(!maskDirectory.exists()){
				maskDirectory.mkdirs();
				System.out.println("created mask directory: " + outFileName  + "mask\\");
			}		
		}
		
		
		File rawFile = new File(rawImageName);
		String rootRawName = rawFile.getName();
		int idxDot = rootRawName.lastIndexOf(".");
		rootRawName = rootRawName.substring(0, idxDot); 
		//rootRawName = rootRawName.substring(0, rootRawName.length() - 8); // assumed .ome.tif 
						
		//////////////////
		// setup
		int w = getTileWidth();
		int h = getTileHeight();
		int wo = getTileWidthOverlap();
		int ho = getTileHeightOverlap();
		
		int row, col;		
		Roi temp = null;
						
		FileSaver tiffSave = null;
		int index = 0;
		/////////////////////////////////////////
		// this is an added option to change the naming convention for the tiles 
		// either sequentially numbered or grid like numbered starting with 1,1
		int indexRow = 0;
		int indexCol = 0;
		boolean isSequentialTileNaming = false;
		///////////////////////////
		// this is an added option to avoid saving tiles that have only black pixels
		// and therefore and useless for AI training
		boolean isBlackTileSkipped = true;
		///////////////////////////////
		// this is an added option to save also color png tiles in addition to grayscale tiles
		boolean saveColorMask = true;
		ConcreteMaskColorMap mapping = new ConcreteMaskColorMap();
		FileSaver pngSave = null;
		
		for(col = 0; col < ip_raw.getWidth() - w; col += (w - wo) ){
			indexCol ++;
			indexRow = 0;
			for(row = 0; row < ip_raw.getHeight() - h; row += (h - ho) ){
				indexRow ++;
				temp = new Roi(col,row,w,h);
				///////////////////////////////////////////////////
				//apply tiling to mask images
				if(maskImageName != null){
					ip_mask.setRoi(temp);
					ImageProcessor croppedMask = ip_mask.crop();
					if(isBlackTileSkipped){
						int [] hist = croppedMask.getHistogram();
						boolean signalNonEmpty = true;
						for(int k = 1; signalNonEmpty && k< hist.length; k++){
							if(hist[k] > 0){
								signalNonEmpty = false;
							}
						}
						if(signalNonEmpty){
							System.out.println("Tile (col="+indexCol+", row="+indexRow+") is empty");
							continue;
						}
					}				
					ImagePlus maskCropped = new ImagePlus("Mask Cropped", croppedMask);
	
					tiffSave = new FileSaver(maskCropped);
					if(isSequentialTileNaming){
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_" + index + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_" + index + ".png");			
						}
					}else{
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow +  ".png");			
						}
					}
					
					
				}
				//////////////////////////////////////////////////
				// apply tiling to raw images
				ip_raw.setRoi(temp);
				ImageProcessor croppedRaw = ip_raw.crop();
				if(isBlackTileSkipped){
					int [] hist = croppedRaw.getHistogram();
					boolean signalNonEmpty = true;
					for(int k = 1; signalNonEmpty && k< hist.length; k++){
						if(hist[k] > 0){
							signalNonEmpty = false;
						}
					}
					if(signalNonEmpty){
						System.out.println("Tile (col="+indexCol+", row="+indexRow+") is empty");
						continue;
					}
				}
				ImagePlus rawCropped = new ImagePlus("Raw Cropped", croppedRaw);

				tiffSave = new FileSaver(rawCropped);
				if(isSequentialTileNaming){
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_" + index + ".tif");	
				}else{
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");
				}

				index++;
			}
		}
		
		double rem = Math.abs(Math.IEEEremainder(ip_raw.getWidth(), w));
		System.out.println("raw width = " + ip_raw.getWidth());
		System.out.println("raw height = " + ip_raw.getHeight());
		
		System.out.println("remainder columns = " + rem);
		if(rem > (w>>2) ){
			System.out.println("adding tiles along the last column " );
			
			indexCol++;
			indexRow = 0;
			// create additional tiles along the last column if the remained is more than 1/4 of width
			col = ip_raw.getWidth() - w;
			for(row = 0; row < ip_raw.getHeight() - h; row += (h - ho) ){
				indexRow ++;
				temp = new Roi(col,row,w,h);
				// apply tiling to mask images
				if(maskImageName != null){
					ip_mask.setRoi(temp);
					ImageProcessor croppedMask = ip_mask.crop();
					if(isBlackTileSkipped){
						int [] hist = croppedMask.getHistogram();
						boolean signalNonEmpty = true;
						for(int k = 1; signalNonEmpty && k< hist.length; k++){
							if(hist[k] > 0){
								signalNonEmpty = false;
							}
						}
						if(signalNonEmpty){
							System.out.println("Tile (col="+indexCol+", row="+indexRow+" is empty");
							continue;
						}
					}
					ImagePlus maskCropped = new ImagePlus("Mask Cropped", croppedMask);
					
					tiffSave = new FileSaver(maskCropped);
					if(isSequentialTileNaming){
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_" + index + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_" + index + ".png");			
						}
					}else{
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow +  ".png");			
						}
					}
				}
				
				// apply tiling to raw images
				ip_raw.setRoi(temp);
				ImageProcessor croppedRaw = ip_raw.crop();
				if(isBlackTileSkipped){
					int [] hist = croppedRaw.getHistogram();
					boolean signalNonEmpty = true;
					for(int k = 1; signalNonEmpty && k< hist.length; k++){
						if(hist[k] > 0){
							signalNonEmpty = false;
						}
					}
					if(signalNonEmpty){
						System.out.println("Last Column: Tile (col="+indexCol+", row="+indexRow+") is empty");
						continue;
					}
				}
				ImagePlus rawCropped = new ImagePlus("Raw Cropped", croppedRaw);

				tiffSave = new FileSaver(rawCropped);
				if(isSequentialTileNaming)
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_" + index + ".tif");	
				else
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");

				index++;
			}			
		}
		
		rem = Math.abs(Math.IEEEremainder(ip_raw.getHeight(), h));
		System.out.println("remainder rows = " + rem);
		if(rem > (h>>2) ){
			// create additional tiles along the last row if the remained is more than 1/4 of height
			System.out.println("adding tiles along the last row " );
			indexCol = 0;
			indexRow ++;
			row = ip_raw.getHeight() - h;
			for(col = 0; col < ip_raw.getWidth() - w; col += (w -wo) ){
				indexCol ++;
				temp = new Roi(col,row,w,h);
				// apply tiling to mask images
				if(maskImageName != null){
					ip_mask.setRoi(temp);
					ImageProcessor croppedMask = ip_mask.crop();
					if(isBlackTileSkipped){
						int [] hist = croppedMask.getHistogram();
						boolean signalNonEmpty = true;
						for(int k = 1; signalNonEmpty && k< hist.length; k++){
							if(hist[k] > 0){
								signalNonEmpty = false;
							}
						}
						if(signalNonEmpty){
							System.out.println("Tile (col="+indexCol+", row="+indexRow+" is empty");
							continue;
						}
					}
					ImagePlus maskCropped = new ImagePlus("Mask Cropped", croppedMask);
					
					tiffSave = new FileSaver(maskCropped);
					if(isSequentialTileNaming){
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_" + index + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_" + index + ".png");			
						}
					}else{
						tiffSave.saveAsTiff(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");
						if(saveColorMask){						
							ImagePlus colorRes = MaskFromColorImage.convertMaskImage2Color(maskCropped,  mapping) ;							
							pngSave = new FileSaver(colorRes);
							pngSave.saveAsPng(outFileName + "mask\\" + rootRawName + "_c" + indexCol + "_r" + indexRow +  ".png");			
						}
					}
				}
				// apply tiling to raw images
				ip_raw.setRoi(temp);
				ImageProcessor croppedRaw = ip_raw.crop();
				if(isBlackTileSkipped){
					int [] hist = croppedRaw.getHistogram();
					boolean signalNonEmpty = true;
					for(int k = 1; signalNonEmpty && k< hist.length; k++){
						if(hist[k] > 0){
							signalNonEmpty = false;
						}
					}
					if(signalNonEmpty){
						System.out.println("Last Row: Tile (col="+indexCol+", row="+indexRow+") is empty");
						continue;
					}
				}
				ImagePlus rawCropped = new ImagePlus("Raw Cropped", croppedRaw);

				tiffSave = new FileSaver(rawCropped);
				if(isSequentialTileNaming)
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_" + index + ".tif");	
				else
					tiffSave.saveAsTiff(outFileName + "raw\\" + rootRawName + "_c" + indexCol + "_r" + indexRow + ".tif");

				index++;
			}			
		}
		return true;
	}
	
	public boolean batchCreateTiles(String rawFileFolder, String maskFileFolder, String outFileFolder ) throws IOException, FormatException{
		// sanity check
		if (rawFileFolder == null || outFileFolder == null) {
			System.err.println("Error: null rawFileFolder or outFileFolder ");
			return false;
		}
		/*Check directory if exist*/
		File directory=new File(rawFileFolder);
		if(!directory.exists()){
			System.out.println("Input raw intensity Directory does not exist: " + rawFileFolder);
			return false;
		}
		if(maskFileFolder != null){ 
			directory=new File(maskFileFolder);
			if(!directory.exists()){
				System.out.println("Input mask Directory does not exist: " + maskFileFolder);
				return false;
			}
		}
		directory=new File(outFileFolder);
		if(!directory.exists()){
			System.out.println("output Directory does not exist: " + outFileFolder);
			return false;
		}
		///////////////////////////////////////////////////////////
		// getting JSON files to process
		Collection<String> dirRawFiles = FileOper.readFileDirectory(rawFileFolder);

		// select JSON files with the right suffix
		String suffixRaw = new String(".tif");
		Collection<String> dirSelectRawFiles = FileOper.selectFileType(dirRawFiles,suffixRaw );	
		
		// sort stacks to process
		Collection<String> sortedRawInFolder = FileOper.sort(dirSelectRawFiles,
				FileOper.SORT_ASCENDING);
		///////////////////////////////////////////////////////////////
		// getting images to process
		
		Collection<String> dirMaskFiles = null;
		Collection<String> dirSelectMaskFiles = null;
		Collection<String> sortedMaskInFolder = null;
		String suffixMask = new String(".tif");
		if(maskFileFolder != null){ 
			dirMaskFiles = FileOper.readFileDirectory(maskFileFolder);

			// select images with the right suffix
			dirSelectMaskFiles = FileOper.selectFileType(dirMaskFiles,suffixMask );	
			
			// sort stacks to process
			sortedMaskInFolder = FileOper.sort(dirSelectMaskFiles,	FileOper.SORT_ASCENDING);
		}
		//////////////////////////////////////////////////////	
		String maskFileName = new String();	
		String rawFileName = new String();
		//String outFileName = new String();
				
		boolean foundMatch = false;
		boolean ret = true;
		int idxDot = 0;
		for (Iterator<String> k = sortedRawInFolder.iterator(); k.hasNext();) {
			rawFileName = k.next();
			String nameRaw = (new File(rawFileName)).getName();
			idxDot = nameRaw.lastIndexOf(".");
			//nameRaw = nameRaw.substring(0, nameRaw.length()-8); // TODO check what to remove, this case is for  .ome.tif
			nameRaw = nameRaw.substring(0, idxDot); 
			
			if(sortedMaskInFolder != null){
				// find matching rawFileName
				foundMatch = false;
				for(Iterator<String> r = sortedMaskInFolder.iterator(); !foundMatch && r.hasNext(); ){
					maskFileName = r.next();
					String nameMask = (new File(maskFileName)).getName();
					idxDot = nameMask.lastIndexOf(".");
					//nameMask = nameMask.substring(0, nameMask.length()-4); // TODO check what to remove, this case is for  .tif
					nameMask = nameMask.substring(0, idxDot); 	
					if(nameRaw.equalsIgnoreCase(nameMask)){
						
						foundMatch = true;
					}
				}
				if(!foundMatch){
					System.err.println("ERROR: could not find a matching mask image to the raw file");
					System.err.println("raw file = " + rawFileName + ", mask file = " + maskFileName);
					continue;
				}
				System.out.println("INFO: matching pair: RAW = " +  rawFileName + " Mask = " + maskFileName);
			}else{
				// process only raw files
				maskFileName = null;
			}
			ret = ret & createTiles(rawFileName, maskFileName, outFileFolder);
			
		}


		return true;
	}
	
	/*
	//Copy of createTiles, goes with convertPNGAnnotations and creates raw image tiles of unannotated images
	public boolean createRawTiles(ImageProcessor ip) {
		int x = 0, y = 0, w = 256, h = 256;
		String filePath = "/home/kpb3/Documents/CompleteAnnots/RawTiles/Tile";
		int currX = 0;
		while (y <= ip.getHeight()) {
			x = 0;
			while (x <= ip.getWidth()) {
				currX = x;
				int pixelSum = 0;
				Roi temp = new Roi(x,y,w,h);
				ip.setRoi(temp);
				ImageProcessor croppedIP = ip.crop();
				ImagePlus imp = new ImagePlus("Cropped", croppedIP);
				for (int y1 = 0; y1 <= ip.getHeight(); y1++) {
					for (int x1 = 0; x1 <= ip.getWidth(); x1++) {
						pixelSum += croppedIP.getPixel(x1, y1);
					}
				}
				if (pixelSum > 0) {
					FileSaver tiffCrop = new FileSaver(imp);
					tiffCrop.saveAsTiff(filePath + Integer.toString(getNumRawTiles()));
				//	rawCrop.saveAsTiff(filePathTwo + Integer.toString(numTile));
					setNumRawTiles(getNumRawTiles() + 1);
				}
				int tempX = x + w;
				if (x == ip.getWidth()-256) {
					x += 257;
				} else if (tempX + 256 > ip.getWidth()) {
					x = ip.getWidth() - 256;
				} else {
					x = tempX;
				}
				if (!(x == currX))
					System.out.println("x: " + x + "y: " + y);
			}
			int tempY = y + h;
			if (y == ip.getHeight()-256) {
				y += 257;
			} else if (tempY + 256 > ip.getHeight()) {
				y = ip.getHeight() - 256;
			} else {
				y = tempY;
			}
		}
		System.out.println(ip.getWidth() + " " + ip.getHeight());
		System.out.println("Done 2!");
		return true;
	}
	
	
	public boolean createSegmentTiles(ImageProcessor ip) throws FormatException, IOException {
		int[] counts = new int[5];
		//Initialize image reader and writer
		ImageReader reader = new ImageReader();
		IMetadata meta;

		//Set up essential elements to convert to TIFF
		try {
			ServiceFactory factory = new ServiceFactory();
			OMEXMLService service = factory.getInstance(OMEXMLService.class);
			meta = service.createOMEXMLMetadata();
		}
		catch (DependencyException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}
		catch (ServiceException exc) {
			//Close reader.
			reader.close();
			throw new FormatException("Could not create OME-XML store.", exc);
		}

		reader.setMetadataStore(meta);

		//Read TIFF image for background and declare name and location of final TIFF file
		reader.setId("/home/kpb3/Downloads/Core-M3-Total_StitchedWafer_16BPP.ome(1).tif");
		ImageProcessorSource ip2 = new ImageProcessorSource(reader);
		ImageProcessor ip3 = (ImageProcessor) ip2.getObject(0);
		int x = 0, y = 0, w = 32, h = 32, tileX = 0, tileY = 0, numTile = 1;
		String filePath = "/home/kpb3/Documents/Segment7/Mask/";
		String filePathTwo = "/home/kpb3/Documents/Segment7/Raw/";
		
		//Iterate through image
		for (y = 0; y <= ip.getHeight(); y++) {
			for (x = 0; x <= ip.getWidth(); x++) {
				int pixelVal = (int) ip.getPixelValue(x, y);
				if (pixelVal != 0) {
					tileX = x - 16;
					tileY = y - 16;
					Roi tile = new Roi(tileX, tileY, w, h);
					ip3.setRoi(tile);
					ImageProcessor rawTile = ip3.crop();
					ImagePlus imp = new ImagePlus("tile", rawTile);
					FileSaver raw = new FileSaver(imp);
					Roi labelFill = new Roi(0, 0, rawTile.getWidth(), rawTile.getHeight());
					ImageProcessor labelTile = ip3.crop();
					ImagePlus imp2 = new ImagePlus("tile", labelTile);
					FileSaver label = new FileSaver(imp2);
					labelTile.setColor(new Color(pixelVal, pixelVal, pixelVal));
					labelTile.fill(labelFill);
					
					//If it finds an annotated pixel, crop window around it and save
					switch (pixelVal) {
					case 59:
						if (counts[0] < 10000) {
							raw.saveAsTiff(filePathTwo + "Purple" + Integer.toString(counts[0]));
							label.saveAsTiff(filePath + "Purple" + Integer.toString(counts[0]));
							counts[0]++;
						}
						break;
					case 64:
						if (counts[1] < 10000) {
							raw.saveAsTiff(filePathTwo + "Green" + Integer.toString(counts[1]));
							label.saveAsTiff(filePath + "Green" + Integer.toString(counts[1]));
							counts[1]++;
						}
						break;
					case 106:
						if (counts[2] < 10000) {
							raw.saveAsTiff(filePathTwo + "Blue" + Integer.toString(counts[2]));
							label.saveAsTiff(filePath + "Blue" + Integer.toString(counts[2]));
							counts[2]++;
						}
						break;
//					case 127:
//						if (counts[3] < 5000) {
//							raw.saveAsTiff(filePathTwo + "Orange" + Integer.toString(counts[3]));
//							label.saveAsTiff(filePath + "Orange" + Integer.toString(counts[3]));
//							counts[3]++;
//						}
//						break;
					case 191:
						if (counts[4] < 10000) {
							raw.saveAsTiff(filePathTwo + "Pink" + Integer.toString(counts[4]));
							label.saveAsTiff(filePath + "Pink" + Integer.toString(counts[4]));
							counts[4]++;
						}
						break;
					}
					System.out.println(numTile);
					numTile++;
				}
			}
		}
		for (int i = 0; i < 5; i++) {
			System.out.print(counts[i] + " ");
		}
		return true;
	}
	
*/
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FormatException 
	 */
	public static void main(String[] args) throws FormatException, IOException {
	
		// sanity check
		if(args == null || args.length < 3){
			System.err.println("expected arguments: rawImageName, maskImageName, outFileFolder (for output tiles)");
			return;			
		}

		MaskToTiles myClass = new MaskToTiles();
		myClass.setTileHeight(256);
		myClass.setTileWidth(256);
		myClass.setTileHeightOverlap(16);
		myClass.setTileWidthOverlap(16);		
		
/*		String rawImageName = args[0];
		String maskImageName = args[1];
		String outFileFolder = args[2];	
		System.out.println("args[0] inputJSONFileFolder="+rawImageName);
		System.out.println("args[1] inputRawFileFolder=" + maskImageName);
		System.out.println("args[2] outFileFolder="+outFileFolder);*/
		

		
/*		
		//testing one file
		String rawImageName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\rawFOV\\Sample__3_28_.tif");
		String maskImageName = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedAnnotModel\\Sample__3_28_.tif");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedTiles\\");	
	
		boolean ret = myClass.createTiles(rawImageName, maskImageName, outFileFolder);
	*/
		
/*		
 		// testing batch and converting October annotations
		String rawFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\rawFOV\\");
		String maskFileFolder = null;//new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedAnnotModel\\");
		String outFileFolder = new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedTiles\\");	
*/	
		// converting all Raw files for GAN model 
		String rawFileFolder = new String("Y:\\wippModified\\");
		String maskFileFolder = null;//new String("C:\\PeterB\\Projects\\TestData\\concrete_SteveFeldman\\AssistCreationAnnotations\\October2018\\processedAnnotModel\\");
		String outFileFolder = new String("Y:\\wippModifiedTiled\\");	
		
		boolean ret = myClass.batchCreateTiles(rawFileFolder, maskFileFolder, outFileFolder);
	}

}
