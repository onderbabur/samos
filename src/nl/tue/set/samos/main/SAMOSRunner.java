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

package nl.tue.set.samos.main;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.common.Configuration;
import nl.tue.set.samos.common.Constants;
import nl.tue.set.samos.common.FileUtil;
import nl.tue.set.samos.common.Parameters;
import nl.tue.set.samos.common.Util;
import nl.tue.set.samos.common.enums.CTX_MATCH;
import nl.tue.set.samos.common.enums.FREQ;
import nl.tue.set.samos.common.enums.GOAL;
import nl.tue.set.samos.common.enums.IDF;
import nl.tue.set.samos.common.enums.NGRAM_CMP;
import nl.tue.set.samos.common.enums.SCOPE;
import nl.tue.set.samos.common.enums.SERIALIZATION;
import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.SYNONYM;
import nl.tue.set.samos.common.enums.SYNONYM_TRESHOLD;
import nl.tue.set.samos.common.enums.TYPE_MATCH;
import nl.tue.set.samos.common.enums.UNIT;
import nl.tue.set.samos.common.enums.VSM_MODE;
import nl.tue.set.samos.common.enums.WEIGHT;
import nl.tue.set.samos.crawl.Crawler;
import nl.tue.set.samos.extract.EcoreExtractorImpl;
import nl.tue.set.samos.extract.IExtractor;
import nl.tue.set.samos.feature.NTreeApted;
import nl.tue.set.samos.feature.parser.JSONParser;
import nl.tue.set.samos.nlp.NLP;
import nl.tue.set.samos.stats.RAnalyzer;
import nl.tue.set.samos.vsm.VSMBuilder;


public class SAMOSRunner {
	
    private static Logger logger = LoggerFactory.getLogger(SAMOSRunner.class);

