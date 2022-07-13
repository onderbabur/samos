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

package nl.tue.set.samos.extract;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.tue.set.samos.common.Constants;
import nl.tue.set.samos.common.Pair;
import nl.tue.set.samos.common.Util;
import nl.tue.set.samos.common.enums.STRUCTURE;
import nl.tue.set.samos.common.enums.UNIT;
/**
 * This is the main basic feature extractor for Ecore metamodels. It iterates over a given metamodel and extracts features according to the given configuration.
 * 
 *  scope	[MODEL | EPACKAGE | ECLASS] the scope/granularity of feature extraction
 * 
 *  unit	[NAME | ATTRIBUTED] the unit of extraction per model element
 *  
 *  structure [UNIGRAM | BIGRAM | NTREE] the structure of the extracted features
*/
public class EcoreExtractorImpl extends IExtractor {

	final Logger logger = LoggerFactory.getLogger(EcoreExtractorImpl.class);	
	
	@Override
	// iterates over the containment tree of a metamodel and returns all model elements of a certain type
	public List<Object> getAllContainedObjectsByType(File f, String scope) {	
		try {			
			Resource metamodel_resource;
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", 
					new EcoreResourceFactoryImpl());

			metamodel_resource = resourceSet.getResource(
					URI.createFileURI(f.getAbsolutePath()),true);

			List<EObject> objects = new ArrayList<EObject>();
			
			EList<EObject> topContents = metamodel_resource.getContents();		
			for(EObject o: topContents)
				getAllEObjects(o, objects);
			
			List<Object> results = new ArrayList<Object>();
			for (EObject eo : objects) {
				if (scope.equals("MODEL") || eo.eClass().getName().equalsIgnoreCase(scope))
					results.add(eo);
			}
			return results;
		} catch (Exception ex) {
			logger.error("Could not process " + f.getAbsolutePath());
			ex.printStackTrace();
		} catch (Error er) {
			logger.error("Could not process " + f.getAbsolutePath());
			er.printStackTrace();
		}
		
