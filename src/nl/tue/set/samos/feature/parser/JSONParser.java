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

package nl.tue.set.samos.feature.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.feature.AttributedNode;
import nl.tue.set.samos.feature.Feature;
import nl.tue.set.samos.feature.NGram;
import nl.tue.set.samos.feature.NTreeApted;
import nl.tue.set.samos.feature.SimpleFeature;
import nl.tue.set.samos.feature.SimpleName;
import nl.tue.set.samos.feature.SimpleType;
import nl.tue.set.samos.feature.TypedName;
import nl.tue.set.samos.feature.TypedValuedName;
import nl.tue.set.samos.feature.compare.AptedNodeCustom;
import node.Node;

/**
 * Simple parser class for processing JSON feature files
 */
public class JSONParser {
	
	static final Logger logger = LoggerFactory.getLogger(JSONParser.class);
	
	public static Feature parseText(String text) {
		if (text == null)
			return null;
		text = text.trim();
		if (text.equals(""))
			return null;
		
		JSONObject jsonObject = new JSONObject(text); 
		
		return parseJSON(jsonObject);
	}
	
	public static final boolean MERGE_EGDE_NODES = true;
	public static final boolean SORT_NODES = false;
	
	public static AptedNodeCustom<Feature> merge(AptedNodeCustom<Feature> aptedTree){
		AptedNodeCustom<Feature> newAptedTree = new AptedNodeCustom<Feature>(null);
		newAptedTree.setNodeData(aptedTree.getNodeData());
		merge(aptedTree, newAptedTree);		
		return newAptedTree;
	}
	
	public static void merge(AptedNodeCustom<Feature> aptedTree, AptedNodeCustom<Feature> newAptedTree){
		Feature currentFeature = aptedTree.getNodeData();
		assert currentFeature instanceof NGram;
		if (((NGram)currentFeature).get(0) instanceof SimpleType) {
			logger.error("// should not be so");			
		} else {
			//newAptedTree.setNodeData(aptedTree.getNodeData());
			for (Node<Feature> subtree : aptedTree.getChildren()){
				NGram simpleType = (NGram) subtree.getNodeData();
				assert ((NGram)simpleType).get(0) instanceof SimpleType;
				assert simpleType.n == 1;
				for (Node<Feature> subsubtree : subtree.getChildren()){
					NGram realFeature = (NGram) subsubtree.getNodeData();
					assert realFeature.n == 1;
					ArrayList<SimpleFeature> featureList = new ArrayList<SimpleFeature>();
					featureList.add(simpleType.get(0)); featureList.add(realFeature.get(0));
					NGram ngram = new NGram(featureList);		
					AptedNodeCustom<Feature> newNode = new AptedNodeCustom<Feature>(ngram);
					merge((AptedNodeCustom<Feature>) subsubtree, newNode);
					newAptedTree.addChild(newNode);					
				}
			}
		}
		//return newAptedTree;
	}
	
	public static Feature parseJSON(JSONObject jsonObject) {
		
		if (jsonObject.getString("ftype").equals("NTree")) {			
			AptedNodeCustom<Feature> aptedTree = parseJSONAsNode(jsonObject);
			if (MERGE_EGDE_NODES)
				aptedTree = merge(aptedTree);
			NTreeApted tree = new NTreeApted(aptedTree);
			if (SORT_NODES)
				tree.sort();
			return tree;
		}
		else if (jsonObject.getString("ftype").equals("SimpleName")) {
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			features.add(new SimpleName(jsonObject.getString("name")));
			return new NGram(features);			
		}
		else if (jsonObject.getString("ftype").equals("SimpleType")) {
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			features.add(new SimpleType(jsonObject.getString("type")));	
			return new NGram(features);							
		}
		else if (jsonObject.getString("ftype").equals("TypedName")) {
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			features.add(new TypedName(jsonObject.getString("type"), jsonObject.getString("name")));
			return new NGram(features);				
		}
		else if (jsonObject.getString("ftype").equals("TypedValueName")) {
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			features.add(new TypedValuedName(
					jsonObject.getString("type"), 
					jsonObject.getString("name"),
					jsonObject.optString("typeValueType"),
					jsonObject.optString("typeValueValue")));
			return new NGram(features);				
		}
		else if (jsonObject.getString("ftype").equals("Attributed")) {
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			HashMap<String, Object> keyValueMap = new HashMap<String, Object>();
			for (String key: jsonObject.keySet())
				keyValueMap.put(key, jsonObject.get(key));
			features.add(new AttributedNode(keyValueMap));
			return new NGram(features);
		}
		else {
			logger.error("Problem with parsing object " + jsonObject.toString());
			return null;
		}		
	}
	
	public static AptedNodeCustom<Feature> parseJSONAsNode(JSONObject jsonObject) {
		
		if (jsonObject.getString("ftype").equals("NTree")) {
			
			Feature parentFeature = parseJSON(jsonObject.getJSONObject("node"));
			AptedNodeCustom<Feature> node = new AptedNodeCustom<Feature>(parentFeature);
			JSONArray contents = jsonObject.getJSONArray("contents");
			Vector<Node<Feature>> childNodes = node.getChildren();
			for (Object o : contents){ 
				childNodes.add(parseJSONAsNode((JSONObject) o)); 
				}
			return node;
		}
		else {
			logger.error("Problem with parsing object " + jsonObject.toString());
			return null;
		}		
	}
	
