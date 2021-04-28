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


import node.Node;

public class AptedNodeCustom<D> extends Node<D> {



  public AptedNodeCustom(D nodeData) {
		super(nodeData);
	}

/**
   * Returns a string representation of the tree in bracket notation.
   *
   * <p>IMPORTANT: Works only for nodes storing {@link node.StringNodeData}
   * due to using {@link node.StringNodeData#getLabel()}.
   *
   * @return tree in bracket notation.
   */
  public String toString() {
    String res = (new StringBuilder("{")).append((getNodeData().toString())).toString();
    for(Node<D> child : getChildren()) {
      res = (new StringBuilder(String.valueOf(res))).append(child.toString()).toString();
    }
    res = (new StringBuilder(String.valueOf(res))).append("}").toString();
    return res;
  }


}