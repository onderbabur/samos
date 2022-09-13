Potentially more up-to-date public online documentation, including installation guide and a short demonstration, for SAMOS can be found in the [SAMOS GitHub.io page](https://onderbabur.github.io/samos/).

# Introduction
**SAMOS** (Statistical Analysis of Models) is a framework for model analytics and management. It is capable of processing large numbers of models for different scenarios, notably domain clustering and clone detection. The underlying principles are inspired from the domains information retrieval, data science and machine learning. The main components of a typical SAMOS workflow consist of (a) model fragmentation in terms n-grams, (b) feature comparison using algorithms and natural language processing, (c) vector space model computation, (d) distance measurement and clustering.   


# Features in SAMOS 1.0.0
1. Support for processing Ecore metamodels, for clustering and clone detection scenarios
2. Simple CLI for crawling test models, and running clustering and clone detection with standard settings
3. Configuration (through changing the source code) of the following:
* static scoping for model fragment: whole model, EPackage, EClass
* unigram and bigram feature extraction with full set of attributes
* many more, notably natural language processing parameters such as tokenisation, lemmatization, WordNet semantic similarity

# Running SAMOS from the virtual image 
1. Download [Virtual Box](https://www.virtualbox.org/wiki/Downloads) for your operating system.
2. Download [the Lubuntu image archive we provide on Zenodo](https://zenodo.org/record/7074428), which contains all the required software and configuration to run SAMOS. 
3. Unrar the image file Lubuntu 21.04 (64bit).vdi into a folder. 
4. Go to Virtual Box, and using Tools click on "New" to add a new virtual machine. 
5. Write a name you'd like. In the hard disk options, choose "Use an existing virtual hard disk file". 
6. Click on the folder icon on the right, then click "Add". Choose the downloaded image file and click on "Open" to go back to the previous screen. Click on "Choose" to proceed. 
7. The type and version of the virtual machine should be automatically specified as Linux and Ubuntu (64-bit). Next specify how much memory to allocate to the virtual machine. We recommend at least 8 gb. 
8. Click on "Create". Now you should have an item in the list of virtual machines. With that item selected, click on the icon "Start" to boot up the virtual machine. 
9. Enter the password "osboxes.org" to log in.
10. On the desktop, there is already an Eclipse installation with SAMOS set up. Open the folder "Eclipse", and execute the application "eclipse".
11. With the default workspace given (/home/osboxes/Desktop/eclipse-workspace), click on "Launch". 
12. You can now follow the instructions for running SAMOS. See below section titled "Running SAMOS with sample models". 

# Setting up SAMOS using Eclipse

#### Install required software
1. Install [Java SE JDK](https://www.oracle.com/java/technologies/javase-downloads.html) (tested with versions 1.8 and 1.11).
2. Download [Eclipse Modeling Tools](https://www.eclipse.org/downloads/packages/release/2021-03/r/eclipse-modeling-tools) for your operating system (tested with Windows, MacOS, Linux for the Eclipse package version 2021-03).
3. Run Eclipse and through Eclipse marketplace install the Eclipse plugin [m2e](https://github.com/eclipse-m2e/m2e-core/blob/master/README.md#-installation) for Maven integration. 
4. Install [R](https://cran.r-project.org/mirrors.html) for your operating system (tested with versions 3.6.1 and 4+). 
5. Install [rJava](https://rforge.net/JRI/) by going to R and running the command `install.packages("rJava")`. Make sure to note down the values for R_HOME and the JRI library path as explained in the rJava website, to be later added in Eclipse run configurations. See below for the typical values for MacOS and Linux. 
6. Install the custom R package "vegan" from the archive file under the path lib\vegan_2.4-3.tar.gz, using the R command `install.packages(FULL_PATH_TO_ARCHIVE, repos = NULL)`. The rest of the required R packages are installed when you first run SAMOS analyses. 

MACOS paths for rJava:

	R_HOME = /Library/Frameworks/R.framework/Resources/Library/Frameworks/R.framework/Resources
	JRI library path = /Library/Frameworks/R.framework/Resources/library/rJava/jri/

Linux (Lubuntu as in the VM) paths for rJava:

	R_HOME = /usr/lib/R/
	JRI library path = /usr/lib/R/site-library/rJava/

#### Importing and building SAMOS
1. Download project zip file from the GitHub address of [SAMOS](https://github.com/onderbabur/samos/archive/refs/heads/main.zip).
2. Open Eclipse, and go to menu item File->Import. Select "General" and then "Projects from Folder or Archive". Click on the button "Archive...". Select the zip file and click ok. Eclipse creates two items to be imported. Check the one containing an Eclipse project ("samos-main_expanded.zip/samos-main") and click on "Finish". 
3. You should have SAMOS as a project in Eclipse workspace now. To build SAMOS, right click on the "pom.xml" in the root project folder of SAMOS, and click on "Maven install...". All dependencies should be downloaded and SAMOS should be compiled. 

#### Running SAMOS with sample models
1. There are three run configuration files in the project root folder, with the extension ".launch". R_HOME and JRI library paths should be set for running SAMOS. For every .launch file, you should change the values in the XML configuration to the correct values in your system:

    ```
    	...
            <mapEntry key="R_HOME" value="PATH_TO_YOUR_R_HOME"/>
        ...
        <stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="-Djava.library.path=PATH_TO_YOUR_JRI_LIB ..."/>
     
    ```
2. Right click on "SAMOSRunner crawl atlzoo.launch", select "Run As..." and choose the first item to run the crawler and download sample files to run SAMOS with. 
3. Afterwards you can repeat the step 11 for either "SAMOSRunner cluster atlzoo.launch" or "SAMOSRunner clone atlzoo.launch" for domain clustering and clone detection respectively. 
4. Once SAMOS is finished, the results can be viewed under the project root folder "results/r/atlzoo". 

#### Running SAMOS with different datasets and configuration
For playing with other settings, you can run SAMOS (main entry point: nl.tue.set.samos.main.SAMOSRunner.java) with different datasets (e.g. [the large Ecore dataset on Zenodo](https://zenodo.org/record/2585456) and configuration such as scope, and NLP preprocessing. 

# References
To cite SAMOS, please use the following bibtex entry:

````
@article{babur2022samos,
title = "{SAMOS} - A Framework for Model Analytics and Management",
journal = "Science of Computer Programming",
year = "2022",
author = "{\"O}nder Babur and Loek Cleophas and Mark {van den Brand}"
}
````

The preprint of the paper can be found under the folder "paper".

Other major publications for SAMOS: 

Önder Babur, Loek Cleophas, and Mark van den Brand. Hierarchical clustering of metamodels for comparative analysis and visualization. In European Conference on Modelling Foundations and Applications, pages 3–18. Springer, 2016.

Önder Babur, Loek Cleophas, and Mark van den Brand.  Metamodel clone detection with SAMOS. Journal of Computer Languages, 51, pages 57–74, 2019.

Önder  Babur and Loek  Cleophas. Using  n-grams  for  the  automated  clustering  of structural  models. In International Conference on Current Trends in Theory and Practice of Informatics, pages 510–524. Springer, 2017.

# Features to be integrated in future versions:

- major performance optimizations, including iterative clone detection
- support for other languages: feature models, UML, BPMN, Simulink
- other stat and ML algorithms: topic modeling, deep learning
- HPC support with Apache Spark
- MSR and Neo4j bridge for model analytics
