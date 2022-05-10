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


import nl.tue.set.samos.common.enums.CTX_MATCH;
import nl.tue.set.samos.common.enums.EXTRACT_STR;
import nl.tue.set.samos.common.enums.FREQ;
import nl.tue.set.samos.common.enums.IDF;
import nl.tue.set.samos.common.enums.NGRAM_CMP;
import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.SYNONYM;
import nl.tue.set.samos.common.enums.SYNONYM_TRESHOLD;
import nl.tue.set.samos.common.enums.TYPE_MATCH;
import nl.tue.set.samos.common.enums.UNIT;
import nl.tue.set.samos.common.enums.VSM_MODE;
import nl.tue.set.samos.common.enums.WEIGHT;

public class Parameters {
	public EXTRACT_STR _EXTRACT_STR;
	public UNIT _UNIT;
	public STRUCTURE _STRUCTURE;
	public WEIGHT _WEIGHT;
	public IDF _IDF;
	public TYPE_MATCH _TYPE_MATCH;
	public SYNONYM _SYNONYM;
	public SYNONYM_TRESHOLD _SYNONYM_TRESHOLD;
	public NGRAM_CMP _NGRAM_CMP;
	public CTX_MATCH _CTX_MATCH;
	public FREQ _FREQ;
	
	public VSM_MODE _VSM_MODE;

	public Parameters(EXTRACT_STR _EXTRACT_STR, UNIT _UNIT, STRUCTURE _STRUCTURE, WEIGHT _WEIGHT, IDF _IDF,
			TYPE_MATCH _TYPE_MATCH, SYNONYM _SYNONYM, SYNONYM_TRESHOLD _SYNONYM_TRESHOLD,
			NGRAM_CMP _NGRAM_CMP, CTX_MATCH _CTX_MATCH, FREQ _FREQ, VSM_MODE _VSM_MODE) {
		this._EXTRACT_STR = _EXTRACT_STR;
		this._UNIT = _UNIT;
		this._STRUCTURE = _STRUCTURE;
		this._WEIGHT = _WEIGHT;
		this._IDF = _IDF;
		this._TYPE_MATCH = _TYPE_MATCH;
		this._SYNONYM = _SYNONYM;
		this._SYNONYM_TRESHOLD = _SYNONYM_TRESHOLD;
		this._NGRAM_CMP = _NGRAM_CMP;
		this._CTX_MATCH = _CTX_MATCH;
		this._FREQ = _FREQ;
		
		this._VSM_MODE = _VSM_MODE; 
	}
	
	public Parameters(UNIT _UNIT, STRUCTURE _STRUCTURE) {
		this(null, _UNIT, _STRUCTURE, null, null, null, null, null, null, null, null, null);
	}
}