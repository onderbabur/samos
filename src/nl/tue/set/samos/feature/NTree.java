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
import java.util.ArrayList;


// HACK, first attempt that is just 1 parent + x children leaves, not generalizable to depth > 2
public class NTree extends AggregateFeature{ 
		/**
	 * 
	 */
	private static final long serialVersionUID = 2543062747898858123L;
		private final SimpleFeature parentFeature;
		private final ArrayList<Feature> childFeatures;
		public ArrayList<Feature> getChildFeatures() {
			return childFeatures;
		}
		public final int n;
		public NTree(SimpleFeature parent, ArrayList<Feature> children) { 
			this.parentFeature = parent;
			this.childFeatures = children;
			n = 2;//1 + childFeatures.size();
		}
		
		public SimpleFeature getParent() {return parentFeature;}
		public Feature getChild(int i) {return childFeatures.get(i);}
		
		public Feature get(int i) {
			if (i == 0)
				return getParent();
			else
				return childFeatures.get(i-1);
		}
		
		public boolean equals(Object o) {
			if (o instanceof NTree)
			{
				NTree target = (NTree) o;
				if (this.n != target.n) return false;
				
				if(!this.getParent().equals(target.getParent()))
				
				for (int i=0; i<this.childFeatures.size(); i++)
					if (!this.getChild(i).equals(target.getChild(i)))
						return false;
				return true;
			}
			else 
				return false;
		}
		public String toString() {
			String s = "";
			s += "[[" + parentFeature.toString() + "]]->";
			for (Feature f : childFeatures)
				s += "(" + f.toString() + ")--"; 
			return s.substring(0, s.length()-2);
		}

		public int hashCode(){
			int code = 0;
			for (int i=0; i<n; i++)
				code += 2^(i+1) * getChild(i).hashCode(); 
			code += 2^(n+1) * getParent().hashCode();
			return code;
		}
	}	