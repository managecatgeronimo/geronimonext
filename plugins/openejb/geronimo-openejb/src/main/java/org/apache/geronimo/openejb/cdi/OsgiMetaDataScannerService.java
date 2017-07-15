/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.openejb.cdi;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.se.DefaultBDABeansXmlScanner;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.xbean.finder.BundleAssignableClassFinder;
import org.apache.xbean.osgi.bundle.util.BundleClassFinder;
import org.apache.xbean.osgi.bundle.util.BundleResourceFinder;
import org.apache.xbean.osgi.bundle.util.BundleUtils;
import org.apache.xbean.osgi.bundle.util.ClassDiscoveryFilter;
import org.apache.xbean.osgi.bundle.util.DiscoveryRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * In an OSGi environment, resources will not be delivered in
 * jars or file URLs, but as 'bundle://'.
 * This {@link org.apache.webbeans.spi.ScannerService} parses for all classes
 * in such a bundle.
 */
public class OsgiMetaDataScannerService implements ScannerService
{
    private WebBeansLogger logger = WebBeansLogger.getLogger(OsgiMetaDataScannerService.class);

    private final WebBeansContext webBeansContext;

    private static final String META_INF_BEANS_XML = "META-INF/beans.xml";
    private static final String WEB_INF_BEANS_XML = "WEB-INF/beans.xml";

    protected boolean isBDAScannerEnabled = false;
    protected BDABeansXmlScanner bdaBeansXmlScanner;

    /** All classes which have to be scanned for Bean information */
    private Set<Class<?>> beanClasses = new HashSet<Class<?>>();

    /** the URLs of all META-INF/beans.xml files */
    private Set<URL> beanXMLs = new HashSet<URL>();

    /**contains all the JARs we found with valid beans.xml in it */
    private Set<String> beanArchiveJarNames = new HashSet<String>();

    public OsgiMetaDataScannerService(WebBeansContext webBeansContext) {
        this.webBeansContext = webBeansContext;
    }

    @Override
    public Set<String> getAllAnnotations(String className) {
        return Collections.EMPTY_SET;
    }
    
    @Override
    public void init(Object object)
    {
        // set per BDA beans.xml flag here because setting it in constructor
        // occurs before
        // properties are loaded.
        String usage = webBeansContext.getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.USE_BDA_BEANSXML_SCANNER);
        this.isBDAScannerEnabled = Boolean.parseBoolean(usage);
        if (isBDAScannerEnabled)
        {
            bdaBeansXmlScanner = new DefaultBDABeansXmlScanner();
        }
    }

    @Override
    public void release()
    {
        beanClasses = new HashSet<Class<?>>();
        beanXMLs = new HashSet<URL>();
        beanArchiveJarNames = new HashSet<String>();
    }    
    
    @Override
    public void scan() throws WebBeansDeploymentException
    {
        logger.info("Using OsgiMetaDataScannerService!");
        Bundle mainBundle = BundleUtils.getContextBundle(true);


        if (mainBundle != null) {
            ServiceReference reference = mainBundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
            try
            {
                PackageAdmin packageAdmin = (PackageAdmin) mainBundle.getBundleContext().getService(reference);

                // search for all META-INF/beans.xml files
                findBeansXml(mainBundle, packageAdmin);

                // search for all classes
                if (!beanXMLs.isEmpty()) {
                    findBeanClasses(mainBundle, packageAdmin);
                }
            }
            catch(Exception e)
            {
                throw new WebBeansDeploymentException("problem while scanning OSGi bundle", e);
            }
            finally
            {
                mainBundle.getBundleContext().ungetService(reference);
            }
        }

    }

    private void findBeanClasses(Bundle mainBundle, PackageAdmin packageAdmin)
    {
        BundleClassFinder bundleClassFinder =
                new BundleAssignableClassFinder(packageAdmin, mainBundle,
                                                new Class<?>[]{Object.class},
                                                new ClassDiscoveryFilter()
         {

            @Override
            public boolean directoryDiscoveryRequired(String directory)
            {
                return true;
            }

            @Override
            public boolean jarFileDiscoveryRequired(String jarUrl)
            {
                return beanArchiveJarNames.contains(jarUrl);
            }

            @Override
            public boolean packageDiscoveryRequired(String packageName)
            {
                return true;
            }

            @Override
            public boolean rangeDiscoveryRequired(DiscoveryRange discoveryRange)
            {
                return discoveryRange.equals(DiscoveryRange.BUNDLE_CLASSPATH);
            }
        });

        Set<String> acceptedClassNames = bundleClassFinder.find();
        for (String clsName : acceptedClassNames)
        {
            try
            {
                Class<?> cls = mainBundle.loadClass(clsName);
                beanClasses.add(cls);
            }
            catch(Throwable e)
            {
                logger.info("cannot load class from bundle: " + clsName);
            }
        }
    }

    private void findBeansXml(Bundle mainBundle, PackageAdmin packageAdmin)
            throws Exception
    {
        BundleResourceFinder brfXmlJar =  new BundleResourceFinder(packageAdmin, mainBundle, "", META_INF_BEANS_XML);

        BundleResourceFinder.ResourceFinderCallback rfCallback = new BundleResourceFinder.ResourceFinderCallback()
        {

            public boolean foundInDirectory(Bundle bundle, String basePath, URL url) throws Exception
            {
                logger.info("adding the following beans.xml URL: " + url);
                beanXMLs.add(url);
                return true;
            }

            public boolean foundInJar(Bundle bundle, String jarName, ZipEntry entry, InputStream in) throws Exception
            {
                URL jarURL = bundle.getEntry(jarName);
                URL beansUrl = new URL("jar:" + jarURL.toString() + "!/" + entry.getName());

                logger.info("adding the following beans.xml URL: " + beansUrl);

                beanXMLs.add(beansUrl);
                beanArchiveJarNames.add(jarName);
                return true;
            }

        };

        brfXmlJar.find(rfCallback);

        // TODO I found no other way to find WEB-INF/beanx.xml directly
        Enumeration<URL> urls = mainBundle.findEntries("", "beans.xml", true);
        boolean webBeansXmlFound = false;
        while(urls != null && urls.hasMoreElements())
        {
            URL webBeansXml = urls.nextElement();
            if (!webBeansXml.toExternalForm().endsWith("/" + WEB_INF_BEANS_XML))
            {
                continue;
            }

            if (webBeansXmlFound)
            {
                throw new WebBeansDeploymentException("found more than WEB-INF/beans.xml file!" + webBeansXml); 
            }

            logger.info("adding the following WEB-INF/beans.xml URL: " + webBeansXml);
            beanXMLs.add(webBeansXml);
            webBeansXmlFound = true;

        }

    }

    @Override
    public Set<URL> getBeanXmls()
    {
        return beanXMLs;
    }

    @Override
    public Set<Class<?>> getBeanClasses()
    {
        return beanClasses;
    }

    @Override
    public boolean isBDABeansXmlScanningEnabled()
    {
        return isBDAScannerEnabled;
    }

    @Override
    public BDABeansXmlScanner getBDABeansXmlScanner()
    {
        return bdaBeansXmlScanner;
    }

}
