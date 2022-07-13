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

package nl.tue.set.samos.extract;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.tue.set.samos.common.enums.SCOPE;
import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.UNIT;
import nl.tue.set.samos.nlp.NLP;

public abstract class IExtractor {
	
	public NLP nlp;
	
	public boolean PREPROCESS_TOKENIZE;
	public boolean PREPROCESS_LEMMATIZE;
//	public int MIN_FEATURE_COUNT_PER_FRAGMENT;
	
	public IExtractor(){		
		nlp = new NLP();
		nlp.loadWordNet();
	}	
	
	public HashMap<String, ArrayList<String>> process(File f, SCOPE _SCOPE, UNIT _UNIT, STRUCTURE _STRUCTURE) {
		
		List<Object> allContents = getAllContainedObjectsByType(f, _SCOPE.toString());		
		String filename = f.getName();
		
		HashMap<String, ArrayList<String>> featureMap = new HashMap<String, ArrayList<String>>();

		if (_SCOPE == SCOPE.MODEL) {
			featureMap.put(filename, new ArrayList<String>());
			if (allContents != null)
				for (Object object : allContents) {
					process(object, featureMap.get(filename), _UNIT, _STRUCTURE);
				}
		}
		else { // lower granularity: e.g. Package or Class
			for(Object object : allContents){
				String key = filename + "$" + getName(object);
				featureMap.put(key, new ArrayList<String>());
				process(object, featureMap.get(key), _UNIT, _STRUCTURE);
			}
		}
		
		return featureMap;
	}
		
	public void process(Object currentObject, ArrayList<String> featureList, UNIT _UNIT, STRUCTURE _STRUCTURE) {
		List<String> currentFeatures = extractFeatures(currentObject, _UNIT, _STRUCTURE);
		if (currentFeatures != null){
			featureList.addAll(currentFeatures);
		}
		
		for (Object element : getNextElements(currentObject)){
			process(element, featureList, _UNIT, _STRUCTURE);
		}
	}
	
	
	// to be implemented
	public abstract List<String> extractFeatures(Object CURRENT, UNIT _UNIT, STRUCTURE _STRUCTURE);
	// to be implemented
	public abstract List<Object> getNextElements(Object CURRENT);
	// to be implemented
	public abstract List<Object> getAllContainedObjectsByType(File f, String type);
	
	public abstract String getName(Object o);
}