	public static void main(String[] args){
		// examples below, not deleting for testing purposes later
//		String s = "[NG] [AN] type:EClass,name:LocatedElement,instanceClassName:null,instanceTypeName:null,abstract:true,interface:false;[AN] type:contains;[AN] type:EAttribute,name:location,ordered:false,unique:false,lowerBound:1,upperBound:1,many:false,required:true,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false";		
//		String s = "[NG] [SN] asd; [ST] edge; [SN] qwe";
		//String s = "[NT] [AN] type:EClass,name:located_element,instanceClassName:null,instanceTypeName:null,abstract:true,interface:false;[ST] contains;[AN] type:EOperation,name:operation_black_hawk,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Boolean;[ST] contains;[AN] type:EAttribute,name:location,ordered:false,unique:false,lowerBound:1,upperBound:1,many:false,required:true,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:comment_before,ordered:true,unique:false,lowerBound:0,upperBound:-1,many:true,required:false,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:comment_after,ordered:true,unique:false,lowerBound:0,upperBound:-1,many:true,required:false,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:be_important,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Boolean,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:true,iD:false;[ST] contains;[AN] type:EAttribute,name:key,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Integer,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:true;[ST] contains;[AN] type:EReference,name:movie_reference_lotr,ordered:true,unique:true,lowerBound:0,upperBound:5,many:true,required:false,eType:LordOfTheRings,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,containment:false,container:false,resolveProxies:true,eOpposite:null;[ST] contains;[AN] type:EReference,name:book_reference_sw,ordered:true,unique:true,lowerBound:2,upperBound:2,many:true,required:true,eType:StarWars,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,containment:false,container:false,resolveProxies:true,eOpposite:null;[ST] supertypeOf;[AN] type:EClass,name:lord_of_the_ring,instanceClassName:null,instanceTypeName:null,abstract:false,interface:false";
		//String s = "[NT] [TN] EReference,book_reference_sw <[NT] [ST] typeOf <[NT] [TN] EClass,star_war<>>>";
		//String s = "[NT] [TN] EClass,located_element <[NT] [ST] contains<[NT] [TN] EOperation,operation_black_hawk<>;[NT] [TN] EAttribute,location<>;[NT] [TN] EAttribute,comment_before<>;[NT] [TN] EAttribute,comment_after<>;[NT] [TN] EAttribute,be_important<>;[NT] [TN] EAttribute,key<>;[NT] [TN] EReference,movie_reference_lotr<>;[NT] [TN] EReference,book_reference_sw<>;>[NT] [ST] supertypeOf<[NT] [TN] EClass,lord_of_the_ring<>;>>";
		//String s = "{\"node\":{\"ftype\":\"TypedName\",\"name\":\"book_reference_sw\",\"type\":\"EReference\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"typeOf\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"TypedName\",\"name\":\"star_war\",\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[]}]}]}";
		//String s = "{\"node\":{\"ftype\":\"TypedName\",\"name\":\"located_element\",\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"contains\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"TypedName\",\"name\":\"operation_black_hawk\",\"type\":\"EOperation\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"location\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"comment_before\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"comment_after\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"be_important\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"key\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"movie_reference_lotr\",\"type\":\"EReference\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"TypedName\",\"name\":\"book_reference_sw\",\"type\":\"EReference\"},\"ftype\":\"NTree\",\"contents\":[]}]},{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"supertypeOf\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"TypedName\",\"name\":\"lord_of_the_ring\",\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[]}]}]}";
//		String s = "{\"node\":{\"ftype\":\"TypedName\",\"name\":\"comment_before\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"typeOf\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"TypedName\",\"name\":\"integer\",\"type\":\"EDataType\"},\"ftype\":\"NTree\",\"contents\":[]}]}]}";
//		String s2 = "{\"node\":{\"ftype\":\"TypedName\",\"name\":\"comment_before\",\"type\":\"EAttribute\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"typeOf\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"TypedName\",\"name\":\"string\",\"type\":\"EDataType\"},\"ftype\":\"NTree\",\"contents\":[]}]}]}";
		String s = "{\"node\":{\"ftype\":\"Attributed\",\"name\":\"bib_tex\",\"type\":\"EPackage\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"SimpleType\",\"type\":\"contains\"},\"ftype\":\"NTree\",\"contents\":[{\"node\":{\"ftype\":\"Attributed\",\"name\":\"located_element\",\"abstract\":true,\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"Attributed\",\"name\":\"lord_of_the_ring\",\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[]},{\"node\":{\"ftype\":\"Attributed\",\"name\":\"star_war\",\"type\":\"EClass\"},\"ftype\":\"NTree\",\"contents\":[]}]}]}";
		
		Feature f = parseText(s);
		
		logger.debug(f.toString());
		logger.debug(""+((NTreeApted)f).n);
	}
}
