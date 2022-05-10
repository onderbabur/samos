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

package nl.tue.set.samos.feature;

import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributedNode extends SimpleFeature implements TypedFeature, NamedFeature{
	private static final long serialVersionUID = 6172206750271494217L;
	final Logger logger = LoggerFactory.getLogger(AttributedNode.class);

	private HashMap<String, Object> keyValueMap;
	public AttributedNode(HashMap<String, Object> keyValueMap){
		this.keyValueMap = keyValueMap;
	}
	public AttributedNode(){
		keyValueMap = new HashMap<String, Object>();
	}
	public AttributedNode(String key, Object value) {
		keyValueMap = new HashMap<String, Object>();
		keyValueMap.put(key, value);
	}
	
	public boolean hasAttribute(String key) { return keyValueMap.containsKey(key);}
	public Set<String> getAttributes(){return keyValueMap.keySet();}
	public String getAttribute(String key) {
		if (hasAttribute(key)) 
			//return (String) keyValueMap.get(key);			
			return keyValueMap.get(key).toString();
		else
			logger.error("ERROR: trying to get nonexistent key: " + key);
		return null;
	}
	
	public void put(String key, Object value) {
		if (hasAttribute(key)) 
			logger.error("ERROR: trying to put " + key + " twice!!!!");
		else 
			keyValueMap.put(key, value);
	}
	
	
	@Override public String getType() {return getAttribute("type");}
	@Override public String getName() {return getAttribute("name");}
	// TODO Assume always has name
	//public boolean hasName() {return hasAttribute("name");}
	
	public int size() {return keyValueMap.keySet().size();}
	
	public String toStringFull(){
		Set<String> keys = keyValueMap.keySet();
		String result = "";
		for (String key: keys) {
			result += "<" + key + "-" + keyValueMap.get(key) + ">";
		}
		return result.substring(0, result.length()-1);
	}
	
	@Override
	public String toString(){
		return "<" + getType() + "-" + getName() + "-" + hashCodeSubset() + ">";
	}
	
	@Override
	public int hashCode(){
		// TODO better hash code
		Set<String> keys = keyValueMap.keySet();
		int result = 0;
		for (String key: keys) {
			result += keyValueMap.get(key).toString().toLowerCase().hashCode();
		}
		return result;
	}
	
	public int hashCodeSubset(){
		Set<String> keys = keyValueMap.keySet();
		int result = 0;
		for (String key: keys) {
			if (!key.equals("type") && !key.equals("name"))
				result += keyValueMap.get(key).toString().toLowerCase().hashCode();
		}
		return result;
	}
	
	@Override
	public boolean equals(Object o){		
		if (o instanceof AttributedNode)
		{					
			AttributedNode target = (AttributedNode) o;								
			Set<String> keys = keyValueMap.keySet();
			
			if (this.keyValueMap.keySet().size() != target.keyValueMap.keySet().size()) return false;
			
			for(String key : keys) {
				if (!target.hasAttribute(key)) return false;
				if (!getAttribute(key).equalsIgnoreCase(target.getAttribute(key))) return false;
			}			
			
			// TODO: improve, redundant computation
			if (!this.toString().equalsIgnoreCase(target.toString())) {
				return false;
			}
			return true;
		}
		else 
			return false;
	}
}
