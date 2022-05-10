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

package nl.tue.set.samos.common;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.feature.NTreeApted;
import nl.tue.set.samos.feature.parser.JSONParser;

public class FileUtil {
	
	static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	public static final String DS_STORE = ".DS_Store";

	public static int getSize(File file, String unit){
		int finalSize = 0;
		try{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		int size = 0, featureCount = 0;
		while ((line = br.readLine()) != null) {
			featureCount++;
			
			if (!unit.equals("NTREE"))
				size++;
			else{
				size += ((NTreeApted) JSONParser.parseText(line)).size();
			}
		}
		
		if (unit.equals("NTREE"))
			finalSize = size - featureCount + 1;
		else if (unit.equals("BIGRAM"))
			finalSize = size + 1; // featureCount + 1 also
		else if (unit.equals("UNIGRAM"))
			finalSize = size; // featureCount also
		if (size != finalSize) {
			;
		}
		br.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return finalSize;
	}
	
	public static void printFeatureSizes(String featureFolder, String targetFolder, String unit){
		File dir = new File(featureFolder);
		File[] files = dir.listFiles();
		
		Vector<Integer> sizeVector = new Vector<Integer>();
				
		for (File file: files){
			if (file.isFile()) {
				if (file.getName().equals(DS_STORE)) continue;
				if (!file.getName().endsWith(".features")) continue;	
							
				sizeVector.add(getSize(file, unit));													
			}
		}
		
		int totalSize = 0;
		File output = new File(targetFolder + "/sizes.csv");
		try {
			FileWriter fr = new FileWriter(output);
			int lastElement = sizeVector.lastElement();
			sizeVector.remove(sizeVector.size()-1);
			for(int size : sizeVector){
				fr.write(size + "\n");
				totalSize += size;
				logger.trace("found a model with size = " +size);
			}
			fr.write(lastElement + "\n");
			totalSize += lastElement;
			logger.trace("total Size:"  + totalSize);
			fr.flush();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	public static final String FILE_EXTENSION = ".ecore";
	public static final int PREFIX_SIZE = 0;
	public static void printFilenameList(String featurePath, String outputPath){
		printFilenameList(featurePath, outputPath, FILE_EXTENSION, PREFIX_SIZE);
	}
	
	public static void printFilenameList(String featurePath, String outputPath, String fileExtension, int prefixSize){
		try{
			File folder = new File(featurePath);

			File output = new File(outputPath + "/names.csv");
			FileWriter fr = new FileWriter(output);

			File[] files = folder.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(fileExtension);
				}				
			});
			System.out.println(files.length);

			for (int i=0; i<files.length; i++){
				String name = files[i].getName();
				name = name.substring(prefixSize, name.length()-(fileExtension.length()));
				fr.write(name);
				fr.write("\n");
			}
			fr.close();
		} catch(Exception ex) {ex.printStackTrace();}
	}	
}
