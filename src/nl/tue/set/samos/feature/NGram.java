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

/**
 * Aggregate feature type which can contain a sequence of features. 
 */
public class NGram extends AggregateFeature{ 
		/**
	 * 
	 */
	private static final long serialVersionUID = -2478487566317481439L;
		private final ArrayList<SimpleFeature> features;
		public ArrayList<SimpleFeature> getFeatures() {
			return features;
		}
		public final int n;
		public NGram(ArrayList<SimpleFeature> features) { 
			this.features = features;
			n = features.size();
		}
		
		public SimpleFeature get(int i) {return features.get(i);}
		
		public boolean equals(Object o) {
			if (o instanceof NGram)
			{
				NGram target = (NGram) o;
				if (this.n != target.n) return false;
				
				for (int i=0; i<this.n; i++)
					try {
					if (!this.features.get(i).equals(target.features.get(i)))
						return false;
					} catch(Exception ex) {ex.printStackTrace(); return false;}
				return true;
			}
			else 
				return false;
		}
		public String toString() {
			String s = "";
			for (SimpleFeature f : features)
				s += "(" + f.toString() + ")--"; 
			return s.substring(0, s.length()-2);
		}
		public int hashCode(){
			try{
				int code = 0;
				for (int i=0; i<n; i++)
					code += 2^(i+1) * features.get(i).hashCode();
				return code;
			} catch(Exception ex) { 
				ex.printStackTrace(); return 0;
			}
			
		}
	}	