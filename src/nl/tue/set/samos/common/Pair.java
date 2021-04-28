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

package nl.tue.set.samos.common;
import java.io.Serializable;

public class Pair<X, Y> implements Serializable { 
		private static final long serialVersionUID = -6084234587273566816L;
		public final X x; 
		public final Y y; 
		public Pair(){x = null; y=null;} // TODO workaround
		public Pair(X x, Y y) { 
			this.x = x; 
			this.y = y; 
		} 
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			if (o instanceof Pair<?, ?>)
			{
				Pair<X, Y> target = (Pair<X,Y>) o;
				if (this.x.equals(target.x) && this.y.equals(target.y) )
						return true;
				else 
					return false;
			}
			else 
				return false;
		}
		@Override
		public String toString() {
			return x + "-" + y;
		}
		@Override
		public int hashCode() {
			return 31 * x.hashCode() + y.hashCode(); 
		}
	}	