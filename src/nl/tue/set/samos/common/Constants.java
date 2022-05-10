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

public class Constants {
	
	
	public static final String configFile = "resources/config.properties";
	
	public static final String featureFileSuffix = ".features"; 	
		
	public static final String NG = "[NG] ";
	public static final String NT = "[NT] ";
	public static final String SN = "[SN] ";
	public static final String ST = "[ST] ";
	public static final String TN = "[TN] ";
	public static final String TV = "[TV] ";
	public static final String AN = "[AN] ";
	public static final String NGRAM_SEP = ";";
	public static final String ATTRIB_SEP = ",";
	public static final String ATTRIB_MAP_SEP = ":";
	
	public static final String CONTAINS = "contains";
	public static final String IS_TYPE_OF = "typeOf";
	public static final String HAS_SUPERTYPE = "supertypeOf";
	public static final String THROWS = "throws";
	public static final String ASSOCIATES = "associates";
	public static final String ASSOCIATED_BY = "associatedBy";
	public static final String GENERAL = "generalizedBy";
}