	public static void main(String[] args) {
		
		if (args.length == 2 && args[0].equals("--crawl") ) {
			try {
				Crawler.crawl(args[1]);
			} catch (Exception e) {
				logger.error("error in crawling: " + e.getMessage());
			}
			return;
		}
		else if (args.length == 2 && (args[0].equals("--clone") || args[0].equals("--cluster")))  {
			// ok, continue below
		}
		else {
			logger.error("Need two arguments for crawling (--crawl targetfolder) or running samos (--cluster targetfolder or --clone targetfolder)");
			return;
		}
		
		SAMOSRunner samos = new SAMOSRunner(args);
		
		// TODO check redundant configurations and do not run SAMOS if so. 
//		if (samos.isRedundantConfiguration(new Parameters(_UNIT, _STRUCTURE)))
//				return;
		
		try {
			if (args[0].substring(2).equalsIgnoreCase(GOAL.CLUSTER.toString())) {
				// standard settings for clustering with UNIGRAM-NAME combination for the model scope
				final SCOPE _SCOPE = SCOPE.MODEL; 
				final UNIT _UNIT = UNIT.NAME; 
				final STRUCTURE _STRUCTURE = STRUCTURE.UNIGRAM; 
				
				samos.PREPROCESS_TOKENIZE = true;
				samos.PREPROCESS_LEMMATIZE = true;
				
				samos.MIN_MODEL_ELEMENT_COUNT_PER_FRAGMENT = 1;
				
				logger.info("Starting SAMOS with goal " + samos.configuration._GOAL + " " + "and parameters " + _SCOPE + "-" + _UNIT  + "-" + _STRUCTURE);
				samos.extractFeatures(_SCOPE, _UNIT, _STRUCTURE);
				samos.buildVSMForClustering(_UNIT, _STRUCTURE);
				samos.clusterInR();
			}
			else if (args[0].substring(2).equalsIgnoreCase(GOAL.CLONE.toString())) {
				// standard settings for clustering with ATTRIBUTED-NTREE combination for the EClass scope
				final SCOPE _SCOPE = SCOPE.ECLASS; 
				final UNIT _UNIT = UNIT.ATTRIBUTED; 
				final STRUCTURE _STRUCTURE = STRUCTURE.NTREE; 
				
				samos.PREPROCESS_TOKENIZE = false;
				samos.PREPROCESS_LEMMATIZE = true;
				
				samos.MIN_MODEL_ELEMENT_COUNT_PER_FRAGMENT = 5;
			
				logger.info("Starting SAMOS with goal " + samos.configuration._GOAL + " " + "and parameters " + _SCOPE + "-" + _UNIT  + "-" + _STRUCTURE);
				samos.extractFeatures(_SCOPE, _UNIT, _STRUCTURE);
				samos.buildVSMForCloneDetection(_UNIT, _STRUCTURE);
				samos.detectClonesInR();
			}
			else
				logger.error("Unknown goal, not computing VSM or any analyses!");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	public HashMap<String, ArrayList<String>> featureMap;
	public VSMBuilder vsmBuilder;
	public RAnalyzer r;
	public String targetExtension = ".ecore";
//	public String modelFolder, dataFolder, featureFolder, vsmFolder, rFolder; // go into configuration	
//	public GOAL _GOAL; // go into configuration
	public Configuration configuration;
	public boolean PREPROCESS_TOKENIZE;
	public boolean PREPROCESS_LEMMATIZE;
	public int MIN_MODEL_ELEMENT_COUNT_PER_FRAGMENT;

	
	public SAMOSRunner(String[] args) {
		loadConfiguration(args);
		r = new RAnalyzer();
		vsmBuilder = new VSMBuilder(configuration);
		featureMap = null;
	}
	
	// CONFIG
	private void loadConfiguration(String[] args){
		
//		File file = new File(Constants.configFile);
//		Properties properties = new Properties();
		
		try{
//			FileInputStream fileInput = new FileInputStream(file);
//			properties.load(fileInput);
//			fileInput.close();
			
			configuration = new Configuration();
			configuration.modelFolder = args[1];
			if (configuration.modelFolder.endsWith("/")) configuration.modelFolder = configuration.modelFolder.substring(0, configuration.modelFolder.length()-1);
			configuration.dataFolder = "data/" + configuration.modelFolder;
			
			configuration.featureFolder = "results/features/" + configuration.modelFolder;
			configuration.vsmFolder = "results/vsm/" + configuration.modelFolder + "/";		
			
			configuration.rFolder = "results/r/" + configuration.modelFolder + "/";
			
			configuration._GOAL = GOAL.valueOf(args[0].substring(2).toUpperCase());
			
		} catch(Exception ex) {ex.printStackTrace();}
				
	}
	// CONFIG END
	
	// EXTRACTION
	// NOTE: stack overflow with default stack size. increasing -Xss16m also doesn't help for some large (cyclic?) files
	public static UNIT unitList[] = UNIT.values(); 
	public static STRUCTURE structureList[] = STRUCTURE.values();
	
	public void extractFeatures(SCOPE _SCOPE, UNIT _UNIT, STRUCTURE _STRUCTURE) {
		File sourceFolder = new File(configuration.dataFolder);
		if (!sourceFolder.exists()) {
			logger.error("Folder " + sourceFolder.getAbsolutePath() + " not found!!!!");
			return;
		}
		File[] fs = sourceFolder.listFiles(new FilenameFilter() {
			public boolean accept(File arg0, String name) {
				return (!name.contains("DS_Store") && name.toLowerCase().endsWith(targetExtension));}});

		File targetFolder = new File(configuration.featureFolder);
		try {
			FileUtils.deleteDirectory(targetFolder);
		} catch (IOException e) {}
		targetFolder.mkdirs();
		
		IExtractor extractor = new EcoreExtractorImpl();		
		extractor.PREPROCESS_TOKENIZE = this.PREPROCESS_TOKENIZE;
		extractor.PREPROCESS_LEMMATIZE = this.PREPROCESS_LEMMATIZE;
//		extractor.MIN_FEATURE_COUNT_PER_FRAGMENT = this.MIN_FEATURE_COUNT_PER_FRAGMENT;
		
		int minSizeToOutput = this.MIN_MODEL_ELEMENT_COUNT_PER_FRAGMENT;
		
		logger.info("starting feature extraction");
		long start = System.currentTimeMillis();
		for (File f: fs) {
			logger.info("processing file:" + f.getName());
			featureMap = extractor.process(f, _SCOPE, _UNIT, _STRUCTURE);
			featureMap.keySet().forEach(key -> 
			printNgrams(key, targetFolder.getAbsolutePath() + "/" + key 
					+ Constants.featureFileSuffix, minSizeToOutput, _STRUCTURE)); 


		}				
		logger.info("elapsed time:" + (System.currentTimeMillis() - start));
	}
		
	public void printNgrams(String key, String ngramFilePath, int minSize, STRUCTURE _STRUCTURE){
		File f = new File(ngramFilePath);
		FileWriter fout;
		ArrayList<String> features = featureMap.get(key);
		
		int size = 0, featureCount = 0, finalSize = 0;
		featureCount = features.size();
		
		switch (_STRUCTURE) {
			case UNIGRAM:
				finalSize = featureCount;
				break;
			case BIGRAM:
				finalSize = featureCount + 1;
				break;
			case NTREE:
				for (String s : features) {
					size += ((NTreeApted) JSONParser.parseText(s)).size();				
				}	
				finalSize = size - featureCount + 1; // careful, when featureCount == 0
				break;	
			default:
				System.err.println("Error: Size-based printing not defined for " + _STRUCTURE);
		}		
		
		if (finalSize >= minSize) { 
			try {
				fout = new FileWriter(f);
				for (String s : features)
					fout.write(s + "\n");
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else logger.info("Not enough model elements (min:" + minSize + "), skipping " + key);
	}
	// EXTRACTION END

		
	// VSM 
	public void buildVSMForClustering(UNIT _UNIT, STRUCTURE _STRUCTURE) throws IOException{

		VSM_MODE _VSM_MODE = VSM_MODE.QUADRATIC;
		WEIGHT _WEIGHT = WEIGHT.WEIGHT_1;
		IDF _IDF = IDF.NORM_LOG;
		TYPE_MATCH _TYPE_MATCH = TYPE_MATCH.RELAXED_TYPE;
		SYNONYM _SYNONYM = SYNONYM.REDUCED_SYNONYM;
		SYNONYM_TRESHOLD _SYNONYM_TRESHOLD = SYNONYM_TRESHOLD.SYN80;
		NGRAM_CMP _NGRAM_CMP = NGRAM_CMP.FIX;
		CTX_MATCH _CTX_MATCH = CTX_MATCH.CTX_STRICT;
		FREQ _FREQ = FREQ.FREQ_SUM;
		
		// create output folder if it doesn't exist
		File outputFolder = new File(configuration.vsmFolder);
		if (!outputFolder.exists())
			outputFolder.mkdirs();
		
		Parameters params = new Parameters(null, _UNIT, _STRUCTURE, _WEIGHT, _IDF, _TYPE_MATCH, _SYNONYM, _SYNONYM_TRESHOLD, _NGRAM_CMP, _CTX_MATCH, _FREQ, _VSM_MODE);
		
		logger.info("precomputing NLP comparison values");
		precomputeNLP(_STRUCTURE, _SYNONYM_TRESHOLD);
		
		logger.info("starting vsm computation");
		buildVSMCommon(params, "cluster");	
		
		// compute also the file names
		FileUtil.printFilenameList(configuration.featureFolder, configuration.vsmFolder,  ".features", 0);
		
	}
	
	public void precomputeNLP(STRUCTURE _STRUCTURE, SYNONYM_TRESHOLD _SYNONYM_TRESHOLD) {
		long start = System.currentTimeMillis();
		NLP nlp = new NLP();
		SERIALIZATION _SERIALIZATION = _STRUCTURE.equals(STRUCTURE.NTREE)?SERIALIZATION.JSON:SERIALIZATION.PLAIN;
		try {
			nlp.precomputeTokenLookupTable(configuration.featureFolder, _SERIALIZATION);
			nlp.loadWordNet();
			nlp.precomputeSynonymLookupTable(configuration.featureFolder, _SYNONYM_TRESHOLD.value());

		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("NLP precomputation finished. Time elapsed:" + (System.currentTimeMillis() - start));
	}
	
	


	
	public void buildVSMForCloneDetection(UNIT _UNIT, STRUCTURE _STRUCTURE) throws IOException{		

		IDF _IDF = IDF.NO_IDF;
		NGRAM_CMP _NGRAM_CMP = NGRAM_CMP.FIX; 
		FREQ _FREQ = FREQ.FREQ_SUM;
		
		// create output folder if it doesn't exist
		File outputFolder = new File(configuration.vsmFolder);
		if (!outputFolder.exists())
			outputFolder.mkdirs();
				
		// normal run
		{
			VSM_MODE _VSM_MODE = VSM_MODE.QUADRATIC;
			WEIGHT _WEIGHT = WEIGHT.WEIGHT_1;
			TYPE_MATCH _TYPE_MATCH = TYPE_MATCH.RELAXED_TYPE;
			SYNONYM _SYNONYM =SYNONYM.REDUCED_SYNONYM;
			SYNONYM_TRESHOLD _SYNONYM_TRESHOLD = SYNONYM_TRESHOLD.SYN80;
			CTX_MATCH _CTX_MATCH = _STRUCTURE.equals(STRUCTURE.UNIGRAM)?CTX_MATCH.CTX_STRICT:CTX_MATCH.CTX_LINEAR;
			
			Parameters params = new Parameters(null, _UNIT, _STRUCTURE, _WEIGHT, _IDF, _TYPE_MATCH, _SYNONYM, _SYNONYM_TRESHOLD, _NGRAM_CMP, _CTX_MATCH, _FREQ, _VSM_MODE);
			precomputeNLP(_STRUCTURE, _SYNONYM_TRESHOLD);
			buildVSMCommon(params, "cloneFull");	
		}
		
		// mask run
		{
			VSM_MODE _VSM_MODE = VSM_MODE.LINEAR;
			WEIGHT _WEIGHT = WEIGHT.RAW;
			TYPE_MATCH _TYPE_MATCH = TYPE_MATCH.STRICT_TYPE;
			SYNONYM _SYNONYM =SYNONYM.NO_SYNONYM;
			SYNONYM_TRESHOLD _SYNONYM_TRESHOLD = SYNONYM_TRESHOLD.NO_WORDNET;
			CTX_MATCH _CTX_MATCH = CTX_MATCH.CTX_STRICT;
					
			Parameters params = new Parameters(null, _UNIT, _STRUCTURE, _WEIGHT, _IDF, _TYPE_MATCH, _SYNONYM, _SYNONYM_TRESHOLD, _NGRAM_CMP, _CTX_MATCH, _FREQ, _VSM_MODE);
			buildVSMCommon(params, "cloneMask");	
		}
		
		// compute also the sizes
		FileUtil.printFeatureSizes(configuration.featureFolder, configuration.vsmFolder, _STRUCTURE.toString());
		
		// compute also the file names
		FileUtil.printFilenameList(configuration.featureFolder, configuration.vsmFolder, ".features", 0);			
		
	}
	

	
	public boolean isRedundantConfiguration(Parameters params){
		
		if (PREPROCESS_TOKENIZE && (params._STRUCTURE != STRUCTURE.UNIGRAM || params._UNIT != UNIT.NAME))
		{
			logger.error("Configuration error: PREPROCESS_TOKENIZE can only be used with UNIGRAM NAME combination.");
			return true;
		}
		
		if (params._SYNONYM == SYNONYM.NO_SYNONYM && params._SYNONYM_TRESHOLD != SYNONYM_TRESHOLD.NO_WORDNET)
			return true;
		if (params._STRUCTURE == STRUCTURE.UNIGRAM && (params._CTX_MATCH == CTX_MATCH.CTX_LINEAR || params._CTX_MATCH == CTX_MATCH.CTX_QUAD)) // no context for unigrams
			return true;		
		return false;
	}
	
	public void buildVSMCommon(Parameters params, String tag) throws IOException{		

		String id = Util.generateIdFromParams(params);
		logger.info("running "+ id );								
		vsmBuilder.buildVSM(params, tag);		 			
	}	
	// VSM END
	
	public void clusterInR() {
		r.prepareR();
		
		File targetFolder = new File(configuration.rFolder);
		try {
			FileUtils.deleteDirectory(targetFolder);
		} catch (IOException e) {}
		targetFolder.mkdirs();
		
		logger.info("running clustering in R");
		r.cluster(configuration.vsmFolder + "/vsm-cluster.csv", configuration.vsmFolder + "/names.csv", configuration.rFolder);
		r.finalize();
	}
	
	public void detectClonesInR() {
		r.prepareR();
		
		File targetFolder = new File(configuration.rFolder);
		try {
			FileUtils.deleteDirectory(targetFolder);
		} catch (IOException e) {}
		targetFolder.mkdirs();
		
		logger.info("running clone detection in R");
		r.detectClones(configuration.vsmFolder + "/vsm-cloneFull.csv", configuration.vsmFolder + "/vsm-cloneMask.csv", 
				configuration.vsmFolder + "/names.csv", configuration.vsmFolder + "/sizes.csv", configuration.rFolder);
		r.finalize();
	}

	// R END
}
