/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.provisional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.model.DefaultSiteParser;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.ExtendedSiteURLFactory;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.jarprocessor.JarProcessor;
import org.eclipse.update.internal.jarprocessor.Main;
import org.xml.sax.SAXException;

/**
 * The application class used to perform update site optimizations.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.2
 */
public class SiteOptimizerApplication implements IPlatformRunnable {
	public final static Integer EXIT_ERROR = new Integer(1);
	
	public final static String JAR_PROCESSOR = "-jarProcessor"; //$NON-NLS-1$
	public final static String DIGEST_BUILDER = "-digestBuilder"; //$NON-NLS-1$
	public final static String INPUT = "input"; //$NON-NLS-1$
	public final static String OUTPUT_DIR = "-outputDir"; //$NON-NLS-1$
	
	public final static String JAR_PROCESSOR_PACK = "-pack"; //$NON-NLS-1$
	public final static String JAR_PROCESSOR_UNPACK = "-unpack"; //$NON-NLS-1$
	public final static String JAR_PROCESSOR_REPACK = "-repack"; //$NON-NLS-1$
	public final static String JAR_PROCESSOR_SIGN = "-sign"; //$NON-NLS-1$
	
	public final static String SITE_XML = "-siteXML"; //$NON-NLS-1$
	public final static String SITE_ATTRIBUTES_FILE = "siteAttributes.txt"; //$NON-NLS-1$
	public final static String DIGEST_OUTPUT_DIR = "-digestOutputDir"; //$NON-NLS-1$
	
	/*private final static String DESCRIPTION = "DESCRIPTION";
	private final static String LICENCE = "LICENCE";
	private final static String COPYRIGHT = "COPYRIGHT";
	private final static String FEATURE_LABEL = "FEATURE_LABEL";*/
	

	
	/**
	 * Parses the command line in the form:
	 *  [-key [value]]* [inputvalue]
	 *  If the last argument does not start with a "-" then it is taken as the input value and not the value for a preceding -key
	 * @param args
	 * @return
	 */
	private Map parseCmdLine(String [] args) {
		Map cmds = new HashMap();
		for (int i = 0; i < args.length; i++) {
			if(i == args.length - 1 && !args[i].startsWith("-")) { //$NON-NLS-1$
				cmds.put(INPUT, args[i]);
			} else {
				String key = args[i];
				String val = null;
				if( i < args.length - 2 && !args[i + 1].startsWith("-")){ //$NON-NLS-1$
					val = args[++i];
				}
				
				if (key.startsWith(SITE_XML)) { 
					//System.out.println(val.indexOf(":null"));
					val = key.substring(key.indexOf("=") + 1);
					//System.out.println(key + ":" + val);
					cmds.put(SITE_XML, val);
				} else if (key.startsWith(DIGEST_OUTPUT_DIR)) {
					val = key.substring(key.indexOf("=") + 1);
					//System.out.println(key + ":" + val);
					cmds.put(DIGEST_OUTPUT_DIR, val);
				}else {
				
					//System.out.println(key + ":" + val);
					cmds.put(key, val);
				}
			}
		}
		return cmds;
	}
	
	private boolean runJarProcessor(Map params) {
		Main.Options options = new Main.Options();
		options.pack = params.containsKey(JAR_PROCESSOR_PACK);
		options.unpack = params.containsKey(JAR_PROCESSOR_UNPACK);
		options.repack = params.containsKey(JAR_PROCESSOR_REPACK);
		options.signCommand = (String) params.get(JAR_PROCESSOR_SIGN);
		options.outputDir = (String) params.get(OUTPUT_DIR);
		
		String problem = null;
		
		String input = (String) params.get(INPUT);
		if(input == null)
			problem = Messages.SiteOptimizer_inputNotSpecified;
		else {
			File inputFile = new File(input);
			if(inputFile.exists())
				options.input = inputFile;
			else 
				problem = NLS.bind(Messages.SiteOptimizer_inputFileNotFound, new String[] {input});
		}
		
		if (options.unpack) {
			if (!JarProcessor.canPerformUnpack()) {
				problem = Messages.JarProcessor_unpackNotFound;
			} else 	if (options.pack || options.repack || options.signCommand != null) {
				problem = Messages.JarProcessor_noPackUnpack;
			}
		} else if ((options.pack || options.repack) && !JarProcessor.canPerformPack()) {
			problem = Messages.JarProcessor_packNotFound;
		}
	
		if(problem != null) {
			System.out.println(problem);
			return false;
		}
		
		Main.runJarProcessor(options);
		return true;
	}
	
