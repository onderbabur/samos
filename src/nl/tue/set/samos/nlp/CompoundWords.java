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

package nl.tue.set.samos.nlp;


import java.util.ArrayList;

/*
	'CompoundWords'

	What this class does:

		Returns a list of all possible variations of a compound word - that is, 'a word' that is made up
		of words separated by underscores _ , hyphens - , or spaces. For example, if we have, say,
		"apple-tree", we will get "apple-tree", "apple_tree", and "apple tree"

	Why do we have to do this?

		WordNet does not appear to have a standardised version of compound words. For example, it will
		accept "active-surface_agent" but will not accept "active-surface-agent", or "active_surface_agent" but will
		accept "new york city" and "newcastle-upon-tyne". And so, we have to try generate all combinations if we
		want to be exacting about 'looking up' compound words in WordNet.

	David Hope, 2008, University Of Sussex
*/
public class CompoundWords
{

	private String[] 					editor 		=	null;
	private ArrayList<String>	store			=	null;
	private ArrayList<String>	temp			=	null;
	private ArrayList<String>	separators	=	null;
	public CompoundWords()
	{
		store 		=	new ArrayList<String>();
		temp 		=	new ArrayList<String>();
		separators	=	new ArrayList<String>();
		separators.add("-");
		separators.add("_");
		separators.add(" ");
	}

	public ArrayList<String> getCompounds(String word)
	{
		ArrayList<String> compounds = new ArrayList<String>();
		store.clear();
		editor = word.split("[-_\\s]");
		for(int i = 0; i < editor.length; i++)
		{
			word = editor[i];
			temp.clear();

			if(i == editor.length - 1)
			{
				for(String stored : store)
				{
					compounds.add(stored + word);
				}
			}
			else
			{
				for(String sep : separators)
				{
					if(!store.isEmpty())
					{
						for(String stored : store)
						{
							temp.add(stored + word + sep);
						}
					}
					else
					{
						temp.add(word + sep);
					}
				}
				if(store.isEmpty())
				{
					store.addAll(temp);
				}
				else
				{
					store.clear();
					store.addAll(temp);
				}
			}
		}
		return ( compounds );
	}

}