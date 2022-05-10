/*
 * Copyright (c) 2015-2022 Onder Babur
 * 
 * This file is part of SAMOS Model Analytics and Management Framework.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this 
 * software and associated documentation files (the "Software"), to deal in the Software 
 * without restriction, including without limitation the rights to use, copy, modify, 
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 *  or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * @author Onder Babur
 * @version 1.0
 */

package nl.tue.set.samos.stats;

import java.io.File;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAnalyzer {
	
	final Logger logger = LoggerFactory.getLogger(RAnalyzer.class);
	
	public Rengine re = null;
	
	public RAnalyzer(){
	}
	
	
	public void finalize(){
		this.re.end();
	}
	
	public boolean prepareR(){
		String args_[] = {"--no-save"};
		if (!Rengine.versionCheck()) {
			logger.error("** Version mismatch - Java files don't match library version.");
			return false;
		}
		logger.debug("Creating Rengine (with arguments)");
		// 1) we pass the arguments from the command line
		// 2) we won't use the main loop at first, we'll start it later
		//    (that's the "false" as second argument)
		// 3) the callbacks are implemented by the TextConsole class above
		re=new Rengine(args_, false, new TextConsole());
		logger.info("Rengine created, waiting for R");
		// the engine creates R is a new thread, so we should wait until it's ready
		if (!re.waitForR()) {
			logger.error("Cannot load R");
			return false;
		}

		
		@SuppressWarnings("unused")
		REXP rexp;
		
		rexp = re.eval("if (!require(\"coop\")) install.packages(\"coop\")");
//		System.out.println(rexp);	
//		String pathForVeganPackage = System.getProperty("user.dir") + "/lib/vegan_2.4-3.tar.gz";
//		rexp = re.eval("if (!require(\"vegan\")) install.packages(\"vegan\")");	
//		rexp = re.eval("remove.packages(\"vegan\")");
//		rexp = re.eval("if (!require(\"vegan\")) install.packages(\"" + pathForVeganPackage +  "\", repos = NULL)");
		
		// TODO need to do this manually it seems
		
//		System.out.println(rexp);
		rexp = re.eval("if (!require(\"bigmemory\")) install.packages(\"bigmemory\")");
//		System.out.println(rexp);
		rexp = re.eval("if (!require(\"Matrix\")) install.packages(\"Matrix\")");
//		System.out.println(rexp);
		rexp = re.eval("if (!require(\"dbscan\")) install.packages(\"dbscan\")");
//		System.out.println(rexp);
		rexp = re.eval("if (!require(\"stringr\")) install.packages(\"stringr\")");
//		System.out.println(rexp);		
		
		rexp = re.eval("library(\"vegan\")");	
		
		
		//logger.debug(System.getProperty("user.dir"));
		String s = System.getProperty("user.dir").replaceAll("\\\\", "/");
		rexp = re.eval("source(\"" + s + "/src/nl/tue/set/samos/stats/Clustering.R\")");
		rexp = re.eval("source(\"" + s + "/src/nl/tue/set/samos/stats/CloneDetection.R\")");
		
		return true;
	}
	
	public void cluster(String vsmFile, String nameFile, String outputFolder) {
		String command = "cluster(\"" + new File(vsmFile).getAbsolutePath() + "\",\"" + new File(nameFile).getAbsolutePath() + "\",\"" + 
	new File(outputFolder).getAbsolutePath() + "\")";
		command = command.replaceAll("\\\\", "/");
		logger.info(command);
		REXP rexp = re.eval(command);
		logger.info("Finished clustering, final R output: "+rexp);
		logger.info("Check results folder for the cluster labels");
	}
	
	public void detectClones(String vsmFile, String vsmMaskFile, String nameFile, String sizeFile, String outputFolder) {
		String command = "detectClones(\"" + 
				new File(vsmFile).getAbsolutePath() + "\",\"" 
				+ new File(vsmMaskFile).getAbsolutePath() + "\",\""
				+ new File(nameFile).getAbsolutePath() + "\",\""  
				+ new File(sizeFile).getAbsolutePath() + "\",\"" 
				+ new File(outputFolder).getAbsolutePath() + "\")";
		command = command.replaceAll("\\\\", "/");
		logger.info(command);
		REXP rexp = re.eval(command);
		logger.info("Finished clone detection, final R output: "+rexp);
		logger.info("Check results folder for the clone labels");
	}
	
}