	private boolean runDigestBuilder(Map params) {
		
		List featureList = getFeatureList(params);
		
		if ((featureList == null) || featureList.isEmpty()) {
			System.out.println("no features to process");
			return false;
		}
		Map perFeatureLocales = new HashMap();
		Map availableLocales = getAvailableLocales(featureList, perFeatureLocales);
		try {
			openInputStremas(availableLocales);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Can not create file in output direcotry");
			return false;
		}
		
		Iterator featureIterator = featureList.iterator();
		int i = 0;
		while(featureIterator.hasNext()) {
			String featureJarFileName = (String)featureIterator.next();
			//System.out.println("i=" + i++);
			
			if (featureJarFileName.endsWith("jar"))
				System.out.println("Processing... " + featureJarFileName);
			else 
				System.out.println("Skipping... " + featureJarFileName);
			
			JarFile featureJar = null;
			try {
				featureJar = new JarFile(featureJarFileName);
			} catch (IOException e) {
				System.out.println("Problem with openning jar: " + featureJarFileName);
				e.printStackTrace();
				return false;
			}
			FeatureModelFactory fmf = new FeatureModelFactory();

			
			try {
				ZipEntry featureXMLEntry = featureJar.getEntry("feature.xml");
				Map featureProperties = loadProperties(featureJar, featureJarFileName, perFeatureLocales);
				
				FeatureModel featureModel = fmf.parseFeature(featureJar.getInputStream(featureXMLEntry));
				
				Iterator availableLocalesIterator = availableLocales.values().iterator();
				while(availableLocalesIterator.hasNext()) {
					((AvailableLocale)availableLocalesIterator.next()).writeFeatureDigests( featureModel, featureProperties);
				}
				
				
			} catch (SAXException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
		}
		Iterator availableLocalesIterator = availableLocales.values().iterator();
		String outputDirectory = (String)params.get(DIGEST_OUTPUT_DIR);
		
		outputDirectory = outputDirectory.substring(outputDirectory.indexOf("=") + 1);
		if ( !outputDirectory.endsWith(File.separator)) {
			outputDirectory = outputDirectory + File.separator;
		}
		while(availableLocalesIterator.hasNext()) {
			try {
				((AvailableLocale)availableLocalesIterator.next()).finishDigest(outputDirectory);
			} catch (IOException e) {
				System.out.println("Can not write in digest output directory: " + outputDirectory);
				e.printStackTrace();
				return false;
			}
		}
		System.out.println("Done");
		return true;
	}
	
	private Map loadProperties(JarFile featureJar, String featureJarFileName, Map perFeatureLocales) {
		//System.out.println( ((List)perFeatureLocales.get(featureJarFileName)).size());
		Iterator it = ((List)perFeatureLocales.get(featureJarFileName)).iterator();
		Map result = new HashMap();
		while(it.hasNext()) {
			String propertyFileName = (String)it.next();
			
			ZipEntry featurePropertiesEntry = featureJar.getEntry(propertyFileName);
			Properties featureProperties = new Properties();
			if (featurePropertiesEntry != null) {
				try {
					featureProperties.load(featureJar.getInputStream(featurePropertiesEntry));
					String localeString = null;
					if(propertyFileName.endsWith("feature.properties")) {
						localeString = "";
					} else {
						localeString = propertyFileName.substring( 8 , propertyFileName.indexOf('.') );
					}
					result.put(localeString, featureProperties);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
		}
		return result;
	}

	private void openInputStremas(Map availableLocales) throws IOException {
		Iterator locales = availableLocales.values().iterator();
		while(locales.hasNext()) {
			AvailableLocale availableLocale = (AvailableLocale)locales.next();
			availableLocale.openLocalizedOutputStream();
		}
	}

	private Map getAvailableLocales(List featureList, Map perFeatureLocales) {
		Iterator features = featureList.iterator();
		Map locales = new HashMap();
		while (features.hasNext()) {
			String feature = (String)features.next();
			try {
				System.out.println("Extracting locales from " + feature);
				processLocalesInJar(locales, feature, perFeatureLocales);
			} catch (IOException e) {
				System.out.println("Error while extracting locales from " + feature);
				e.printStackTrace();
				return null;
			}
		}
		return locales;
	}

	private void processLocalesInJar(Map locales, String feature, Map perFeatureLocales) throws IOException {
		
		JarFile jar = new JarFile(feature);
		//System.out.println(feature);
		Enumeration files = jar.entries();
		
		List localesTemp = new ArrayList();
		perFeatureLocales.put(feature, localesTemp);
		
		while( files.hasMoreElements()) {
			ZipEntry file = (ZipEntry)files.nextElement();
			String localeString = null;
			String name = file.getName();
			//System.out.println("processLocalesInJar:"+name);
			if (name.startsWith("feature") && name.endsWith(".properties")) {
				//System.out.println(name);
				localesTemp.add(name);
				//System.out.println(name);
				if(name.endsWith("feature.properties")) {
					localeString = "";
				} else {
					localeString = name.substring( 8 , name.indexOf('.') );
				}
				//System.out.println(name +"::::\"" + localeString + "\"");
				if (!locales.containsKey(localeString)) {
					locales.put(localeString, new AvailableLocale(localeString));
				}
				AvailableLocale currentLocale = (AvailableLocale)locales.get(localeString);
				currentLocale.addFeatures(feature);
			}
		}
		
	}

	private List getFeatureList(Map params) {
		if (params.containsKey(SITE_XML) && (fileExists((String)params.get(SITE_XML)))) {		
			return getFeatureListFromSiteXML((String)params.get(SITE_XML));
		} else if (params.containsKey(INPUT) && isDirectory((String)params.get(SiteOptimizerApplication.INPUT))) {
			return getFeatureListFromDirectory((String)params.get(INPUT));
		}
		return null;
	}

	private boolean fileExists(String fileName) {
		//System.out.println("fileExists:"+fileName);
		File file = new File(fileName);
		if ((file != null) && file.exists()) 
			return true;
		return false;
	}

	private List getFeatureListFromDirectory(String directoryName) {
		List featuresURLs = new ArrayList();
		File directory = new File(directoryName);
		String[] featureJarFileNames = directory.list();
		for(int i = 0; i < featureJarFileNames.length; i++) {
			featuresURLs.add(directoryName + File.separator + featureJarFileNames[i]);
		}
		return featuresURLs;
	}

	private boolean isDirectory(String fileName) {
		
		File directory = new File(fileName);
		if ((directory != null) && directory.exists() && directory.isDirectory()) 
			return true;
		return false;
	}

	private List getFeatureListFromSiteXML(String siteXML) {
		
		List featuresURLs = new ArrayList();
		String directoryName = (new File(siteXML)).getParent();
		if (!directoryName.endsWith(File.separator)) {
			directoryName = directoryName + File.separator;
		}
		
		DefaultSiteParser siteParser = new DefaultSiteParser();
		siteParser.init( new ExtendedSiteURLFactory());
		
		try {
			SiteModel site = siteParser.parse(new FileInputStream(siteXML));
			site.getFeatureReferenceModels()[1].getURLString();
			FeatureReferenceModel[] featureReferenceModel = site.getFeatureReferenceModels();
			//System.out.println("featureReferenceModel# =" +  featureReferenceModel.length);
			for(int i = 0; i < featureReferenceModel.length; i++) {
				featuresURLs.add(directoryName + featureReferenceModel[i].getURLString());
			}
			//System.out.println("featureReferenceModel# =" +  featuresURLs.size());
			return featuresURLs;
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Parsing problem: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Problem while parsing: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		Platform.endSplash();
		if (args == null)
			return EXIT_ERROR;
		if (args instanceof String[]) {
			Map params = parseCmdLine((String[]) args);
			if(params.containsKey(JAR_PROCESSOR)){
				if(!runJarProcessor(params))
					return EXIT_ERROR;
			}
			
			if(params.containsKey(DIGEST_BUILDER)){
				if(!runDigestBuilder(params))
					return EXIT_ERROR;
			}
		}
		return IPlatformRunnable.EXIT_OK;
	}
	
	
	private class AvailableLocale {
		
		private String PREFIX = "temp";
		
		private String locale;
		private Map /*VersionedIdentifier*/ features = new HashMap();
		private PrintStream localizedPrintStream;
		private File tempDigestDirectory;
		public Map availableLocales;
		
		public Map getAvailableLocales() {
			return availableLocales;
		}
		public void finishDigest(String outputDirectory) throws IOException {
			localizedPrintStream.println("</digest>");
			if (localizedPrintStream != null) {
				localizedPrintStream.close();
			}
			File digest = new File(outputDirectory + File.separator +"digest" + locale + ".zip");
			System.out.println(digest.getAbsolutePath());
			System.out.println(digest.getName());
			if (digest.exists()) {
				digest.delete();
			}
			digest.createNewFile();
			OutputStream os = new FileOutputStream(digest);
			JarOutputStream jos = new JarOutputStream(os);
			jos.putNextEntry( new ZipEntry("digest.xml")); 
			InputStream is = new FileInputStream(tempDigestDirectory);
			byte[] b = new byte[4096];
			int bytesRead = 0;
			do {
				bytesRead = is.read(b);
				if (bytesRead > 0) {
					jos.write(b, 0, bytesRead);
				}
			} while(bytesRead > 0);
			
			jos.closeEntry();
			jos.close();
			os.close();
			is.close();
			tempDigestDirectory.delete();
			
			
		}
		public void setAvailableLocales(Map availableLocales) {
			this.availableLocales = availableLocales;
		}
		public AvailableLocale( String locale) {
			this.locale = locale;
		}
		public Map getFeatures() {
			return features;
		}
		public void addFeatures(String feature) {
			features.put(feature, feature);
		}
		public String getLocale() {
			return locale;
		}
		public PrintStream getLocalizedPrintStream() {
			return localizedPrintStream;
		}
		public void openLocalizedOutputStream() throws IOException {
			tempDigestDirectory = File.createTempFile(PREFIX, null);
			localizedPrintStream = new PrintStream(tempDigestDirectory);
			localizedPrintStream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n <digest>");
			tempDigestDirectory.deleteOnExit();
		}
		public int hashCode() {
			return locale.hashCode();
		}
		public boolean equals(Object obj) {
			
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final AvailableLocale other = (AvailableLocale) obj;
			if (locale == null) {
				if (other.locale != null)
					return false;
			} else if (!locale.equals(other.locale))
				return false;
			return true;
		}	
		
		public void writeFeatureDigests(FeatureModel featureModel, Map featureProperties) {
			
			if (this.locale.equals("")) {
				writeFeatureDigest( localizedPrintStream, featureModel, (Properties)featureProperties.get("") );
				return;
			}
			Properties temp = new Properties();
			if(locale.indexOf("_") < 0) {
				temp = combineProperties((Properties)featureProperties.get(""), (Properties)featureProperties.get(locale), temp);
				writeFeatureDigest( localizedPrintStream, featureModel, temp );
			} else {
				temp = combineProperties((Properties)featureProperties.get(locale.substring(locale.indexOf("_") + 1)), (Properties)featureProperties.get(locale), temp);
				writeFeatureDigest( localizedPrintStream, featureModel, temp);
			}
			
		}
		private Properties combineProperties(Properties properties, Properties properties2, Properties properties3) {
			return new CombinedProperties(properties3, properties2, properties);

		}
	
	}
	
	public static void writeFeatureDigest( PrintStream digest, FeatureModel featureModel, Properties featureProperties) {
		
		String label = null;
		String provider = null;
		String description = null;
		String license = null;
		String copyright = null;
		
		if ((featureProperties != null) && featureModel.getLabel().startsWith("%")) {
			label = featureProperties.getProperty(featureModel.getLabel().substring(1));
		} else {
			label = featureModel.getLabel();
		}
		if ((featureProperties != null) &&(featureModel.getDescriptionModel() != null) && featureModel.getDescriptionModel().getAnnotation().startsWith("%")) {
			//System.out.println(featureProperties.getProperty(featureModel.getDescriptionModel().getAnnotation().substring(1)));
			description = featureProperties.getProperty(featureModel.getDescriptionModel().getAnnotation().substring(1));
		} else {
			description = featureModel.getDescriptionModel().getAnnotation();
		}
		if ((featureProperties != null) && featureModel.getProvider().startsWith("%")) {
			provider = featureProperties.getProperty(featureModel.getProvider().substring(1));
		} else {
			provider = featureModel.getProvider();
		}
		
		if (((featureProperties != null) && featureModel.getCopyrightModel() != null) && featureModel.getCopyrightModel().getAnnotation().startsWith("%")) {
			copyright = featureProperties.getProperty(featureModel.getCopyrightModel().getAnnotation().substring(1));
		} else {
			if (featureModel.getCopyrightModel() != null) {
				copyright = featureModel.getCopyrightModel().getAnnotation();
			} else {
				copyright = null;
			}
		}
		
		if ((featureProperties != null) && (featureModel.getLicenseModel() != null) && featureModel.getLicenseModel().getAnnotation().startsWith("%")) {
			license = featureProperties.getProperty(featureModel.getLicenseModel().getAnnotation().substring(1));
		} else {
			license = featureModel.getLicenseModel().getAnnotation();
		}
			
		
		digest.print("<feature ");
		digest.print("label=\"" + label + "\" ");
		digest.print("provider-name=\"" + provider + "\" ");
		digest.print("id=\"" + featureModel.getFeatureIdentifier() + "\" ");
		digest.print("version=\"" + featureModel.getFeatureVersion() + "\" ");
		if (featureModel.getOS() != null) 
			digest.print("os=\"" + featureModel.getOS() + "\" ");
		if (featureModel.getNL() != null) 
			digest.print("nl=\"" + featureModel.getNL() + "\" ");
		if (featureModel.getWS() != null) 
			digest.print("ws=\"" + featureModel.getWS() + "\" ");
		if (featureModel.getOSArch() != null) 
			digest.print("arch=\"" + featureModel.getOSArch() + "\" ");
		if (featureModel.isExclusive()) 
			digest.print("exclusive=\"" + featureModel.isExclusive() + "\" ");
		
		if ( ((featureModel.getImportModels() == null) || (featureModel.getImportModels().length == 0))
				&& ( (featureModel.getDescriptionModel() == null) || (featureModel.getDescriptionModel().getAnnotation() == null) || (featureModel.getDescriptionModel().getAnnotation().trim().length() == 0))
				&& ( (featureModel.getCopyrightModel() == null) || (featureModel.getCopyrightModel().getAnnotation() == null) || (featureModel.getCopyrightModel().getAnnotation().trim().length() == 0))
				&& ( (featureModel.getLicenseModel() == null) || (featureModel.getLicenseModel().getAnnotation() == null) || (featureModel.getLicenseModel().getAnnotation().trim().length() == 0))
				) {
			digest.println("/> ");
		} else {
			digest.println("> ");
			if ( featureModel.getImportModels().length > 0) {

				digest.println("\t<requires> ");
				ImportModel[] imports = featureModel.getImportModels();
				for( int j = 0; j < imports.length; j++) {
					digest.print("\t\t<import ");
					if (imports[j].isFeatureImport()) {
						digest.print("feature=\"");
					} else {
						digest.print("plugin=\"");
					}
					digest.print(imports[j].getIdentifier() + "\" ");
					digest.print("version=\"");
					digest.print(imports[j].getVersion() + "\" ");
					digest.print("match=\"");
					digest.print(imports[j].getMatchingRuleName() + "\" ");
					if (imports[j].isPatch()) {
						digest.print("patch=\"true\" ");
					}
					digest.println(" />");
				}

				digest.println("\t</requires>");
				
			} 
			
			if ( (featureModel.getDescriptionModel() != null) && (featureModel.getDescriptionModel().getAnnotation() != null) && (featureModel.getDescriptionModel().getAnnotation().trim().length() != 0)) {
				digest.println("\t<description>");
				digest.println("\t\t"+ description);
				digest.println("\t</description>");
			}
			
			if (featureModel.getCopyrightModel() != null) { 
				if (featureModel.getCopyrightModel().getAnnotation() != null) {
					//if (featureModel.getDescriptionModel().getAnnotation().length() != 0) {				
						digest.println("\t<copyright>");
						digest.println("\t\t"+ copyright);
						digest.println("\t</copyright>");
					//}
				}
			}
			
			if ( (featureModel.getLicenseModel() != null) && (featureModel.getLicenseModel().getAnnotation() != null) && (featureModel.getDescriptionModel().getAnnotation().trim().length() != 0)) {
				digest.println("\t<license>");
				digest.println("\t\t"+ license);
				digest.println("\t</license>");
			}
			
			PluginEntryModel[] plugins = featureModel.getPluginEntryModels();
			if ( (plugins !=null) && (plugins.length != 0)){
				for( int i = 0; i < plugins.length; i++ ) {
					digest.print("\t<plugin ");
					digest.print("id=\"" + plugins[i].getPluginIdentifier() + "\" ");
					digest.print("version=\"" + plugins[i].getPluginVersion() + "\" ");
					if (plugins[i].getOS() != null) 
						digest.print("os=\"" + plugins[i].getOS() + "\" ");
					if (plugins[i].getNL() != null) 
						digest.print("nl=\"" + plugins[i].getNL() + "\" ");
					if (plugins[i].getWS() != null) 
						digest.print("ws=\"" + plugins[i].getWS() + "\" ");
					if (plugins[i].getOSArch() != null) 
						digest.print("arch=\"" + plugins[i].getOSArch() + "\" ");
					if (plugins[i].getDownloadSize() > 0) 
						digest.print("download-size=\"" + plugins[i].getDownloadSize() + "\" ");
					if (plugins[i].getInstallSize() > 0) 
						digest.print("install-size=\"" + plugins[i].getInstallSize() + "\" ");
					if (!plugins[i].isUnpack()) 
						digest.print("unpack=\"" + plugins[i].isUnpack() + "\" ");
					
					digest.println("/> ");
				}
			}
			digest.println("</feature>");
		}
	}
	
	private class CombinedProperties extends Properties {
		
		private Properties properties1;
		private Properties properties2;
		private Properties properties3;

		public CombinedProperties(Properties properties1, Properties properties2, Properties properties3) {
			this.properties1 = properties1;
			this.properties2 = properties2;
			this.properties3 = properties3;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public String getProperty(String key) {
			String result = null;
			if (properties3 != null)
				result = properties3.getProperty(key);
			if (properties2 != null)
				result = properties2.getProperty(key);
			if (properties1 != null)
				result = properties1.getProperty(key);
			return result;
		}
		
	}

}
