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

import nl.tue.set.samos.common.Pair;

public class TypedValuedName extends SimpleFeature implements TypedFeature, NamedFeature{
	private static final long serialVersionUID = 6172206750271494217L;
	private Pair<String, String> pair;
	private TypedName typeValue;
	public TypedValuedName(String type, String name, String valueType, String valueTypeName){
		this.pair = new Pair<String, String>(type, name);
		this.typeValue = new TypedName(valueType, valueTypeName);
	}
	
	public String getType() {return pair.x;}
	public String getName() {return pair.y;}
	public TypedName getTypeValue() {return typeValue;}
	
	@Override
	public String toString(){return pair.toString() + "-typeOf-" + typeValue.toString();}
	@Override
	public int hashCode(){return pair.hashCode() + typeValue.hashCode();}
	@Override
	public boolean equals(Object o){
		if (o instanceof TypedValuedName)
		{
			TypedValuedName target = (TypedValuedName) o;
			return this.pair.equals(target.pair) && this.typeValue.equals(target.getTypeValue());
		}
		else 
			return false;
	}
}
