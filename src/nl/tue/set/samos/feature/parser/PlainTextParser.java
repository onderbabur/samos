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

package nl.tue.set.samos.feature.parser;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.common.Constants;
import nl.tue.set.samos.feature.AttributedNode;
import nl.tue.set.samos.feature.Feature;
import nl.tue.set.samos.feature.NGram;
import nl.tue.set.samos.feature.NTree;
import nl.tue.set.samos.feature.SimpleFeature;
import nl.tue.set.samos.feature.SimpleName;
import nl.tue.set.samos.feature.SimpleType;
import nl.tue.set.samos.feature.TypedName;
import nl.tue.set.samos.feature.TypedValuedName;

public class PlainTextParser {
	
	static final Logger logger = LoggerFactory.getLogger(PlainTextParser.class);

	public static Feature parseText(String text) {
		if (text == null)
			return null;
		text = text.trim();
		if (text.equals(""))
			return null;
		
		if (text.startsWith(Constants.NG)) {
			text = text.substring(Constants.NG.length());
			
			String[] tokens = null;
			tokens = text.split(Constants.NGRAM_SEP);
			assert (tokens.length > 0);
			
			ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
			for (int i=0; i<tokens.length; i++)
			{
				tokens[i] = tokens[i].trim();
				features.add((SimpleFeature) parseText(tokens[i]));
			}
			return new NGram(features);
		}
		else if (text.startsWith(Constants.SN)) {
			text = text.substring(Constants.SN.length());
			
			text = text.trim();
			
			return new SimpleName(text);			
		}
		else if (text.startsWith(Constants.ST)) {
			text = text.substring(Constants.ST.length());
			
			text = text.trim();
			
			return new SimpleType(text);			
		}
		else if (text.startsWith(Constants.TN)) {
			text = text.substring(Constants.TN.length());
			
			String[] tokens = null;
			tokens = text.split(Constants.ATTRIB_SEP);
			assert (tokens.length == 2);	
			
			return new TypedName(tokens[0], tokens[1]);			
		}
		else if (text.startsWith(Constants.TV)) {
			text = text.substring(Constants.TV.length());
			
			String[] tokens = null;
			tokens = text.split(Constants.ATTRIB_SEP);
			//assert (tokens.length == 4);	
			if (tokens.length == 4)
				return new TypedValuedName(tokens[0], tokens[1], tokens[2], tokens[3]);
			else // length = 2
				return new TypedValuedName(tokens[0], tokens[1], "", "");
		}
		else if (text.startsWith(Constants.AN)) {
			text = text.substring(Constants.AN.length());
			
			String[] tokens = null;
			tokens = text.split(Constants.ATTRIB_SEP);
			assert (tokens.length > 0);
			assert (tokens.length/2 == 0);
			
			HashMap<String, Object> keyValueMap = new HashMap<String, Object>();			
			for (int i=0; i<tokens.length; i++)
			{
				String[] terms = tokens[i].split(Constants.ATTRIB_MAP_SEP);
				assert(terms.length == 2);
				try{
					keyValueMap.put(terms[0], terms.length == 1?"null":terms[1]);
				} catch(Exception ex){
					ex.printStackTrace();
				}
			}
			return new AttributedNode(keyValueMap);
		} 
		else if (text.startsWith("[NT] ")){
			logger.error("Trees not supported in plain text parser!!!");
			return null;
		}
		else {
			logger.error("Problem with parsing line " + text);
			return null;
		}		
	}
	

	public static void main(String[] args){
//		String s = "[NG] [AN] type:EClass,name:LocatedElement,instanceClassName:null,instanceTypeName:null,abstract:true,interface:false;[AN] type:contains;[AN] type:EAttribute,name:location,ordered:false,unique:false,lowerBound:1,upperBound:1,many:false,required:true,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false";		
//		String s = "[NG] [SN] asd; [ST] edge; [SN] qwe";
		//String s = "[NT] [AN] type:EClass,name:located_element,instanceClassName:null,instanceTypeName:null,abstract:true,interface:false;[ST] contains;[AN] type:EOperation,name:operation_black_hawk,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Boolean;[ST] contains;[AN] type:EAttribute,name:location,ordered:false,unique:false,lowerBound:1,upperBound:1,many:false,required:true,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:comment_before,ordered:true,unique:false,lowerBound:0,upperBound:-1,many:true,required:false,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:comment_after,ordered:true,unique:false,lowerBound:0,upperBound:-1,many:true,required:false,eType:String,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:false;[ST] contains;[AN] type:EAttribute,name:be_important,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Boolean,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:true,iD:false;[ST] contains;[AN] type:EAttribute,name:key,ordered:true,unique:true,lowerBound:0,upperBound:1,many:false,required:false,eType:Integer,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,iD:true;[ST] contains;[AN] type:EReference,name:movie_reference_lotr,ordered:true,unique:true,lowerBound:0,upperBound:5,many:true,required:false,eType:LordOfTheRings,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,containment:false,container:false,resolveProxies:true,eOpposite:null;[ST] contains;[AN] type:EReference,name:book_reference_sw,ordered:true,unique:true,lowerBound:2,upperBound:2,many:true,required:true,eType:StarWars,changeable:true,volatile:false,transient:false,defaultValueLiteral:null,unsettable:false,derived:false,containment:false,container:false,resolveProxies:true,eOpposite:null;[ST] supertypeOf;[AN] type:EClass,name:lord_of_the_ring,instanceClassName:null,instanceTypeName:null,abstract:false,interface:false";
		//String s = "[NT] [TN] EReference,book_reference_sw <[NT] [ST] typeOf <[NT] [TN] EClass,star_war<>>>";
		//String s = "[NT] [TN] EClass,located_element <[NT] [ST] contains<[NT] [TN] EOperation,operation_black_hawk<>;[NT] [TN] EAttribute,location<>;[NT] [TN] EAttribute,comment_before<>;[NT] [TN] EAttribute,comment_after<>;[NT] [TN] EAttribute,be_important<>;[NT] [TN] EAttribute,key<>;[NT] [TN] EReference,movie_reference_lotr<>;[NT] [TN] EReference,book_reference_sw<>;>[NT] [ST] supertypeOf<[NT] [TN] EClass,lord_of_the_ring<>;>>";
		String s = "[NT] [TN] EClass,located_element <[NT] [ST] contains<[NT] [TN] EOperation,operation_black_hawk<>;[NT] [TN] EAttribute,location<>;>[NT] [ST] supertypeOf<[NT] [TN] EClass,lord_of_the_ring<>;>>";
		
		Feature f = parseText(s);
		
		logger.debug(f.toString());
		logger.debug(""+((NTree)f).n);
	}
}
