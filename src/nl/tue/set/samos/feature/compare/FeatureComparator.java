/*
 * Copyright (c) 2015-2021 Onder Babur
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

package nl.tue.set.samos.feature.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import distance.APTED;
import nl.tue.set.samos.common.Pair;
import nl.tue.set.samos.common.Parameters;
import nl.tue.set.samos.common.Util;
import nl.tue.set.samos.common.enums.CTX_MATCH;
import nl.tue.set.samos.common.enums.NGRAM_CMP;
import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.SYNONYM;
import nl.tue.set.samos.common.enums.SYNONYM_TRESHOLD;
import nl.tue.set.samos.common.enums.TYPE_MATCH;
import nl.tue.set.samos.feature.AttributedNode;
import nl.tue.set.samos.feature.Feature;
import nl.tue.set.samos.feature.NGram;
import nl.tue.set.samos.feature.NTree;
import nl.tue.set.samos.feature.NTreeApted;
import nl.tue.set.samos.feature.NamedFeature;
import nl.tue.set.samos.feature.SimpleFeature;
import nl.tue.set.samos.feature.SimpleType;
import nl.tue.set.samos.feature.TypedFeature;
import nl.tue.set.samos.feature.TypedName;
import nl.tue.set.samos.feature.TypedValuedName;
import nl.tue.set.samos.nlp.NLP;
import node.Node;

public class FeatureComparator {
	
	final Logger logger = LoggerFactory.getLogger(FeatureComparator.class);
	
	// TODO do this configurable
	public final int TREE_COMPARE = 
//			0; // ordered tree edit distance
			1; // hungarian (assignment problem for the leaves)
	
	Parameters parameters;
	
	public final NLP nlp = new NLP(); 
	
	public LinkedHashSet<String> dictionary = new LinkedHashSet<String>();
	public HashMap<String, Integer> reverseDictionary = new HashMap<String, Integer>();
	public HashMap<Integer, String[]> tokenLookup = new HashMap<Integer, String[]>();
	public HashMap<Pair<Integer, Integer>, Double> synonymLookup = new HashMap<Pair<Integer, Integer>, Double>();
	
	APTED<FeatureCostModel, Feature> apted;
	
	public FeatureComparator(Parameters parameters) {
		this.parameters = parameters;
		
		fillTempDataStructures();
		
		apted = new APTED<>(new FeatureCostModel(this));
		
		nlp.loadWordNet();
	}
	
	public static void arrayCopy(double[][] aSource, double[][] aDestination) {
	    for (int i = 0; i < aSource.length; i++) {
	        System.arraycopy(aSource[i], 0, aDestination[i], 0, aSource[i].length);
	    }
	}
	
	public boolean TRACE_SIMILARS = false;
	public boolean TRACE_SIMILARS_NTREE = false;
	
	// HACK to avoid creating new arrays
	private void fillTempDataStructures(){
		// TODO is it ok to do this, as this is the max size of the matrix?
		//int n = parameters._STRUCTURE.ordinal()+1; // N if no edges included
		
		// ORIGINAL
		//int n = 2 * parameters._STRUCTURE.ordinal() +1; // N + (N-1) with edges
		
		// workaround for NTrees, TODO proper
		int n = 40;
				
		emptyMatrix  = new double[n][n];
		for (double[] row: emptyMatrix)
			Arrays.fill(row, 0.0);
		
		typeMultipliers = new double[n][n];
		typeExactMatches = new boolean[n][n];
		typeValueMultipliers = new double[n][n];
		typeValueExactMatches = new boolean[n][n];
		synMultipliers = new double[n][n];
		synPairs  = new Object[n][n];
		synPairRevs = new Object[n][n];
//		synDoubles = new double[n][n];
		attributeMultipliers = new double[n][n];
		sims = new double[n][n];
	}
	
	@SuppressWarnings("unchecked")
	public void loadUpCache(String sourceFileFolder) throws IOException{
//		dictionary = new LinkedHashSet<String>();
		File dictFile = new File(sourceFileFolder + "/dictionary.ser");  
		if (dictFile.exists())
		{						  
			logger.info("found dictionary file!!");
			FileInputStream fis = new FileInputStream(dictFile);  
			ObjectInputStream s = new ObjectInputStream(fis);    
			try {
				dictionary = (LinkedHashSet<String>)s.readObject();
				// adding dummy name TODO proper 
				dictionary.add("#ASD#QWE#ZXC#");
//				int size = dictionary.size();
//				for (int i=0; i<size; i++)
//					reverseDictionary.put(dictionary.get(i), i);
				int i=0;
				for (String word : dictionary) {
					reverseDictionary.put(word, i);
					i++;
				}
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				s.close();
				return;
			} 								
			s.close();
		}
		
		synonymLookup = new HashMap<Pair<Integer, Integer>, Double>();
		
		if (parameters._SYNONYM != SYNONYM.NO_SYNONYM) {										
			String suffix = parameters._SYNONYM_TRESHOLD!=SYNONYM_TRESHOLD.NO_WORDNET?"_WNET":"_NOWNET";
			File synFile = new File(sourceFileFolder + "/syn" + suffix + ".ser");  
			
			FileInputStream fis = new FileInputStream(synFile);  
			ObjectInputStream s = new ObjectInputStream(fis);    
			try {
				synonymLookup = (HashMap<Pair<Integer, Integer>, Double>)s.readObject();
				logger.info("synonym file loaded: " + synonymLookup.size());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				s.close();
				// turned off TODO proper 
				// return;
			} 								
			s.close();
		}		
	}
	
	public final double REDUCED_TM_MULTIPLIER = 0.5;
	
	@SuppressWarnings("unused")
	public double compare(Feature f1, Feature f2){
		
		if (!f1.getClass().equals(f2.getClass()))
			return 0.0;
		if (f1 instanceof NGram && f2 instanceof NGram)
			return compareNGram((NGram)f1, (NGram)f2);
		if (f1 instanceof NTreeApted && f2 instanceof NTreeApted) {
			
			if (TREE_COMPARE == 0)
				return compareNTreeApted((NTreeApted)f1, (NTreeApted)f2);
			else
				return compareNTreeHungarian((NTreeApted)f1, (NTreeApted)f2);
		}
		else {
			System.err.println("Problematic case for feature comparison: " + f1.toString() + " vs " + f2.toString());
			return 0.0;
		}
	}
	
	// Note: Only talking about simple features here
	public boolean isTypedFeature(SimpleFeature f){
		return (f instanceof TypedFeature);
	}
	
	public boolean isTypeValuedFeature(SimpleFeature f){
		return (f instanceof TypedValuedName) || (f instanceof AttributedNode && ((AttributedNode)f).hasAttribute("eType"));
	}
	
	public String getType(SimpleFeature f){
		if (f instanceof TypedFeature)
			return ((TypedFeature)f).getType();
		else
			return "";
	}
	
	public TypedName getTypeValue(SimpleFeature f){
		if (f instanceof TypedValuedName)
			return ((TypedValuedName)f).getTypeValue();
		else
			return null;
	}
	
	public boolean isNamedFeature(SimpleFeature f){
		return (f instanceof NamedFeature);
	}
	
	public String getName(SimpleFeature f){
		if (f instanceof NamedFeature)
			return ((NamedFeature)f).getName();
		else
			return "";
	}
	
	protected double typeMultipliers[][];
	protected boolean typeExactMatches[][];
	protected double typeValueMultipliers[][];
	protected boolean typeValueExactMatches[][];
	protected double synMultipliers[][];
	protected Object[][] synPairs;
	protected Object[][] synPairRevs;
//	protected double synDoubles[][];
	protected double sims[][];
	protected double attributeMultipliers[][];
	protected static double emptyMatrix[][];
	
	public double compareNGram(NGram rowNgram, NGram columnNgram){
		if (rowNgram.n != columnNgram.n) {
			// TODO turn this on again, or implement a better check
			//logger.error("non-matching ngrams!!");
			return 0.0;
		}

		if (parameters._TYPE_MATCH == TYPE_MATCH.IGNORE_TYPE)
		{
			for (double[] row: typeMultipliers)
				Arrays.fill(row, 1.0); 
			for (double[] row: typeValueMultipliers)
				Arrays.fill(row, 1.0); 
		}
		else  // reduced or exact match
		{
			arrayCopy(emptyMatrix, typeMultipliers);
			arrayCopy(emptyMatrix, typeValueMultipliers);
			
			// CHECKING TYPES
			for (int i=0; i<rowNgram.n; i++){
				for (int j=0; j<columnNgram.n; j++){
					if (!isTypedFeature(rowNgram.get(i)) && !isTypedFeature(columnNgram.get(j)))
						typeExactMatches[i][j] = true; // HACK
					else if (!isTypedFeature(rowNgram.get(i)) || !isTypedFeature(columnNgram.get(j)))
						typeExactMatches[i][j] = false;
					else {						
						TypedFeature f1 = (TypedFeature) rowNgram.get(i);
						TypedFeature f2 = (TypedFeature) columnNgram.get(j); 
						typeExactMatches[i][j] = f1.getType().equals(f2.getType());
												
						// for the case with AttributedNodes & EAttributes, also check eType (i.e. don't generate a bigram for it) TODO proper 
						if (rowNgram.get(i) instanceof AttributedNode && columnNgram.get(j) instanceof AttributedNode){
							AttributedNode an1 = ((AttributedNode) rowNgram.get(i));
							AttributedNode an2 = ((AttributedNode) columnNgram.get(j));
							if (an1.hasAttribute("eType") && an2.hasAttribute("eType")) {
								String t1 = ((AttributedNode) rowNgram.get(i)).getAttribute("eType");
								String t2 = ((AttributedNode) columnNgram.get(j)).getAttribute("eType");
								try{ typeExactMatches[i][j] = typeExactMatches[i][j] && t1.equalsIgnoreCase(t2);} catch(Exception ex){ // TODO do this properly with NLP
									ex.printStackTrace();
								}
							}
						}
					}
				}
			}
			
			for (int i=0; i<rowNgram.n; i++)
				for (int j=0; j<columnNgram.n; j++)
					typeMultipliers[i][j] = typeExactMatches[i][j]?1.0:(parameters._TYPE_MATCH == TYPE_MATCH.RELAXED_TYPE?REDUCED_TM_MULTIPLIER:0.0); // TODO too cryptic, refine
			
			
			// COPY PASTA FROM ABOVE, for typeValues
			for (int i=0; i<rowNgram.n; i++){
				for (int j=0; j<columnNgram.n; j++){
					if (!isTypeValuedFeature(rowNgram.get(i)) && !isTypeValuedFeature(columnNgram.get(j)))
						typeValueExactMatches[i][j] = true; // HACK
					else if (!isTypeValuedFeature(rowNgram.get(i)) || !isTypeValuedFeature(columnNgram.get(j)))
						typeValueExactMatches[i][j] = false;
					else {						
						AttributedNode f1 = (AttributedNode) rowNgram.get(i);
						AttributedNode f2 = (AttributedNode) columnNgram.get(j); 
						typeValueExactMatches[i][j] = f1.getAttribute("eType").equalsIgnoreCase(f2.getAttribute("eType")); // TODO do proper NLP compare											
					}
				}
			}
			
			for (int i=0; i<rowNgram.n; i++)
				for (int j=0; j<columnNgram.n; j++)
					typeValueMultipliers[i][j] = typeValueExactMatches[i][j]?1.0:(parameters._TYPE_MATCH == TYPE_MATCH.RELAXED_TYPE?REDUCED_TM_MULTIPLIER:0.0); // TODO too cryptic, refine
		}

		// CHECKING SYNONYMS
		arrayCopy(emptyMatrix, synMultipliers);		
		if (parameters._SYNONYM == SYNONYM.NO_SYNONYM){
			for (int i=0; i<rowNgram.n; i++)
				for (int j=0; j<columnNgram.n; j++) {
					if (!isNamedFeature(rowNgram.get(i)) && !isNamedFeature(columnNgram.get(j)))
						synMultipliers[i][j] = 1; // HACK: as a rule of thumb, assume edges are not included when type checking is off -> domain analysis only
					else if (!isNamedFeature(rowNgram.get(i)) || !isNamedFeature(columnNgram.get(j)))
						synMultipliers[i][j] = 0;
					else {
						NamedFeature f1 = (NamedFeature) rowNgram.get(i);
						NamedFeature f2 = (NamedFeature) columnNgram.get(j); 
						synMultipliers[i][j] = f1.getName().equals(f2.getName())?1.0:0.0;
					}
				}
		} else { 			
			// TODO tricky situation e.g. when checking a contains b vs. b contains c
			// case 1: comparing edges TN contains vs TN contains 
			// case 2: comparing edges TN contains vs TN supertype
			// type checking is clear, 1 for case 1, 0 for case 2
			// synonym checking:
			// if 0 -> negates the effect of type checking, so case 1 and case 2 become same ==> so should not give 0 by default
			// if 1 -> bias for edges, as ALL the edges are going to be considered similar
			// ASSUMPTION type checking should always be on when dealing with n-grams + edges included, partly solves the problem for edges of different tyles
			// PROBLEM: if 1 blindly, then every [X contains Y] always similar to [Z contains W]. 
			// see workaround below
			for (int i=0; i<rowNgram.n; i++){
				for (int j=0; j<columnNgram.n; j++){
					if (!isNamedFeature(rowNgram.get(i)) && !isNamedFeature(columnNgram.get(j)))
						synMultipliers[i][j] = 1; // HACK
					else if (!isNamedFeature(rowNgram.get(i)) || !isNamedFeature(columnNgram.get(j)))
						synMultipliers[i][j] = 0;
					else {
						NamedFeature f1 = (NamedFeature) rowNgram.get(i);
						NamedFeature f2 = (NamedFeature) columnNgram.get(j); 
						Integer index1 = reverseDictionary.get(f1.getName());
						Integer index2 = reverseDictionary.get(f2.getName());
						synPairs[i][j] = new Pair<Integer, Integer>(index1, index2);
						synPairRevs[i][j] = new Pair<Integer, Integer>(index2, index1);
						
						// NORMAL CHECK BEGIN
						try{
						if (synonymLookup.containsKey(synPairs[i][j])) {
							synMultipliers[i][j] = synonymLookup.get(synPairs[i][j]);
							if (parameters._SYNONYM_TRESHOLD != SYNONYM_TRESHOLD.NO_WORDNET && 
									synMultipliers[i][j] < Util.getSynonymTreshold(parameters._SYNONYM_TRESHOLD)) synMultipliers[i][j] = 0.0;
						} else if (synonymLookup.containsKey(synPairRevs[i][j])) {
							synMultipliers[i][j] = synonymLookup.get(synPairRevs[i][j]);
							if (parameters._SYNONYM_TRESHOLD != SYNONYM_TRESHOLD.NO_WORDNET && 
									synMultipliers[i][j] < Util.getSynonymTreshold(parameters._SYNONYM_TRESHOLD)) synMultipliers[i][j] = 0.0;
						}
						else 
							synMultipliers[i][j] = 0.0;
						} catch(Exception ex){
							// this happens when generating e.g. bigrams with types/supertypes retrieved from the URL -> hence not found in unigrams TODO fix
							synMultipliers[i][j] = nlp.compareMultiword(f1.getName(), f2.getName(), Util.getSynonymTreshold(parameters._SYNONYM_TRESHOLD));
						}
						// NORMAL CHECK END						
					}
				}
			}
		}
		
		// NEW: attributes adhoc now, TODO improve
		// TODO performance issues when there are no attributes!!!
		for (double[] row : attributeMultipliers)
			Arrays.fill(row, 1.0);
		
		for (int i=0; i<rowNgram.n; i++){
			for (int j=0; j<columnNgram.n; j++){
				if (rowNgram.get(i) instanceof AttributedNode && columnNgram.get(j) instanceof AttributedNode	// both ANs							
						&& ((AttributedNode)rowNgram.get(i)).getType().equals(((AttributedNode)columnNgram.get(j)).getType()) // same types also assume when there are attributes,type checking should be always on TODO improve
						) 
				{
					double atrCompare = compareAttributes((AttributedNode)rowNgram.get(i), (AttributedNode)columnNgram.get(j));
					if (atrCompare > 0.7 && atrCompare < 1.0)
						; // TODO for debugging, remove
					if (parameters._CTX_MATCH == CTX_MATCH.CTX_STRICT && parameters._TYPE_MATCH == TYPE_MATCH.STRICT_TYPE) // HACK added the second expression to check type match as well, problematic when dealing with unigrams  (always CTX_STRICT)
						attributeMultipliers[i][j] = atrCompare==1.0?1.0:0.0;
					else
						attributeMultipliers[i][j] = atrCompare;														
				}
			}
		}

		arrayCopy(emptyMatrix, sims);
		for (int i=0; i<rowNgram.n; i++){
			for (int j=0; j<columnNgram.n; j++){
				sims[i][j] = typeMultipliers[i][j] * synMultipliers[i][j] * attributeMultipliers[i][j]
						* typeValueMultipliers[i][j];
			}
		}
		
		// workaround: another pass on the synDoubles matrix just for the edges: TODO proper
		// assume edges cannot be at the corners of the matrix!
		// another one, switch on off w.r.t structure
		if (!(parameters._STRUCTURE == STRUCTURE.NTREE))
		{
			for (int i=0; i<rowNgram.n; i++){
				for (int j=0; j<columnNgram.n; j++){
					// if both simple types ~ edges, typically should occur at i=j
					if (rowNgram.get(i) instanceof SimpleType && columnNgram.get(j) instanceof SimpleType){
						// go up and down on the diagonal, check if both are zero. If so, set it to zero as well.
						if (sims[i-1][j-1] == 0 && sims[i+1][j+1] == 0) {
							sims[i][j] = 0;
						}
						// if any of them non-zero, leave it as it is. 
					}
				}
			}
		}
		
		// edges can also be at the corners of the matrix!!
		else {
			for (int i=0; i<rowNgram.n; i++){
				for (int j=0; j<columnNgram.n; j++){
					// if both simple types ~ edges, typically should occur at i=j
					if (rowNgram.get(i) instanceof SimpleType && columnNgram.get(j) instanceof SimpleType){
						// go up and down on the diagonal, check if both are zero. If so, set it to zero as well.
						boolean upFlag = (i>1 && j>1 && (sims[i-1][j-1] == 0)) || (i<1 || j<1);
						boolean downFlag = (i<rowNgram.n-1 && j<columnNgram.n-1 && (sims[i+1][j+1] == 0)) || 
								(i>rowNgram.n-1 || j>columnNgram.n-1);
						if (upFlag && downFlag) {
							sims[i][j] = 0;
						}
						// if any of them non-zero, leave it as it is. 
					}
				}
			}
		}

		double resultSim;
		int resultN;
		if (parameters._NGRAM_CMP == NGRAM_CMP.FIX) {
			double[] res = this.nlp.fcsAll(sims); 
			resultSim = res[0];
			resultN = (int) res[1];
		}
		else { // if MSS
			resultSim = this.nlp.lcs(sims);
			resultN = this.nlp.lcsLength(sims);
		}
		
		double finalResult = 0.0;
		if(parameters._CTX_MATCH == CTX_MATCH.CTX_STRICT) // average sim if all match
			finalResult = (resultN == rowNgram.n)?(resultSim/rowNgram.n):0.0;
		else if (parameters._CTX_MATCH == CTX_MATCH.CTX_LINEAR)
			finalResult = ((resultN+1.0)/(rowNgram.n + 1.0)) * (resultSim) / rowNgram.n;
		else if (parameters._CTX_MATCH == CTX_MATCH.CTX_QUAD)
			finalResult = Math.pow(((resultN+1.0)/(rowNgram.n + 1.0)), 2) * (resultSim) / rowNgram.n;
		
		if (TRACE_SIMILARS && finalResult > 0 && !rowNgram.equals(columnNgram)/* & finalResult > 0.8*/) 
			logger.trace(rowNgram + "\t\t" + columnNgram + "\t\t" + finalResult);
		
		return finalResult;
	}
	
	public double compareNTreeApted(NTreeApted rowNTreeApted, NTreeApted columnNTreeApted) {
		float distance;
		try { 
			distance = apted.computeEditDistance(rowNTreeApted.aptedTree, columnNTreeApted.aptedTree);
			assert distance >= 0 && distance <= 1;
			distance = distance / Math.max(rowNTreeApted.size(), columnNTreeApted.size());			
		} catch(Exception ex) {
			logger.error("exception for " + rowNTreeApted + " vs " + columnNTreeApted);
			ex.printStackTrace();
			distance = 1;
			}
		float result = 1 - distance;
		// TODO this shouldn't happen
		if (result < 0) result = 0;			
		if(parameters._CTX_MATCH == CTX_MATCH.CTX_STRICT)
			return result==1?1:0;
		else {
			if (TRACE_SIMILARS_NTREE && result > 0 && !rowNTreeApted.equals(columnNTreeApted)) 
				logger.trace(rowNTreeApted + "\t\t" + columnNTreeApted + "\t\t" + result);
			return result;
		}
	}
	
	public double compareNTreeHungarian(NTreeApted rowNTreeApted, NTreeApted columnNTreeApted) {
		float distance;
		try { 
			distance = computeHungarianDistance(rowNTreeApted.aptedTree, columnNTreeApted.aptedTree);
			assert distance >= 0 && distance <= 1;
		} catch(Exception ex) {
			logger.error("exception for " + rowNTreeApted + " vs " + columnNTreeApted);
			ex.printStackTrace();
			distance = 1;
			}
		float result = 1 - distance;
		if(parameters._CTX_MATCH == CTX_MATCH.CTX_STRICT)
			return result==1?1:0;
		else {
			if (TRACE_SIMILARS_NTREE && result > 0 && !rowNTreeApted.equals(columnNTreeApted)) 
				logger.error(rowNTreeApted + "\t\t" + columnNTreeApted + "\t\t" + result);
			return result;
		}
	}
	
	public float computeHungarianDistance(Node<Feature> tree1, Node<Feature> tree2) {
		Vector<Node<Feature>> children1 = tree1.getChildren();
		Vector<Node<Feature>> children2 = tree2.getChildren();
		
		Vector<Feature> features1 = new Vector<Feature>();
		for (Node<Feature> node : children1) features1.add(node.getNodeData());
		
		Vector<Feature> features2 = new Vector<Feature>();
		for (Node<Feature> node : children2) features2.add(node.getNodeData());
		
		// swap TODO check
		if (features1.size() < features2.size()) {
			Vector<Feature> featuresTemp = features1;
			features1 = features2;
			features2 = featuresTemp;
		}
		
		double[][] comparisonMatrix = new double[features1.size()][features2.size()];
		for (int i=0; i<features1.size(); i++) {			
			for (int j=0; j<features2.size(); j++) {
				comparisonMatrix[i][j] = 1.0d - compareNGram((NGram) features1.get(i), (NGram) features2.get(j));
				
				// if completely different (1.0), set to 2.0
				if (comparisonMatrix[i][j] >= 1.0) 
					comparisonMatrix[i][j] = 2.0;
			}
		}
		
		double parentDistance = 1.0d - compareNGram((NGram) tree1.getNodeData(), (NGram) tree2.getNodeData());
		int minLeafCount = Math.min(features1.size(), features2.size()); 
		int maxLeafCount = Math.max(features1.size(), features2.size());
		double leavesDistance = 0.0;
		
		if (features1.size() == 0 && features2.size() == 0) {
			leavesDistance = 0.0;
			return (float) ((parentDistance + leavesDistance) / (1 + maxLeafCount));
		}
		else if (minLeafCount == 0) {
			leavesDistance = Math.max(features1.size(), features2.size());
			return (float) ((parentDistance + leavesDistance) / (1 + maxLeafCount));
		}
		else{
			HungarianAlgorithm hungarian = new HungarianAlgorithm(comparisonMatrix);
			int[] match = hungarian.execute();
			double cost = hungarian.computeCost(comparisonMatrix, match);
			
			leavesDistance = cost + Math.abs(features1.size() - features2.size());
			
			for (int i = 0; i<match.length; i++) {
				if (match[i] != -1 && comparisonMatrix[i][match[i]] > 1)
					maxLeafCount++;
			}
			
			return (float) ((parentDistance + leavesDistance) / (1 + maxLeafCount));
		}
		 
	}
	
	
	 public<E> List<List<E>> generatePerm(List<E> original) {
	     if (original.size() == 0) { 
	       List<List<E>> result = new ArrayList<List<E>>();
	       result.add(new ArrayList<E>());
	       return result;
	     }
	     E firstElement = original.remove(0);
	     List<List<E>> returnValue = new ArrayList<List<E>>();
	     List<List<E>> permutations = generatePerm(original);
	     for (List<E> smallerPermutated : permutations) {
	       for (int index=0; index <= smallerPermutated.size(); index++) {
	         List<E> temp = new ArrayList<E>(smallerPermutated);
	         temp.add(index, firstElement);
	         returnValue.add(temp);
	       }
	     }
	     return returnValue;
	   }

	private Object getAttributeOrDefaultValue(AttributedNode node, String attributeName){
		if (node.hasAttribute(attributeName))
			return node.getAttribute(attributeName);
		else return Util.getDefaultAttributeValue(attributeName);
	}
	
	public double compareAttributes(AttributedNode node1, AttributedNode node2){
		assert(node1.size() == node2.size());
		int nonmatch = 0;
		Set<String> allAttributes = new HashSet<String>();
		allAttributes.addAll(node1.getAttributes());
		allAttributes.addAll(node2.getAttributes());
		for (String key : allAttributes){
			if (key.equals("name") || key.equals("type") || key.equals("eType"))
				continue;
			else {
				// TODO maybe can optimize a bit more, when both are absent - without checking default values 
				if (!(getAttributeOrDefaultValue(node1, key).equals(getAttributeOrDefaultValue(node2, key))))
					nonmatch++;
			}
		}

		double value = (1 - ((1.0 * nonmatch) / Util.getTotalAttributeCount(node1.getType())));
		return value; 
	}
	
	public double compareNTrees(NTree nt1, NTree nt2){
		ArrayList<Double> globalSims = new ArrayList<Double>();
		
		double max = 0.0;
		for (Double d : globalSims){
			max = Math.max(max, d);
		}
		return max;
	}
}