		return null;
	}
	
	// recursively find all the EObjects in a model and accumulate them in a list. 
	public void getAllEObjects(EObject object, List<EObject> objects){		
		// filter
		if (isFiltered(object))
			return;
		
		objects.add(object);
		List<Object> topContents = getNextElements(object);
		for (Object o: topContents)
			getAllEObjects((EObject)o, objects);
	}
	
	// at the moment filtering these three types for clustering and clone detection purposes. 
	private boolean isFiltered(Object object) {
		return (object instanceof EAnnotation || object instanceof EGenericType || object instanceof ETypeParameter);
	}
		
	@Override
	// extract the immediate features given an object (i.e. metamodel element) in the metamodel
	public List<String> extractFeatures(Object object, UNIT _UNIT, STRUCTURE _STRUCTURE) {
		
		// filter
		if (isFiltered(object))
			return null;
		
		if (object instanceof ENamedElement) {
			ArrayList<String> expandedFeatures = new ArrayList<String>();
			if (!PREPROCESS_TOKENIZE) {
				expandedFeatures.addAll(generateFeatureAux((EObject)object, _UNIT, _STRUCTURE));
			}
			else { // need to expand, again works only for unigrams. Not planned for n>2-grams at the moment. 
				expandedFeatures.addAll(generateFeaturesExpand((EObject)object, _UNIT, _STRUCTURE));
			}

			return expandedFeatures;				
		}			

		return null;
	}

	@Override
	// get all the directly connected elements in the metamodel structure
	public List<Object> getNextElements(Object object) {
		
		// filter
		if (isFiltered(object))
			return new ArrayList<Object>();
		
		List<Object> nextElements = new ArrayList<Object>();
		// get all contained elements
		List<EObject> contents = ((EObject)object).eContents();
		nextElements.addAll(contents);
		return nextElements;
	}
	
			
	// expansion only for unigrams, not planned for n>2-grams at the moment. 
	public ArrayList<String> generateFeatures(EObject object, UNIT _UNIT, STRUCTURE _STRUCTURE){
		
		ArrayList<String> features = new ArrayList<String>();
		if (!PREPROCESS_TOKENIZE) {
			features.addAll(generateFeatureAux(object, _UNIT, _STRUCTURE));
		}
		else { // need to expand, again works only for unigrams, not planned for n>2-grams at the moment. 
			features.addAll(generateFeaturesExpand(object, _UNIT, _STRUCTURE));
		}
		return features;
	}
	
	public ArrayList<String> generateFeatureAux(EObject object, UNIT _UNIT, STRUCTURE _STRUCTURE){
		ArrayList<String> results = new ArrayList<String>();
		if (_STRUCTURE == STRUCTURE.UNIGRAM) {
			String result = Constants.NG + generateSimpleFeature(object, _UNIT, Util.isJSON(_STRUCTURE));
			results.add(result);
			return results;
		}
		else if (_STRUCTURE == STRUCTURE.BIGRAM) {	
				String result = generateFeatureFromChildren(object, _UNIT, Util.isJSON(_STRUCTURE));
				// workaround at the moment, generateFeaturesFromChildren should not return null. 
				if (result != null) {
					String[] splits = result.split("\n");
					results.addAll(Arrays.asList(splits));
				}
				return results;
				
		} else { // NTREE
			// workaround with timeout in case we have resolving problems etc.			
			String result = null;
			{
				final Duration timeout = Duration.ofSeconds(10);
				ExecutorService executor = Executors.newSingleThreadExecutor();
				final Future<String> handler = executor.submit(new Callable<String>() {
				    @Override
				    public String call() throws Exception {
				        return (String) generateFeatureFromChildren(object, _UNIT, Util.isJSON(_STRUCTURE));
				    }
				});

				try {
					result = handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					e.printStackTrace();
				    handler.cancel(true);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				executor.shutdownNow();
			}

			if (result != null) {
				String[] splits = result.split("\n");
				results.addAll(Arrays.asList(splits));
			}
			return results;
		}
	}

	// get the supertypes of a given EClass instance 
	public List<EClass> getSupertypes(EObject object) {
		List<EClass> supertypes = new ArrayList<EClass>();
		if (object instanceof EClass){
			if (((EClass)object).getESuperTypes().size() > 0) {			
				for (EClass superClass: ((EClass)object).getESuperTypes()){			
					// need to check for cycles, also checking max parent size FIXME
					// this is also wrong because just checking parent NODES, not edges!!! 		
//					boolean hasSuperClass = false;
					if (superClass != null) {
						// HACK
						String superClassName = superClass.getName();					
						// if no real name, try to get it from proxy uri
						if (superClassName == null || superClassName.equals("")) {
							String[] tokens = EcoreUtil.getURI(superClass).toString().split("//"); 
							superClassName = tokens[tokens.length-1];
							if (superClassName == null || superClassName.equals("")) // if the above didn't help
								logger.debug("Despite the workaround, NULL type for " + ((ETypedElement)object).getEType());
							else { // replace the child with a dummy EClass
								EClass dummyClass = EcoreFactory.eINSTANCE.createEClass();
								dummyClass.setName(superClassName);
								superClass = dummyClass;
//								hasSuperClass = true;
							}
						}
						supertypes.add(superClass);
						// ENDHACK
					}
				}
			}
		}
		return supertypes;
	}
	
	// get the directly connected elements along with their edges, given a model element
	public Pair<List<Object>, List<String>> getNextElementsWithEdges(EObject object) {
		Pair<List<Object>, List<String>> children = new Pair<List<Object>, List<String>>(new ArrayList<Object>(), new ArrayList<String>());
		
		// add contained elements, common for any type of EObject
		List<Object> contents = getNextElements(object);					
		if (contents != null){
			// Note: don't need to check for cycles, as containment is always acyclic
			if (contents.size() > 0) {
				for (Object content : contents) {
					if (!(isFiltered(content))) {
						children.x.add(content);
						children.y.add(Constants.CONTAINS);
					}
				}					
			}
		}

		// iteration w.r.t. supertypes only for EClass
		List<EClass> supertypes = getSupertypes(object);		
		if (supertypes.size() > 0) {
			for (EClass supertype : supertypes) {
				children.x.add(supertype);
				children.y.add(Constants.HAS_SUPERTYPE);											
			}
		}

		// add EExceptions thrown for EOperation 
		if (object instanceof EOperation){
			EOperation op = (EOperation) object;
			if (op.getEExceptions().size() > 0) {
				for (EClassifier ec : op.getEExceptions()) {
					children.x.add(ec);
					children.y.add(Constants.THROWS);	
				}
			}
		}
		
		return children;
	}
	
	// generate structural features from a given model element. Can output plain text or JSON
	public String generateFeatureFromChildren(EObject object, UNIT _UNIT, boolean isJSON) {
		StringBuffer buffer = new StringBuffer();
		Pair<List<Object>, List<String>> children = getNextElementsWithEdges(object);
		HashMap<String, ArrayList<JSONObject>> JSONMap = new HashMap<String, ArrayList<JSONObject>>();
		for (int i=0; i<children.x.size(); i++){
			if(!isJSON) {
				buffer.append(Constants.NG)
					.append(generateSimpleFeature(object, _UNIT, isJSON))
					.append(Constants.NGRAM_SEP)
					.append((String) Util.generateSimpleType(children.y.get(i), false))
					.append(Constants.NGRAM_SEP)
					.append((String) generateSimpleFeature((EObject)children.x.get(i), _UNIT, false))
					.append("\n");
			} else {		
				String key = children.y.get(i);
				if (!JSONMap.containsKey(key))
					JSONMap.put(key, new ArrayList<JSONObject>());
				JSONMap.get(key).add(Util.createJSONNode(generateSimpleFeature((EObject)children.x.get(i), _UNIT, true)));	
			}
		}
		
		// for JSON, convert the hashmap objects into JSON
		if (isJSON) {
			ArrayList<JSONObject> topContents = new ArrayList<JSONObject>();
			for (String key: JSONMap.keySet()) 
				topContents.add(Util.createJSONNode(Util.generateSimpleType(Constants.CONTAINS, true), JSONMap.get(key).toArray()));
			
			// if production failed (somehow), return null
			if (topContents.size() == 0) 
				return null;
			
			JSONObject finalObject = Util.createJSONNode(generateSimpleFeature(object, _UNIT, true), topContents.toArray());
			return finalObject.toString();
		} else {				
			String result = buffer.toString();
			// prune the last newline, if ngrams
			if (result != null) {
				if (result.equals(""))
					result = null;
				else if (result.endsWith("\n"))
					result = result.substring(0, result.length()-1);
			}
			return result;
		}		
	}

	public ArrayList<String> generateFeaturesExpand(EObject object, UNIT _UNIT, STRUCTURE _STRUCTURE){		
		// currently always N-gram, intended for n-grams at the moment (e.g. not for ntrees)
		ArrayList<String> simpleFeatures = generateSimpleFeaturesExpand(object, _UNIT);
		for (int i=0; i<simpleFeatures.size(); i++)
			simpleFeatures.set(i, Constants.NG + simpleFeatures.get(i));
		return simpleFeatures;
	}

	// generate a simple feature (i.e. unigram) given a model element. can output plain text or json.
	public Object generateSimpleFeature(EObject object, UNIT _UNIT, boolean isJSON){		
		JSONObject obj = null;
		StringBuffer feature = null;
		
		// initialize data structure
		if (isJSON) {
			obj = new JSONObject();
			if (_UNIT != UNIT.ATTRIBUTED) obj.put("ftype", Util.getFtypeString(_UNIT, isJSON));
		} else {
			feature = new StringBuffer(); 
			if (_UNIT != UNIT.ATTRIBUTED) feature.append(Util.getFtypeString(_UNIT, isJSON));
		}
		
		switch(_UNIT) {
		case NAME:
			try{
				String name = nlp.lemmatizeIfFlagSet(((ENamedElement)object).getName(), PREPROCESS_TOKENIZE, PREPROCESS_LEMMATIZE);
				if (isJSON)
					obj.put("name", name);
				else 
					feature.append(name);
			} catch(Exception ex){
				ex.printStackTrace();
			}
			break;
		case TYPEDNAME:
			try{ 
				String name = nlp.lemmatizeIfFlagSet(((ENamedElement)object).getName(), PREPROCESS_TOKENIZE, PREPROCESS_LEMMATIZE);
				String type = object.eClass().getName();

				if (isJSON) {
					obj.put("type", type);
					obj.put("name", name);
				}
				else {
					feature.append(type + Constants.ATTRIB_SEP + name);
				}
			} catch(Exception ex){
				ex.printStackTrace();
			}
			break;
		case TYPEDVALUEDNAME: 
			try {
				String name = nlp.lemmatizeIfFlagSet(((ENamedElement)object).getName(), PREPROCESS_TOKENIZE, PREPROCESS_LEMMATIZE);
				String type = object.eClass().getName();
				String eType = null;
				{
					if (/*INCLUDE_TYPES && */(object instanceof EReference || object instanceof EOperation || object instanceof EAttribute || object instanceof EParameter)){
						ETypedElement typed = (ETypedElement) object;
						EClassifier typeObject = typed.getEType();
						boolean hasType = false;

						if (typeObject != null) {
							// HACK
							String typeName = typeObject.getName();

							// if no real name, try to get it from proxy uri
							if (typeName == null || typeName.equals("")) {
								String[] tokens = EcoreUtil.getURI(typeObject).toString().split("//"); 
								typeName = tokens[tokens.length-1];
								if (typeName == null || typeName.equals("")) // if the above didn't help
									logger.debug("Despite the workaround, NULL type for " + ((ETypedElement)object).getEType());
								else { // replace the child with a dummy EClass
									EClass dummyClass = EcoreFactory.eINSTANCE.createEClass();
									dummyClass.setName(typeName);
									typeObject = dummyClass;
									hasType = true;
								}
							} else 
								hasType = true;				
							// ENDHACK
						} 

						if (hasType) {	
							eType = typeObject.eClass().getName();
						} else {
							eType = "-";
						}
					}
//						else result += Constants.ATTRIB_SEP + Constants.ATTRIB_SEP;
				}
				
				if (isJSON) {
					obj.put("name", name); 
					obj.put("type", type); 
					obj.put("eType", eType);
				}
				else {
					feature.append(type).
						append(Constants.ATTRIB_SEP).
						append(name).
						append(Constants.ATTRIB_SEP).
						append(eType);
				}								
			} catch(Exception ex){
				ex.printStackTrace();
			}
			break;	
		case ATTRIBUTED:
			if (isJSON) 
				obj = (JSONObject) extractAttributedFeatureFromSingleObject(object, isJSON);
			else 
				feature.append((String) extractAttributedFeatureFromSingleObject(object, isJSON));
			break;
		default:
			break;				
		}
		
		if (isJSON)
			return obj;
		else {
			// HACK
			if (_UNIT != UNIT.ATTRIBUTED) {
				String featureString = feature.substring(0, feature.length());
				return featureString;
			} else {
				String featureString = feature.substring(0, feature.length()-1);
				return featureString;
			}
		}
	}
	
	public ArrayList<String> generateSimpleFeaturesExpand(EObject object, UNIT _UNIT){
		ArrayList<String> results = new ArrayList<String>();
		String name;
		String[] expandedTokens;
		
		switch(_UNIT) {
		case NAME:
			name = ((ENamedElement)object).getName();
			if (name == null)
				logger.debug("null name!");
			expandedTokens = nlp.filterAndTokenize(name);
			for (String token : expandedTokens)
				results.add(Constants.SN + 
						nlp.lemmatizeIfFlagSet(token, PREPROCESS_TOKENIZE, PREPROCESS_LEMMATIZE));
			break;
		default:			
			break;				
		}
		
		return results;
	}
	
	// FIXME e.g. default instanceValueName == null should not be printed
	private boolean isAttributeNonDefault(String attributeName, Object value) {
		if (attributeName.equals("name") || attributeName.equals("type") || attributeName.equals("eType")) 
			return true;
		Object defaultValue = Util.getDefaultAttributeValue(attributeName); 
		if ((defaultValue == null || defaultValue.equals("null")) && (value == null || value.equals("null"))) return false;
		if ((defaultValue == null || defaultValue.equals("null")) || (value == null || value.equals("null"))) return true;
		return (!defaultValue.equals(value));
	} 
	
	private void append(String attr, Object value, StringBuffer feature, JSONObject obj, boolean isJSON) {
		if (isAttributeNonDefault(attr, value)) {
			if (isJSON)
				obj.put(attr, value);
			else 
				feature.append(attr + Constants.ATTRIB_MAP_SEP + value + Constants.ATTRIB_SEP);
		}
	}
	
	// generates a full-fledged attributed feature (containing all details of the node) given a model element 
	// TODO check if cleanse is being applied consistently
	private Object extractAttributedFeatureFromSingleObject(EObject object, boolean isJSON) {		
		JSONObject obj = null;
		StringBuffer feature = null;
		
		// initialize data structure
		if (isJSON) {
			obj = new JSONObject();			
			obj.put("ftype", "Attributed");
		} else {
			feature = new StringBuffer(); 
			feature.append(Constants.AN);
		}		
		
		// type
		append("type", nlp.cleanse(object.eClass().getName()), feature, obj, isJSON);
		
		if (object instanceof ENamedElement) {
			ENamedElement eNamedElement = ((ENamedElement)object);
			append("name", nlp.lemmatizeIfFlagSet(nlp.cleanse(eNamedElement.getName()), PREPROCESS_TOKENIZE, PREPROCESS_LEMMATIZE), feature, obj, isJSON);			
		}
		
		if (object instanceof ETypedElement) {
			ETypedElement eTypedElement = ((ETypedElement)object);
			append("ordered", eTypedElement.isOrdered(), feature, obj, isJSON);
			append("unique", eTypedElement.isUnique(), feature, obj, isJSON);
			append("lowerBound", eTypedElement.getLowerBound(), feature, obj, isJSON);
			append("upperBound", eTypedElement.getUpperBound(), feature, obj, isJSON);
			append("many", eTypedElement.isMany(), feature, obj, isJSON);
			append("required", eTypedElement.isRequired(), feature, obj, isJSON);
						
			if (! (object instanceof EReference)) // TODO extract proper type, that is both type class and type name
				append("eType", 
	(eTypedElement.getEType()==null?"null":nlp.cleanse(eTypedElement.getEType().getName())), feature, obj, isJSON);
			else {
				
				EClassifier typeObject = null;
				
				if (((EReference)object).isResolveProxies() == true)
				{
					final Duration timeout = Duration.ofSeconds(10);
					ExecutorService executor = Executors.newSingleThreadExecutor();
					final Future<EClassifier> handler = executor.submit(new Callable<EClassifier>() {
					    @Override
					    public EClassifier call() throws Exception {
					        return (EClassifier) ((EReference)object).getEType();
					    }
					});

					try {
						typeObject = handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						e.printStackTrace();
					    handler.cancel(true);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					executor.shutdownNow();
				}
				else
					typeObject = ((EReference)object).getEType();
				
				String typeName = "";
				if (typeObject != null) {
					// TODO get name properly
					typeName = typeObject.getName();
					
					// if no real name, try to get it from proxy uri
					if (typeName == null || typeName.equals("")) {
						String[] tokens = EcoreUtil.getURI(typeObject).toString().split("//"); 
						typeName = tokens[tokens.length-1];
						if (typeName == null || typeName.equals("")) { // if the above didn't help
							logger.debug("Despite the workaround, NULL type for " + ((ETypedElement)object).getEType());
							typeName = "null";
						}
					}					
				}
				append("eType", nlp.cleanse(typeName), feature, obj, isJSON);
			}
		}
		
		if (object instanceof EClassifier) {
			EClassifier eClassifier = ((EClassifier)object);
			append("instanceClassName", nlp.cleanse(eClassifier.getInstanceClassName()), feature, obj, isJSON);
			append("instanceTypeName", nlp.cleanse(eClassifier.getInstanceTypeName()), feature, obj, isJSON);
			// instanceClass
			// defaultValue
			
		}
		
		if (object instanceof EStructuralFeature) {
			EStructuralFeature eStructuralFeature = ((EStructuralFeature)object);			
			append("changeable", eStructuralFeature.isChangeable(), feature, obj, isJSON);
			append("volatile", eStructuralFeature.isVolatile(), feature, obj, isJSON);
			append("transient", eStructuralFeature.isTransient(), feature, obj, isJSON);
			append("defaultValueLiteral", nlp.cleanse(nlp.handleEmptyString(eStructuralFeature.getDefaultValueLiteral())), feature, obj, isJSON);
//			feature += "defaultValue" + Constants.ATTRIB_MAP_SEP + eStructuralFeature.getDefaultValue() + Constants.ATTRIB_SEP;
			append("unsettable", eStructuralFeature.isUnsettable(), feature, obj, isJSON);
			append("derived", eStructuralFeature.isDerived(), feature, obj, isJSON);
		}
		
		if (object instanceof EClass) {
			EClass eClass = ((EClass)object);
			append("abstract", eClass.isAbstract(), feature, obj, isJSON);
			append("interface", eClass.isInterface(), feature, obj, isJSON);
		}			
						
		if (object instanceof EAttribute) {
			EAttribute eAttribute = ((EAttribute)object);
			append("iD", eAttribute.isID(), feature, obj, isJSON);
		}
		
		// eKeys - ignoring for now
		
		if (object instanceof EReference) {
			EReference eReference = ((EReference)object);
			append("containment", eReference.isContainment(), feature, obj, isJSON);
			append("container", eReference.isContainer(), feature, obj, isJSON);
			append("resolveProxies", eReference.isResolveProxies(), feature, obj, isJSON);
			append("eOpposite", (eReference.getEOpposite()==null?"null":nlp.cleanse(eReference.getEOpposite().getName())), feature, obj, isJSON);
		}
		
		// EOperation - nothing special
		// exceptions, generic type
		
		// EParameter - nothing special
		
		if (object instanceof EDataType) {
			EDataType eDataType = ((EDataType)object);
			append("serializable", eDataType.isSerializable(), feature, obj, isJSON);
		}
		
		if (object instanceof EEnumLiteral) {
			EEnumLiteral eEnumLiteral = ((EEnumLiteral)object);
			append("value", eEnumLiteral.getValue(), feature, obj, isJSON);
			append("literal", nlp.cleanse(eEnumLiteral.getLiteral()), feature, obj, isJSON);
			//feature += "instance" + Constants.ATTRIB_MAP_SEP + eEnumLiteral.getInstance() + Constants.ATTRIB_SEP;
		}
		if (isJSON)
			return obj;
		else {
			String featureString = feature.toString();
			return featureString;
		}
		
	}

	@Override
	// find out the name of a model element
	public String getName(Object o) {
		if (o instanceof ENamedElement) 
			return ((ENamedElement)o).getName();
		else 
			return null;
	}
}