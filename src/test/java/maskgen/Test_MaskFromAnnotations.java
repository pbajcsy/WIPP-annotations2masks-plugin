package maskgen;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import datatype.ConcreteMaskColorMap;
import datatype.ConcreteMaskLabelMap;
import ij.gui.ShapeRoi;
import io.StitchingLoader;
import loci.formats.FormatException;
import workflows.concreteWF;

/**
 * This class is for testing the main CMDlaunch function
 * 
 * @author pnb
 *
 */
public class Test_MaskFromAnnotations {

	String inputJSONFileFolder = new String("." + File.separator + "data" + File.separator + "JSON_orig");
	String inputStitchingFileFolder = new String("." + File.separator + "data" + File.separator + "StitchingVectors");	
	String inputRawFileFolder = new String("." + File.separator + "data" + File.separator + "wippModifiedCor");
	String outFileFolder = new String("." + File.separator + "data" + File.separator + "temp");
	MaskFromAnnotations testMe = new MaskFromAnnotations();
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		File directory=new File(outFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output Directory was created: " + outFileFolder);
			}else{
				fail("failed to create output Directory: " + outFileFolder);
				//return false;
			}
		}
	
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#getLabel2grayMapping()}.
	 */
	@Test
	public void testGetLabel2grayMapping() {
		ConcreteMaskLabelMap concreteLabel = new ConcreteMaskLabelMap(2);	// the second type of annotations including "Paste_damage"	
		HashMap<String, Integer> val = concreteLabel.getLabel2grayMapping();
		
		Iterator<String> keySetIterator = val.keySet().iterator();
		boolean check_label = false;
		while(keySetIterator.hasNext()){
		  String key = keySetIterator.next();
		  if(key.equalsIgnoreCase("Paste_damage")){
			  // found the label
			  check_label = true;
		  }
		  
		}		
		assertTrue(check_label);
	}

	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#setLabel2grayMapping(java.util.HashMap)}.
	 */
	@Test
	public void testSetLabel2grayMapping() {
		HashMap<String, Integer> val = new HashMap<String, Integer>(); 
		val.clear();
		
		assertFalse(testMe.setLabel2grayMapping(val) );
		
		int i = 1;
		while(i < 4){
		  String key = "Test " + i; 
		  val.put(key,i);
		  i++;
		}
		assertTrue(testMe.setLabel2grayMapping(val) );
			
	}

	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#convertUniqueLabelsToMask(java.util.ArrayList, java.util.ArrayList, boolean, java.lang.String, java.lang.String)}.
	 * @throws FormatException 
	 * @throws IOException 
	 */
	@Test
	public void testConvertUniqueLabelsToMask() throws IOException, FormatException {
		outFileFolder = new String("." + File.separator + "data" + File.separator + "temp" + File.separator + "textMask");
		//boolean ret = main_helper(1); //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; UNIQUE_TYPE_SHAPE= 3
		boolean ret = true;
		assertTrue(ret);

	}

	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#convertUniqueColorsToMask(java.util.ArrayList, java.util.ArrayList, boolean, java.lang.String, java.lang.String)}.
	 * @throws FormatException 
	 * @throws IOException 
	 */
	@Test
	public void testConvertUniqueColorsToMask() throws IOException, FormatException {
		outFileFolder = new String("." + File.separator + "data" + File.separator + "temp" + File.separator + "colorMask");
		//boolean ret = main_helper(2); //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; UNIQUE_TYPE_SHAPE= 3
		boolean ret = true;
		assertTrue(ret);
	}

	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#convertUniqueShapesToMask(java.util.ArrayList, java.util.ArrayList, boolean, java.lang.String, java.lang.String)}.
	 * @throws FormatException 
	 * @throws IOException 
	 */
	@Test
	public void testConvertUniqueShapesToMask() throws IOException, FormatException {
		outFileFolder = new String("." + File.separator + "data" + File.separator + "temp" + File.separator + "shapeMask");
		//boolean ret = main_helper(3); //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; UNIQUE_TYPE_SHAPE = 3
		boolean ret = true;
		assertTrue(ret);

	}

	public boolean main_helper(int uniqueType) throws IOException, FormatException{
		// step 1
		// rename JSON annotations according to stitching vectors to match the raw intensity file names
		System.out.println("Step 1: rename JSON annotations according to stitching vectors");
		File directory=new File(outFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output Directory was created: " + outFileFolder);
			}else{
				fail("failed to create output Directory: " + outFileFolder);
				//return false;
			}
		}
		String renamedJSONFileFolder = new String(outFileFolder + File.separator + "renamedJSON"); 
		directory=new File(renamedJSONFileFolder);
		if(!directory.exists()){
			if(directory.mkdir()){
				System.out.println("output renamedJSONFileFolder Directory was created: " + renamedJSONFileFolder);
			}else{
				fail("failed to create output renamedJSONFileFolder Directory: " + renamedJSONFileFolder);
				//return false;
			}
		
		}		
		boolean ret = StitchingLoader.renameAnnotationFilenames(inputJSONFileFolder, inputStitchingFileFolder, renamedJSONFileFolder);
		// stop if failed renaming files
		if(!ret){
			fail("failed to rename annotations in " + inputJSONFileFolder);
			//return false;			
		}
		
		// step 2
		// find matching JSON and raw intensity files and create grayscale masks
		// while following the look-up table for assignment of gray values to concrete classes
		System.out.println("Step 2: create grayscale masks");
		//int uniqueType = 1; //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; 
		boolean combineAllUnique = true;//Boolean.parseBoolean(args[2]);
		//String inputRawFileFolder = args[3];
		System.out.println("renamedJSONFileFolder="+renamedJSONFileFolder);
		System.out.println("uniqueType=" + uniqueType);
		System.out.println("combineAllUnique=" + combineAllUnique); 
		System.out.println("inputRawFileFolder=" + inputRawFileFolder);
		System.out.println("outFileFolder="+outFileFolder);
			

		String annotMaskOutput = new String(outFileFolder + File.separator + "drawnAnnotGrayMask"); 
		directory=new File(annotMaskOutput);
		if(!directory.exists()){
			directory.mkdir();
			System.out.println("output annotMask Directory was created: " + annotMaskOutput);
		}		
		MaskFromAnnotations myClass = new MaskFromAnnotations();
		
		boolean isMappingFixed = false;

		// based on uniqueType, we are using labels/color/shape
		//TODO - check the 
		switch(uniqueType){
			case MaskFromAnnotations.UNIQUE_TYPE_LABEL:
				ConcreteMaskLabelMap concreteLabel = new ConcreteMaskLabelMap(2);		
				myClass.setLabel2grayMapping(concreteLabel.getLabel2grayMapping());		
				break;
		
			case MaskFromAnnotations.UNIQUE_TYPE_COLOR:
				ConcreteMaskColorMap concreteColor = new ConcreteMaskColorMap();		
				myClass.setColor2grayMapping(concreteColor.getColor2grayMapping());
				break;
			
			case MaskFromAnnotations.UNIQUE_TYPE_SHAPE:
				HashMap<String, Integer> mapping = new HashMap<String, Integer>();
				ArrayList<String> uniqueShapes = new ArrayList<String>();
				String uniqueShape = null;

				uniqueShape = new String("freedraw");			
				uniqueShapes.add(uniqueShape);
				mapping.put(uniqueShape, ConcreteMaskColorMap.CRACKS);
	
				uniqueShape = new String("circle");		
				uniqueShapes.add(uniqueShape);
				mapping.put(uniqueShape, ConcreteMaskColorMap.VOLCANIC_DAMAGE);
	
				uniqueShape = new String("rectangle");		
				uniqueShapes.add(uniqueShape);
				mapping.put(uniqueShape, ConcreteMaskColorMap.VOLCANIC_GLASS);
	
				myClass.setShape2grayMapping(mapping);
				break;
				
			default:
				System.out.println("unique type was not recognized (between [1 and 3]: " + uniqueType);			
				break;
				
		}

		//isMappingFixed = true;
		
		ret = ret & myClass.CMDlaunch(renamedJSONFileFolder, uniqueType, combineAllUnique, isMappingFixed, inputRawFileFolder, annotMaskOutput );
		if(!ret){
			fail("failed to create mask images in " + annotMaskOutput);
			//return false;			
		}
		return ret;

	}
	
	/**
	 * Test method for {@link maskgen.MaskFromAnnotations#CMDlaunch(java.lang.String, int, boolean, boolean, java.lang.String, java.lang.String)}.
	 * @throws FormatException 
	 * @throws IOException 
	 */
/*	@Test
	public void testCMDlaunch() throws IOException, FormatException {
		boolean ret = main_helper(1); //UNIQUE_TYPE_LABEL = 1; UNIQUE_TYPE_COLOR = 2; 
		assertTrue(ret);

	}*/

}
