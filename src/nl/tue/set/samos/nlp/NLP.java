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

package nl.tue.set.samos.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import edu.cmu.lti.lexical_db.ILexicalDatabase;
//import edu.cmu.lti.lexical_db.NictWordNet;
//import edu.cmu.lti.ws4j.RelatednessCalculator;
//import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.item.POS;
import nl.tue.set.samos.common.Constants;
import nl.tue.set.samos.common.Pair;
import nl.tue.set.samos.common.enums.SERIALIZATION;
import nl.tue.set.samos.feature.Feature;
import nl.tue.set.samos.feature.NGram;
import nl.tue.set.samos.feature.NTreeApted;
import nl.tue.set.samos.feature.NamedFeature;
import nl.tue.set.samos.feature.parser.JSONParser;
import nl.tue.set.samos.feature.parser.PlainTextParser;
import uk.ac.open.crc.intt.IdentifierNameTokeniser;
import uk.ac.open.crc.intt.IdentifierNameTokeniserFactory;

// https://sourceforge.net/p/jwordnet/code/HEAD/tree/trunk/jwnl/src/net/didion/jwnl/utilities/Examples.java
// https://code.google.com/archive/p/jawjaw/downloads
// http://www.nltk.org/howto/wordnet.html

/**
 * This is the main conglomerate class for NLP functionalities. It includes many top level methods for computing and storing nlp results for better performance. It also contains the following technhiques:
 * 
 *	- WordNet semantic similarity checking with Lin similarity
 *  - maximum similar subsequence algorithm, a variation of longest common subsequence
 *  - levenshtein distance
 *  - lemmatizer (Stanford NLP)
 *  - tokenizer (intt)
 *  - stemmer (Porter)
 *  - stop word removal and other filtering techniques
*/
public class NLP {
	
	final Logger logger = LoggerFactory.getLogger(NLP.class);
	
	private final int minLength = 3;
	private final double LevenshteinSimilarityTreshold = 0.2;
	
	private final String tokenSeparators = "$_- ";
	
	// added the word 'has' here, which was not originally in the set
	private final List<String> englishStopWords = Arrays.asList(
		       "a", "an", "and", "are", "as" , "at", "be", "but", "by", // as disabled for statecharts
		       "for", "if", "in", "into", "is", "it",
		       "no", "not", "of", "on", "or", "such",
		       "that", "the", "their", "then", "there", "these",
		       "they", "this", "to", "was", "will", "with", "has"
		     );
	
	public HashMap<String, String[]> tokenLookup = new HashMap<String, String[]>();
	public HashMap<String, Double> synLookup = new HashMap<String, Double>();
		
	public Lin lin;
	public Path path;
	public IDictionary dict;
	
	public Lemmatizer lemmatizer;
	
	public boolean TRACE_SYNONYMS = false;
	
	public NLP(){
        WS4JConfiguration.getInstance().setMFS(true);
        
        // experimental settings to tackle the boolean expressions in statecharts
        factory.setSeparatorCharacters(tokenSeparators); 
        
        tokeniser = factory.create();
        
        lemmatizer = Lemmatizer.getInstance();
	}
	
