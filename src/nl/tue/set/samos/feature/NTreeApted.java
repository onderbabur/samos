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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.feature.compare.AptedNodeCustom;
import node.Node;


// HACK, first attempt that is just 1 parent + x children leaves, not generalizable to depth > 2
public class NTreeApted extends AggregateFeature{ 
	
		/**
	 * 
	 */
	private static final long serialVersionUID = -8269732096154199861L;
	
	final Logger logger = LoggerFactory.getLogger(NTreeApted.class);

		public AptedNodeCustom<Feature> aptedTree;
//		private final SimpleFeature parentFeature;
//		private final ArrayList<Feature> childFeatures;
		
		public final int n;
		
		public NTreeApted(AptedNodeCustom<Feature> aptedTree) {this.aptedTree = aptedTree; n = 2;}
		public NTreeApted(Feature parent, ArrayList<AptedNodeCustom<Feature>> children) { 
			aptedTree = new AptedNodeCustom<Feature>(parent);
			for (AptedNodeCustom<Feature> child : children){
				aptedTree.addChild(child);
			}
			n = 2;//1 + childFeatures.size();
		}
		
		public int size() {return size(aptedTree);}
		public int size(AptedNodeCustom<Feature> tree) {
			int size = 0;
			if (tree.getNodeData() != null) size++;
			// regular size measurement
//			for (Node<Feature> child : tree.getChildren())
//				size += size((AptedNodeCustom)child);
			
			// excluding supertypes
			for (Node<Feature> child : tree.getChildren()) {
				Object nodeData = ((AptedNodeCustom<Feature>)child).getNodeData(); 
				if (nodeData instanceof NGram) {
					NGram ng = (NGram) nodeData;
					if (ng.get(0) instanceof SimpleType && 
							!((SimpleType)ng.get(0)).getType().equals("supertypeOf"))
						size += size((AptedNodeCustom<Feature>)child);	
					else
						;
				} 
				else
					logger.error("WHAT ELSE THAN AN NGRAM??? " + nodeData);
			}
			
			return size;
		}
		
//		public ArrayList<Feature> getChildFeatures() {
//			return childFeatures;
//		}
//		
//		public SimpleFeature getParent() {return parentFeature;}
//		public Feature getChild(int i) {return childFeatures.get(i);}
//		
//		public Feature get(int i) {
//			if (i == 0)
//				return getParent();
//			else
//				return childFeatures.get(i-1);
//		}
		
		// HACK sorts implemented here, also no iteration/recursion into lower depths
		public void sort(){
			bubbleSort(this.aptedTree.getChildren());
		}
		
		private int compare(Node<Feature> lhs, Node<Feature> rhs) {
			NGram ngLHS = (NGram) lhs.getNodeData();
			NGram ngRHS = (NGram) rhs.getNodeData();
			
			int cmp1 = ngLHS.get(0).toString().compareTo(ngRHS.get(0).toString()); 
			if (cmp1 != 0) return cmp1;
			
			int cmp2 = ((TypedFeature) ngLHS.get(1)).getType().compareTo(((TypedFeature) ngRHS.get(1)).getType());
			if (cmp2 != 0) return cmp2;
			
			int cmp3 = ((NamedFeature) ngLHS.get(1)).getName().compareTo(((NamedFeature) ngRHS.get(1)).getName());
			if (cmp3 != 0) return cmp3;
			
			int cmp4 = new Integer(((AttributedNode) ngLHS.get(1)).hashCodeSubset()).compareTo(((AttributedNode) ngRHS.get(1)).hashCodeSubset());
			return cmp4;
		}
		
		public void bubbleSort(List<Node<Feature>> nodesToSort) {

			Node<Feature> temp;
	        for (int i = 0; i < nodesToSort.size() - 1; i++) {
	            for (int j = 1; j < (nodesToSort.size() - i); j++) {
	                if (compare(nodesToSort.get(j - 1), nodesToSort.get(j)) > 0) {
	                    //swap the elements!
	                    temp = nodesToSort.get(j - 1);
	                    nodesToSort.set(j - 1, nodesToSort.get(j));
	                    nodesToSort.set(j, temp);
	                }

	            }
	        }
	    }
		
		public boolean equals(Object o) {
			if (o instanceof NTreeApted)
			{
//				NTreeApted target = (NTreeApted) o;
//				if (this.n != target.n) return false;
//				
//				if(!this.getParent().equals(target.getParent()))
//				
//				for (int i=0; i<this.childFeatures.size(); i++)
//					if (!this.getChild(i).equals(target.getChild(i)))
//						return false;
//				return true;
				
				NTreeApted target = (NTreeApted) o;
				return this.toString().equals(target.toString());
			}
			else 
				return false;
		}
		
		public String toString() {
			return aptedTree.toString();
//			String s = "";
//			s += "[[" + parentFeature.toString() + "]]->";
//			for (Feature f : childFeatures)
//				s += "(" + f.toString() + ")--"; 
//			return s.substring(0, s.length()-2);
		}

//		public int hashCode(){
//			int code = 0;
//			for (int i=0; i<n; i++)
//				code += 2^(i+1) * getChild(i).hashCode(); 
//			code += 2^(n+1) * getParent().hashCode();
//			return code;
//		}
		public int hashCode() { return this.toString().hashCode();}
	}	