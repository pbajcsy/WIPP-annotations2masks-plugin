/* This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */
package util;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import ij.ImagePlus;
import loci.common.DebugTools;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class BioFormatsUtils {
	
private static final Logger LOGGER = Logger.getLogger(BioFormatsUtils.class.getName()); 
	
	public static ImagePlus readImage(String filepath) {
		DebugTools.enableLogging("WARN");
		ImagePlus imp;

		File file = new File(filepath);
		LOGGER.log(Level.INFO, "Loading " + file.getName() + " using BioFormats");
		
		try {
			ImporterOptions options = new ImporterOptions();
			options.setId(file.getAbsolutePath());
			options.setSplitChannels(false);
			options.setSplitTimepoints(false);
			options.setSplitFocalPlanes(false);
			options.setAutoscale(false);
			options.setVirtual(false);

			ImagePlus[] tmp = BF.openImagePlus(options);
			imp = tmp[0];

		} catch (Exception e) {
			LOGGER.log(Level.INFO, "Cannot open image using BioFormats");
			return null;
		}

		return imp;
	}


}