	// bulk load wordnet-related files
	public void loadWordNet(){      
      String wnhome 	= "wordnet/dict";
      String icfile		= "wordnet/semcor/ic-semcor.dat";
      URL url = null;
      try
      {
      	url = new URL("file", null, wnhome);
      }
      catch(MalformedURLException e)
      {
      	e.printStackTrace();
      }
      if(url == null) return;
      dict = new RAMDictionary(url);
      try {
      	dict.open();
      } catch (IOException e) {
      	e.printStackTrace();
      }
      ICFinder 			icfinder 			=	new ICFinder(icfile);
      // ....................................................................................................................................................................
      lin = new Lin(dict, icfinder);
      // ....................................................................................................................................................................
	}
	
	
	// maximum similar subsequence algorithm, a variation of longest common subsequence which can work with non-exact similarity scores [0,1]. 
	// TODO Optimize to cut off early using a difference limit (inspired by NiCaD-Simone)
	public double lcs(double[][] scoreMatrix){ //String x, String y
		int M = scoreMatrix.length;
		int N = scoreMatrix[0].length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        double[][] opt = new double[M+1][N+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
            	if (scoreMatrix[i][j] > 0)
            		opt[i][j] = opt[i+1][j+1] + scoreMatrix[i][j];
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print it to standard output
        int i = 0, j = 0;
        while(i < M && j < N) {
        	if (scoreMatrix[i][j] > 0) {
                i++;
                j++;
            }
            else if (opt[i+1][j] >= opt[i][j+1]) i++;
            else                                 j++;
        }
        
        double max = 0;
        for (i=0; i<opt.length; i++)
        	for (j=0; j<opt[0].length; j++)
        		max = Math.max(max, opt[i][j]);
        return max;
	}
	
	// fixed maximum similar subsequence algorithm, where items are matched with fixed positions
	// TODO lcsAll also optimize
	// TODO even put temp double[] for more optimization
	public double[] fcsAll(double[][] scoreMatrix){ //String x, String y
		int M = scoreMatrix.length;
		// assume same length ngrams
		
		double max = 0;
		int length = 0;
		for (int i=0; i<M; i++) {
			max += scoreMatrix[i][i];
			if (scoreMatrix[i][i] > 0) length++;
		}

      return new double[]{max, length};
	}
	
	public double fcs(double[][] scoreMatrix){ //String x, String y
		int M = scoreMatrix.length;
		// assume same length ngrams
		
		double max = 0;
		for (int i=0; i<M; i++)
			max += scoreMatrix[i][i];

      return max;
	}
	
	public int fcsLength(double[][] scoreMatrix){ //String x, String y
		int M = scoreMatrix.length;
		// assume same length ngrams
		
		int max = 0;
		for (int i=0; i<M; i++)
			if (scoreMatrix[i][i] > 0) max++;

      return max;
	}

	public int lcsLength(double[][] scoreMatrix){ //String x, String y
		int M = scoreMatrix.length;
		int N = scoreMatrix[0].length;

      // opt[i][j] = length of LCS of x[i..M] and y[j..N]
      int[][] opt = new int[M+1][N+1];

      // compute length of LCS and all subproblems via dynamic programming
      for (int i = M-1; i >= 0; i--) {
          for (int j = N-1; j >= 0; j--) {
          	if (scoreMatrix[i][j] > 0)
          		opt[i][j] = opt[i+1][j+1] + 1;
              else 
                  opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
          }
      }

      // recover LCS itself and print it to standard output
      int i = 0, j = 0;
      while(i < M && j < N) {
      	if (scoreMatrix[i][j] > 0) {
              i++;
              j++;
          }
          else if (opt[i+1][j] >= opt[i][j+1]) i++;
          else                                 j++;
      }
      
      int max = 0;
      for (i=0; i<opt.length; i++)
      	for (j=0; j<opt[0].length; j++)
      		max = Math.max(max, opt[i][j]);
      return max;
	}

	// main method for comparing two model element names, each of which are typically compound names with multiple tokens 
	public double compareMultiword(String word1, String word2, double wordNetTreshold){ //String x, String y
		//this.NLPOpt.filter(this.NLPOpt.tokeniseIntt(basePair.y))
		String[] expandedTokens1, expandedTokens2;
		expandedTokens1 = filter(tokeniseIntt(word1));
		if (expandedTokens1.length == 0) expandedTokens1 = new String[] {word1};
		expandedTokens2 = filter(tokeniseIntt(word2));
		if (expandedTokens2.length == 0) expandedTokens2 = new String[] {word2};
		double[][] scoreMatrix = new double[expandedTokens1.length][expandedTokens2.length];
		double sum = 0.0;
		for (int i=0; i<expandedTokens1.length; i++) {
			for (int j=0;j<expandedTokens2.length; j++) {
				scoreMatrix[i][j] = isSynonymExact(expandedTokens1[i], expandedTokens2[j], wordNetTreshold);
				//scoreMatrix[i][j] = isSynonymExact(expandedTokens1[i], expandedTokens2[j], 0);
				if(scoreMatrix[i][j] > 0) {
					sum += scoreMatrix[i][j];
				}
			}
		}
		

      return sum  / (1.0 * Math.max(expandedTokens1.length, expandedTokens2.length));
	}
	
	// compare two model element names with token lookup for increased performance
	public double compareMultiwordWithTokenLookup(Integer word1, Integer word2, double wordNetTreshold, HashMap<Integer, String[]> tokenLookup, HashMap<String, String> lemmaLookup){ //String x, String y
		String[] expandedTokens1, expandedTokens2;
		expandedTokens1 = tokenLookup.get(word1);
		expandedTokens2 = tokenLookup.get(word2);
		double[][] scoreMatrix = new double[expandedTokens1.length][expandedTokens2.length];
		double sum = 0.0;
		for (int i=0; i<expandedTokens1.length; i++)
			for (int j=0;j<expandedTokens2.length; j++) {
				scoreMatrix[i][j] = isSynonymExact(expandedTokens1[i], expandedTokens2[j], wordNetTreshold, lemmaLookup);
			}
		
		for (int i=0; i<expandedTokens1.length; i++){
			double tempMax = 0;
			for (int j=0;j<expandedTokens2.length; j++) {
				//scoreMatrix[i][j] = isSynonymExact(expandedTokens[i], expandedTokens[j], getSynonymTreshold(_SYNONYM_TRESHOLD));

				if(scoreMatrix[i][j] > 0) {
					tempMax = Math.max(tempMax, scoreMatrix[i][j]);					
				}
			}
			if (tempMax > 0){
				sum += tempMax;
			}
		}
		
	  double x = sum / (1.0 * Math.max(expandedTokens1.length, expandedTokens2.length));
	  if (x > 1.0)
		  logger.error("sim value > 0!");

      return sum  / (1.0 * Math.max(expandedTokens1.length, expandedTokens2.length));
	}
	
		
	private final IdentifierNameTokeniserFactory factory = new IdentifierNameTokeniserFactory();	
	private final IdentifierNameTokeniser tokeniser;
 		                                             
	private int minimum(int a, int b, int c) {                            
		return Math.min(Math.min(a, b), c);                                      
	}                                                                            
	
	// standard levenshtein distance computation
	private double compareNormalizedLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        

		for (int i = 0; i <= lhs.length(); i++)                                 
			distance[i][0] = i;                                                  
		for (int j = 1; j <= rhs.length(); j++)                                 
			distance[0][j] = j;                                                  

		for (int i = 1; i <= lhs.length(); i++)                                 
			for (int j = 1; j <= rhs.length(); j++)                             
				distance[i][j] = minimum(                                        
						distance[i - 1][j] + 1,                                  
						distance[i][j - 1] + 1,                                  
						distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

		return (1.0 * distance[lhs.length()][rhs.length()]) / Math.max(lhs.length(), rhs.length());
	}

	// tokenise word using the intt library
	public String[] tokeniseIntt(String identifier) {
		if (identifier == null) identifier = "";
		String[] tokens = tokeniser.tokenise(identifier);
		if (tokens.length == 0)
			return new String[]{identifier};
		else
			return tokens;
	}
	
	public String toLowerCase(String term) {return term.toLowerCase();}
	
	// check stop word, using the basic list in this class
	private boolean isStopWord(String word) {return englishStopWords.contains(word);}

	// filter out words to be discarded: too short ones, digits only
	private boolean isDiscardWord(String word) {
		// remove words that are only digits - only considering integers
		boolean isAllDigits = true;
		for (int i=0; i<word.length(); i++)
			if (! Character.isDigit(word.charAt(i)))
				isAllDigits = false;
		
		boolean isTooShort = word.length() < minLength;
		
		return isAllDigits || isTooShort;
	}
	
	public String[] filterAndTokenize(String word){
		try{
			String[] tokens = tokeniseIntt(word);
			tokens = filter(tokens);
			if (tokens.length == 0) {
				tokens = new String[]{word};
			}
			return tokens;
		} catch(Exception ex) {
			ex.printStackTrace();
			return new String[]{word};
		}
	}
	
	private String[] filter(String[] tokens) {
		ArrayList<String> filteredTokens = new ArrayList<String>();
		for (String t: tokens)
		{
			String tl = t.toLowerCase();
			if (!isStopWord(tl) && !isDiscardWord(tl))
				filteredTokens.add(t);
		}
		String[] result = new String[filteredTokens.size()];
		filteredTokens.toArray(result);
		return result;
	}
	
	// standard Porter stemmer
	public String stem(String word){
		Stemmer stemmer = new Stemmer(); // how does this affect performance?
		stemmer.add(word.toCharArray(), word.length());
		stemmer.stem();
		return stemmer.toString();
	}
	
	// check if word is in wordnet
	public boolean isWordInWordnet(String word, POS pos){
		if (word == null || pos == null) return false;
		return dict.getIndexWord(word, pos) != null;
	}
	
	// This is the main method for comparing two words using various NLP techniques.
	public double isSynonymExact(String word1, String word2, double wordNetTreshold){
		if (word1 == null || word2 == null) 
			return 0.0;
		String lemma1, lemma2;
		if (word1.contains(".n.")) {
			lemma1 = word1.substring(0, word1.indexOf('.'));
		}
		else 
			lemma1 = word1;
		
		if (word2.contains(".n.")) {
			lemma2 = word2.substring(0, word2.indexOf('.'));
		}
		else
			lemma2 = word2;
		String lowerCaseWord1 = toLowerCase(lemma1);
		String lowerCaseWord2 = toLowerCase(lemma2);
		
		// if same lower case 
		if (lowerCaseWord1.equals(lowerCaseWord2)) return 1.0;
		
		// if same stems, consider synonym
		if (stem(lowerCaseWord1).equals(stem(lowerCaseWord2))) return 0.9; // WAS 1.0
		
		// assuming lemmatized before here
//		if (lemmatizer.getLemma(lowerCaseWord1).equals(lemmatizer.getLemma(lowerCaseWord1))) return 0.9;
		
		// levenshtein  
		double lev = compareNormalizedLevenshteinDistance(lowerCaseWord1, lowerCaseWord2);
		if (lev <= LevenshteinSimilarityTreshold)  {
			if (!(isWordInWordnet(lowerCaseWord1, POS.NOUN) && isWordInWordnet(lowerCaseWord2, POS.NOUN))) {
			lev = 1 - lev;
//			lev = 1.0; 
			} else
				lev = 0.0;
		}
		else lev = 0.0;

		// wordnet
		double wordnet = 0.0;
		if (wordNetTreshold > 0.0) {
			
						
			int sense1 = 1, //Integer.parseInt(word1.substring(word1.lastIndexOf('.')+1)),
					sense2 = 1;//Integer.parseInt(word2.substring(word2.lastIndexOf('.')+1));
			try{
				wordnet = lin.lin(lemma1, sense1, lemma2, sense2, "n");
				
			} catch(Exception ex) {
				wordnet = 0.0;
			}
			if (wordnet >= wordNetTreshold) {  
				if (wordnet == 1) wordnet = 0.95;
			}
			else 
				wordnet = 0.0;
			
			// end wordnet									
			
		}
		
		return Math.max(wordnet, lev);
	}

	public double isSynonymExact(String word1, String word2, double wordNetTreshold, HashMap<String, String> lemmaLookup){
		if (word1 == null || word2 == null) 
			return 0.0;
		String lemma1, lemma2;
		if (word1.contains(".n.")) {
			lemma1 = word1.substring(0, word1.indexOf('.'));
		}
		else 
			lemma1 = word1;
		
		if (word2.contains(".n.")) {
			lemma2 = word2.substring(0, word2.indexOf('.'));
		}
		else
			lemma2 = word2;
		String lowerCaseWord1 = toLowerCase(lemma1);
		String lowerCaseWord2 = toLowerCase(lemma2);
		
		// if same lower case 
		if (lowerCaseWord1.equals(lowerCaseWord2)) return 1.0;
		
		// if same stems, consider synonym
		if (stem(lowerCaseWord1).equals(stem(lowerCaseWord2))) return 0.9; // WAS 1.0
		
		// TODO check lemmas, if not PREPROCESS_LEMMATIZE
//		if (lemmatizer.getLemma(lowerCaseWord1).equals(lemmatizer.getLemma(lowerCaseWord1))) return 0.9;
		if (lemmaLookup.get(lowerCaseWord1).equals(lemmaLookup.get(lowerCaseWord2))) return 0.9;
		
		// levenshtein  
		double lev = compareNormalizedLevenshteinDistance(lowerCaseWord1, lowerCaseWord2);
		if (lev <= LevenshteinSimilarityTreshold)  {
			if (!(isWordInWordnet(lemmaLookup.get(lowerCaseWord1), POS.NOUN) && isWordInWordnet(lemmaLookup.get(lowerCaseWord2), POS.NOUN))) {
			lev = 1 - lev;
//			lev = 1.0; 
			} else
				lev = 0.0;
		}
		else lev = 0.0;

		// wordnet
		double wordnet = 0.0;
		if (wordNetTreshold > 0.0) {
			
			// begin wordnet				
			int sense1 = 1, 
					sense2 = 1;
			try{
				wordnet = lin.lin(lemmaLookup.get(lowerCaseWord1), sense1, lemmaLookup.get(lowerCaseWord2), sense2, "n");

			} catch(Exception ex) {
				wordnet = 0.0;
			}
			if (wordnet >= wordNetTreshold)  
				if (wordnet == 1) wordnet = 0.95;
				else 
					wordnet = 0.0;
			// end wordnet									
			
		}
		
		return Math.max(wordnet, lev);
	}
	
	// process all metamodel files in a given folder, tokenize all the model element names, and save them in serialized format for faster access later on. 
	public void precomputeTokenLookupTable(String sourceFolder, SERIALIZATION _SERIALIZATION) throws IOException{
		HashMap<Integer, String[]> tokenLookup = new HashMap<Integer, String[]>();
		File tokenFile = new File(sourceFolder + "/tokens.ser");
		
		LinkedHashSet<String> dictionary = new LinkedHashSet<String>();
		File dictFile = new File(sourceFolder + "/dictionary.ser");  
		
		HashMap<String, String> lemmaLookup = new HashMap<String, String>();
		File lemmaFile = new File(sourceFolder + "/lemma.ser");
		
		// double calculation if there are unigrams of different units (e.g. SimpleName + TypedName) TODO improve
		File[] featureFiles = new File(sourceFolder).listFiles(new FilenameFilter() { 
	         public boolean accept(File dir, String filename)
	         															// redundant: checks all the n-gram files (if present)
	              { return filename.endsWith(Constants.featureFileSuffix) /*&& filename.contains(_STRUCTURE.toString())*/; }
  	} );
				
		for(File ff : featureFiles)
		{							
			BufferedReader br = new BufferedReader(new FileReader(ff));

			String s = null;
			while((s = br.readLine()) != null) {
				Feature f;
				if (_SERIALIZATION == SERIALIZATION.PLAIN)
					f = PlainTextParser.parseText(s);					
				else // means JSON
					f = JSONParser.parseText(s);
				if (f != null)
					processFeatureForTokens(f, tokenLookup, lemmaLookup, dictionary);
			}
			br.close();
		}
		
		FileOutputStream fis = new FileOutputStream(tokenFile);  
		ObjectOutputStream s = new ObjectOutputStream(fis);          
		s.writeObject(tokenLookup);
		s.flush();
		s.close();		
		
		fis = new FileOutputStream(dictFile);  
		s = new ObjectOutputStream(fis);          
		s.writeObject(dictionary);
		s.flush();
		s.close();	
		
		fis = new FileOutputStream(lemmaFile);  
		s = new ObjectOutputStream(fis);          
		s.writeObject(lemmaLookup);
		s.flush();
		s.close();	
	}
	
	// process a feature to extract the tokens in all model element names found in the feature
	private void processFeatureForTokens(Feature f, HashMap<Integer, String[]> tokenLookup, HashMap<String, String> lemmaLookup, LinkedHashSet<String> dictionary) {
		if (f instanceof NGram) {
			NGram ng = (NGram) f;
			for (Feature subFeature : ng.getFeatures()) 
				processFeatureForTokens(subFeature, tokenLookup, lemmaLookup, dictionary);
		}
		else if (f instanceof NTreeApted) {
			NTreeApted nt = (NTreeApted) f;
			processFeatureForTokens(nt.aptedTree.getNodeData(), tokenLookup, lemmaLookup, dictionary);			
			for (int i=0; i<nt.aptedTree.getChildren().size(); i++) 
				processFeatureForTokens(nt.aptedTree.getChildren().get(i).getNodeData(), tokenLookup, lemmaLookup, dictionary);
		}
		else if (f instanceof NamedFeature) {
			String name = ((NamedFeature) f).getName();
			if (!dictionary.contains(name)){ // TODO suboptimal to check like this
				String[] expandedTokens = filter(tokeniseIntt(name)); // original vers. replaced with line below TODO unify
//				String[] expandedTokens = new String[]{name.toLowerCase()};
				for (int k=0; k<expandedTokens.length; k++) expandedTokens[k] = expandedTokens[k].toLowerCase();
				if (expandedTokens.length == 0){
					expandedTokens = new String[]{name.toLowerCase()};
				}
				
				dictionary.add(name);
				if (tokenLookup.containsKey(dictionary.size()-1))
					logger.debug("weird tokenlookup for " + name);
				else
					tokenLookup.put(dictionary.size()-1, expandedTokens);
				
				for (String token : expandedTokens){
					if (!lemmaLookup.containsKey(token))
						lemmaLookup.put(token, lemmatizer.getLemma(token));
				}
			}
		} 
		
		// no other option yet, assume AttributedNode has always name
	}
	
	HashMap<String, Double> tempLookup = new HashMap<String, Double>();
	
	// process all metamodel files in a given folder, compare all tokens with each other, compute their semantic similarity score and store them for faster access later on. 
	@SuppressWarnings("unchecked")
	public void precomputeSynonymLookupTable(String sourceFolder, double synonymThreshold) throws IOException{
		File file = new File(sourceFolder + "/tokens.ser"); 
		File lemmaFile = new File(sourceFolder + "/lemma.ser"); 
		HashMap<Integer, String[]> tokenLookup = new HashMap<Integer, String[]>();
		HashMap<String, String> lemmaLookup = new HashMap<String, String>();
		
		if (file.exists())
		{						  
			logger.info("found tokenisation file!!");
			FileInputStream fis = new FileInputStream(file);  
			ObjectInputStream s = new ObjectInputStream(fis);  
			try {
				tokenLookup = (HashMap<Integer,String[]>)s.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				s.close();
				return;
			}     
			s.close();
			
			fis = new FileInputStream(lemmaFile);  
			s = new ObjectInputStream(fis);  
			try {
				lemmaLookup = (HashMap<String, String>)s.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				s.close();
				return;
			} 							
			s.close();
		}
		
		Set<String> tokenSet = new HashSet<String>();
		for (String[] tokens : tokenLookup.values()){
			for (String token : tokens)
				if (!tokenSet.contains(token)) tokenSet.add(token);
		}
		logger.debug("token lookup table size:" + tokenLookup.keySet().size());
		logger.debug("token set size:" + tokenSet.size());
		logger.debug("starting synonym lookup table computation, this can take a while...");
		
		HashMap<Pair<Integer, Integer>, Double> synonymLookup = new HashMap<Pair<Integer, Integer>, Double>();
		
		ArrayList<Integer> keys = new ArrayList<Integer>();
		keys.addAll(tokenLookup.keySet());
		
		int size = keys.size();
		for (int i=0; i<size; i++){
			Integer word1 = keys.get(i);
			if (!TRACE_SYNONYMS && (i % (size/10) == 0)) // debug every 10% of the the progress  
				logger.debug("computing synonyms, progress " + (i / (size/10) * 10) + "%");
			for (int j=i; j<size; j++) {
				Integer word2 = keys.get(j);
				Pair<Integer, Integer> key = new Pair<Integer,Integer>(word1, word2);
				if (synonymLookup.containsKey(key)/* || synonymLookup.containsKey(reverseKey)*/)
					;
				else {
					double d = compareMultiwordWithTokenLookup(word1, word2, synonymThreshold, tokenLookup, lemmaLookup);
					if (d>0 && d >= synonymThreshold) { // only because of the wordnet
						if (TRACE_SYNONYMS) {
							double d0 = compareMultiwordWithTokenLookup(word1, word2, 0.0, tokenLookup, lemmaLookup);
							if (!word1.equals(word2) && (! (synonymThreshold > 0) || d != d0))
								logger.trace(Arrays.toString(tokenLookup.get(word1))
									+ "\t\t" + Arrays.toString(tokenLookup.get(word2)) + "\t\t" + d);
						}
							
						// TODO control what to do when wordnet returns 1.0 as synonym value
						synonymLookup.put(key, d);
					}
				}
			}
		}
		
		String suffix = synonymThreshold>0?"_WNET":"_NOWNET";
		File synFile = new File(sourceFolder + "/syn" + suffix + ".ser");  
		
		FileOutputStream fis = new FileOutputStream(synFile);  
		ObjectOutputStream s = new ObjectOutputStream(fis);          
		s.writeObject(synonymLookup);
		s.flush();
		s.close();		
	}


	// optional lemmatize string if flags for pre-processing tokenization and lemmatization are on
	public String lemmatizeIfFlagSet(String base, boolean preToken, boolean preLemma) {
		String result = lemmatizeIfFlagSet_aux(base, preToken, preLemma);
		if (result == null || result.equals("")) 
			result = "-";
		return result;
	}
	
	public String lemmatizeIfFlagSet_aux(String base, boolean preToken, boolean preLemma) {
		if (preToken) {
			if (preLemma)
				return Lemmatizer.getInstance().getLemma(base.toLowerCase()).trim();
			else
				return base;
		}
		else {
			if (preLemma) {
				String[] expandedTokens = tokeniseIntt(base); 
				for (int i=0; i<expandedTokens.length; i++) {
	
						expandedTokens[i] = Lemmatizer.getInstance().getLemma(expandedTokens[i].toLowerCase()).trim();
				}
				String result = "";
				if (expandedTokens.length == 0)
					logger.error("ERROR:" + base);
				for (String token : expandedTokens)
					result += token.toLowerCase() + "_";
				return result.substring(0, result.length()-1);
			} else
				return base;
		}
	}
	
	//////// utility classes below for handling strings
	
	public String cleanse(String text){
		if (text == null) { text = "null"; return text; } 
		text = text.replace(",", "-");
		text = text.replace(";", "-");
		return text;
	}
	
	public String handleEmptyString(String s){
		if (s != null && s.equals(""))
			return "\"\"";
		else 
			return s;
	}
	
	// testTokenise("Convertkm2m"); doesnt work
	public void testTokenise(String line){
		String[] tokens = tokeniser.tokenise(line);       
        for (int i = 0; i < tokens.length; i++) {
        	logger.debug(tokens[i]);
        	if (i < tokens.length - 1) {
        		;
        	}
        } 		
	}
	
}
