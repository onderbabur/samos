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

package nl.tue.set.samos.feature.compare;

import costmodel.CostModel;
import nl.tue.set.samos.feature.Feature;
import nl.tue.set.samos.feature.NGram;
import node.Node;
import node.StringNodeData;

/**
 * This is a unit-nost model defined on string labels.
 *
 * @see CostModel
 * @see StringNodeData
 */
 // TODO: Use a label dictionary to encode string labels with integers for
 //       faster rename cost computation.
public class FeatureCostModel implements CostModel<Feature> {
	
	private FeatureComparator comparator;

	public FeatureCostModel(FeatureComparator comparator){ this.comparator = comparator;}
	
  /**
   * Calculates the cost of deleting a node.
   *
   * @param n a node considered to be deleted.
   * @return {@code 1} - a fixed cost of deleting a node.
   */
  public float del(Node<Feature> n) {
//	  if (n.getChildren().size() == 1){
//		  Node<Feature> child = n.getChildren().get(0);
//		  if (child.getNodeData() instanceof NGram){
//			  NGram ng = (NGram) child.getNodeData();
//			  if (ng.n == 2 && ng.get(0) instanceof SimpleType && 
//					  ((SimpleType)ng.get(0)).getType().equals("typeOf"))
//				  return 2.0f;
//		  }
//	  }
		  
    return 1.0f;
  }

  /**
   * Calculates the cost of inserting a node.
   *
   * @param n a node considered to be inserted.
   * @return {@code 1} - a fixed cost of inserting a node.
   */
  public float ins(Node<Feature> n) {
//	  if (n.getChildren().size() == 1){
//		  Node<Feature> child = n.getChildren().get(0);
//		  if (child.getNodeData() instanceof NGram){
//			  NGram ng = (NGram) child.getNodeData();
//			  if (ng.n == 2 && ng.get(0) instanceof SimpleType && 
//					  ((SimpleType)ng.get(0)).getType().equals("typeOf"))
//				  return 2.0f;
//		  }
//	  }
	  
    return 1.0f;
  }

  /**
   * Calculates the cost of renaming the label of the source node to the label
   * of the destination node.
   *
   * @param n1 a source node for rename.
   * @param n2 a destination node for rename.
   * @return {@code 1} if labels of renamed nodes are equal, and {@code 0} otherwise.
   */
  public float ren(Node<Feature> n1, Node<Feature> n2) {
//	if ((n1.getNodeData() instanceof TypedName) && (n2.getNodeData() instanceof TypedName)) {
//		TypedName tn1 = (TypedName) n1.getNodeData();
//		TypedName tn2 = (TypedName) n2.getNodeData();
//		
//		float typeMultiplier = (tn1.getType().equals(tn2.getType()))?1.0f:0.5f;
//		float synMultiplier = (tn1.getName().equals(tn2.getName()))?1.0f:0.0f;
//		float sim = (typeMultiplier * synMultiplier);
//		
//		return 1 - sim;
//	}
//	else return 0.0f;    
	  
	  Feature f1 = n1.getNodeData();
	  Feature f2 = n2.getNodeData();
	  float f = 1.0f - (float) comparator.compareNGram((NGram) f1, (NGram) f2);
	  
	  // TODO: what if completely different? should have 2 as distance? 1x delete + 1x insert? 
	  // also need to change the denominator in the sim. formula then. 
	  
//	  if (f != 1 && f != 0 && f < 0.30) 
//		  logger.debug(f1.toString() + "\t" + f2.toString() + "\t" + f);
	  return f;
	  
  }
}