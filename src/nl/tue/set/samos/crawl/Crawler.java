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

package nl.tue.set.samos.crawl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crawler {
	
	static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	//public static final String ATL_ZOO_ECORE_URL_PATTERN = "https://gforge.inria.fr/scm/viewvc.php/atlantic-zoos/AtlantEcore/%s?view=co"; // server down
	
	// moved this as launch configuration parameter
	//public static final String ATL_ZOO_ECORE_URL_PATTERN = "https://raw.githubusercontent.com/atlanmod/atlantic-zoo/main/AtlantEcore/%s"; // github project
	
	
	public static final String[] BIBLIO_CONF_MODEL_NAMES = {
			"BibTeX.ecore",
			"BibTeX1.1.ecore",
			"BIBTEXML.ecore",
			"Book.ecore",
			"cmt.owl.ecore",
			"Cocus.owl.ecore",
			"Conference.owl.ecore",
			"confious.owl.ecore",
			"confOf.owl.ecore",
			"crs_dr.owl.ecore",
			"edas.owl.ecore",
			"ekaw.owl.ecore",
			"iasted.owl.ecore",
			"MICRO.owl.ecore",
			"OpenConf.owl.ecore",
			"paperdyne.owl.ecore",
			"PCS.owl.ecore",
			"sigkdd.owl.ecore"};
	public static final String BIBLIO_CONF_FOLDER = "data/atlzoo/biblioAndConf/";
	
	// moved this as launch configuration parameter
	//public static final String ZENODO_DATASET_PATH = "https://zenodo.org/record/2585456/files/manualDomains.zip?download=1";

	public static void main(String[] args) throws MalformedURLException, IOException {
//		crawl();
		
	}	
	
	public static void crawl(String targetFolder, String urlPattern) throws MalformedURLException, IOException {
			if (targetFolder.equals("zenodo"))				
				crawlZenodo(targetFolder, urlPattern);
			else if (targetFolder.equals("atlzoo"))
				crawlATLZoo(targetFolder, urlPattern);
			else
				logger.error("Did not recognize the dataset to be crawled: " + targetFolder);
		}
	public static void crawlATLZoo(String targetFolder, String urlPattern) throws MalformedURLException, IOException {
	
		if (!targetFolder.endsWith("/")) targetFolder += "/";
		for (String modelName: BIBLIO_CONF_MODEL_NAMES) {
			logger.info(String.format(urlPattern, new Object[]{modelName}));
			FileUtils.copyURLToFile(new URL(String.format(urlPattern, new Object[]{modelName})), 
				new File("data/" + targetFolder + modelName));
		}
	}
	
	public static void crawlZenodo(String targetFolder, String urlPattern) throws MalformedURLException, IOException{
		if (!targetFolder.endsWith("/")) targetFolder += "/";
		
		String path = urlPattern;
		File tempZip = new File("data/" + targetFolder, "temp.zip");
		FileUtils.copyURLToFile(new URL(path), 
				tempZip);
		
		try (java.util.zip.ZipFile zipFile = new ZipFile(tempZip)) {
			  Enumeration<? extends ZipEntry> entries = zipFile.entries();
			  while (entries.hasMoreElements()) {
			    ZipEntry entry = entries.nextElement();
			    File entryDestination = new File("data/" + targetFolder,  entry.getName());
			    logger.info(entryDestination.getName());
			    if (entry.isDirectory()) {
			        entryDestination.mkdirs();
			    } else {
			        entryDestination.getParentFile().mkdirs();
			        try (InputStream in = zipFile.getInputStream(entry);
			             OutputStream out = new FileOutputStream(entryDestination)) {
			            IOUtils.copy(in, out);
			        }
			    }
			  }
			}
		
		tempZip.delete(); // delete the temporary zip file
	}
}
