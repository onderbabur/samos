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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.SYNONYM_TRESHOLD;
import nl.tue.set.samos.common.enums.UNIT;

public class Util {
	
	static final Logger logger = LoggerFactory.getLogger(Util.class);
	
	public static double getSynonymTreshold(SYNONYM_TRESHOLD _SYNONYM_TRESHOLD){
		switch(_SYNONYM_TRESHOLD){
		case SYN80: return 0.8;
		case SYN90: return 0.9;
		case NO_WORDNET: return 0.0;
		default: return 1.0;
		}
	}
	
	public static String generateIdFromParams(Parameters params){
		return generateFromParams(params, "-");
	} 
	
	public static String generateCsvFromParams(Parameters params){
		return generateFromParams(params, ",");
	} 
	
	public static String generateFromParams(Parameters params, String separator) {
		return params._UNIT + separator + params._STRUCTURE + separator + params._WEIGHT + separator + params._IDF + separator + params._TYPE_MATCH +separator + params._SYNONYM + separator + params._SYNONYM_TRESHOLD + separator + params._NGRAM_CMP +  separator + params._CTX_MATCH + separator + params._FREQ;
	}
	
	public static boolean isJSON(STRUCTURE _STRUCTURE) {
		return (_STRUCTURE == STRUCTURE.NTREE); 
	}
	
	public static String getFtypeString(UNIT _UNIT, boolean isJSON) {
		switch(_UNIT){
			case NAME: return isJSON?"SimpleName":Constants.SN;
			case TYPEDNAME: return isJSON?"TypedName":Constants.TN;
			case TYPEDVALUEDNAME: return isJSON?"TypedValuedName":Constants.TV;
			default: logger.error("Error: not recignized simple unit type: " + _UNIT);
		}
		return null;
	}
	
	public static Object generateSimpleType(String type, boolean isJSON) {
		if (isJSON) {
			JSONObject obj = new JSONObject();
			obj.put("ftype", "SimpleType");
			obj.put("type", type);
			return obj;
		}
		else
			return Constants.ST + type;
	}
	
	public static JSONObject createJSONNode(Object value) {
		return createJSONNode(value, new JSONObject[]{}); // create with empty content
	}
	
	public static JSONObject createJSONNode(Object value, Object[] contents) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("ftype", "NTree");
		jsonObject.put("node", value);
		if (contents != null)
			jsonObject.put("contents", contents);
		return jsonObject;
	}
	
	// not necessarily the default on emf, but mostly so
	public static Object getDefaultAttributeValue(String attributeName){
		switch(attributeName){
		case "ordered": return Boolean.TRUE;
		case "unique": return Boolean.FALSE; // unlike EMF
		case "lowerBound": return new Integer(1);
		case "upperBound": return new Integer(1);
		case "many": return Boolean.FALSE;
		case "required": return Boolean.FALSE;
		case "instanceClassName": return "null";
		case "instanceTypeName": return "null";
		case "changeable": return Boolean.TRUE;
		case "volatile": return Boolean.FALSE;
		case "transient": return Boolean.FALSE;
		case "defaultValueLiteral": return "null";
		case "unsettable": return Boolean.FALSE;
		case "derived": return Boolean.FALSE;
		case "abstract": return Boolean.FALSE;
		case "interface": return Boolean.FALSE;
		case "iD": return Boolean.FALSE;
		case "containment": return Boolean.TRUE;
		case "container": return Boolean.FALSE; //?
		case "resolveProxies": return Boolean.FALSE;
		case "eOpposite": return "null";
		case "serializable": return Boolean.FALSE;
		case "value": return "null";
		case "literal": return "null";
		
		default: break;
		}
		
		return Boolean.FALSE;
	}
	
	public static int getTotalAttributeCount(String type){
		switch(type){
		case "EPackage": return 2;
		case "EClass": return 4;
		case "EDataType": return 3;
		case "EEnumLiteral": return 4;
		case "EEnum": return 5; // TODO ???
		case "EAttribute": return 7;
		case "EReference": return 10;
		case "EOperation": return 6;
		case "EParameter": return 6;
		default: logger.error("forgot to add? " + type);		
		}
		return 1;
	}
}
