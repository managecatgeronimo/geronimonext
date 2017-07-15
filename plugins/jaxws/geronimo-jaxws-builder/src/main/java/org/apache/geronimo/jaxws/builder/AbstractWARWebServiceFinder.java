/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.jaxws.builder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.Deployable;
import org.apache.geronimo.deployment.DeployableBundle;
import org.apache.geronimo.deployment.DeployableJarFile;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.WebModule;
import org.apache.geronimo.jaxws.handler.AnnotationHandlerChainFinder;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.util.FileUtils;
import org.apache.geronimo.kernel.util.JarUtils;
import org.apache.geronimo.kernel.util.NestedJarFile;
import org.apache.geronimo.kernel.util.UnpackedJarFile;
import org.apache.xbean.classloader.JarFileClassLoader;
import org.apache.xbean.finder.BundleAnnotationFinder;
import org.apache.xbean.finder.ClassFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractWARWebServiceFinder implements WebServiceFinder<WebModule> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWARWebServiceFinder.class);

    protected AnnotationHandlerChainFinder annotationHandlerChainFinder = new AnnotationHandlerChainFinder();

    protected Set<String> getEJBWebServiceClassNames(Module module) {
        if (module.getModules().size() == 0) {
            return Collections.<String> emptySet();
        }
        Set<String> ejbWebServiceClassNames = new HashSet<String>();
        for (Module subModule : (LinkedHashSet<Module<?, ?>>) module.getModules()) {
            if (subModule.getType() == ConfigurationModuleType.EJB) {
                Set<String> currentEJBWebServiceClassNames = (Set<String>) subModule.getSharedContext().get(EJB_WEB_SERVICE_CLASS_NAMES);
                if (ejbWebServiceClassNames != null) {
                    ejbWebServiceClassNames.addAll(currentEJBWebServiceClassNames);
                }
            }
        }
        return ejbWebServiceClassNames;
    }

    /**
     * Returns a list of any classes annotated with @WebService or
     * @WebServiceProvider annotation.
     */
    protected List<Class<?>> discoverWebServices(WebModule module) throws DeploymentException {
        Deployable deployable = module.getDeployable();
        if (deployable instanceof DeployableJarFile) {
            return discoverWebServices(((DeployableJarFile) deployable).getJarFile(), AbstractWARWebServiceFinder.class.getClassLoader());
        } else if (deployable instanceof DeployableBundle) {
            return discoverWebServices(((DeployableBundle) deployable).getBundle());
        } else {
            throw new DeploymentException("Unsupported deployable: " + deployable.getClass());
        }
    }

    /**
     * Returns a list of any classes annotated with @WebService or
     * @WebServiceProvider annotation.
     */
    private List<Class<?>> discoverWebServices(Bundle bundle) throws DeploymentException {
        logger.debug("Discovering web service classes");

        ServiceReference sr = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin packageAdmin = (PackageAdmin) bundle.getBundleContext().getService(sr);
        try {
            BundleAnnotationFinder classFinder = new BundleAnnotationFinder(packageAdmin, bundle);
            List<Class<?>> classes = new ArrayList<Class<?>>();
            classes.addAll(classFinder.findAnnotatedClasses(WebService.class));
            classes.addAll(classFinder.findAnnotatedClasses(WebServiceProvider.class));
            return classes;
        } catch (Exception e) {
            throw new DeploymentException("Error scanning for web service annotations in bundle", e);
        } finally {
            bundle.getBundleContext().ungetService(sr);
        }
    }

    /**
     * Returns a list of any classes annotated with @WebService or
     * @WebServiceProvider annotation.
     */
    @Deprecated
    private List<Class<?>> discoverWebServices(JarFile moduleFile, ClassLoader parentClassLoader) throws DeploymentException {
        logger.debug("Discovering web service classes");

        File tmpDir = null;
        List<URL> urlList = new ArrayList<URL>();

        File baseDir;

        if (moduleFile instanceof UnpackedJarFile) {
            // war directory is being deployed (--inPlace)
            baseDir = ((UnpackedJarFile) moduleFile).getBaseDir();
        } else if (moduleFile instanceof NestedJarFile && ((NestedJarFile) moduleFile).isUnpacked()) {
            // ear directory is being deployed (--inPlace)
            baseDir = new File(moduleFile.getName());
        } else {
            // war file or ear file is being deployed
            /*
             * Can't get ClassLoader to load nested Jar files, so
             * unpack the module Jar file and discover all nested Jar files
             * within it and the classes/ directory.
             */
            try {
                tmpDir = FileUtils.createTempDir();
                /*
                 * This is needed becuase JarUtils.unzipToDirectory()
                 * always closes the passed JarFile.
                 */
                JarFile module = new JarFile(moduleFile.getName());
                JarUtils.unzipToDirectory(module, tmpDir);
            } catch (IOException e) {
                if (tmpDir != null) {
                    FileUtils.recursiveDelete(tmpDir);
                }
                throw new DeploymentException("Failed to expand the module archive", e);
            }

            baseDir = tmpDir;
        }

        // create URL list
        Enumeration<JarEntry> jarEnum = moduleFile.entries();
        while (jarEnum.hasMoreElements()) {
            JarEntry entry = jarEnum.nextElement();
            String name = entry.getName();
            if (name.equals("WEB-INF/classes/")) {
                // ensure it is first
                File classesDir = new File(baseDir, "WEB-INF/classes/");
                try {
                    urlList.add(0, classesDir.toURI().toURL());
                } catch (MalformedURLException e) {
                    // this should not happen, ignore
                }
            } else if (name.startsWith("WEB-INF/lib/") && name.endsWith(".jar")) {
                File jarFile = new File(baseDir, name);
                try {
                    urlList.add(jarFile.toURI().toURL());
                } catch (MalformedURLException e) {
                    // this should not happen, ignore
                }
            }
        }

        URL[] urls = urlList.toArray(new URL[urlList.size()]);
        JarFileClassLoader tempClassLoader = null;
        try {
            tempClassLoader = new JarFileClassLoader(null, urls, parentClassLoader);
            List<Class<?>> classes = new ArrayList<Class<?>>();
            for (URL url : urlList) {
                try {
                    ClassFinder classFinder = new ClassFinder(tempClassLoader, Collections.singletonList(url));
                    classes.addAll(classFinder.findAnnotatedClasses(WebService.class));
                    classes.addAll(classFinder.findAnnotatedClasses(WebServiceProvider.class));
                } catch (Exception e) {
                    logger.warn("Fail to search Web Service in jar [" + url + "]", e);
                }
            }
            return classes;
        } finally {
            if (tempClassLoader != null) {
                tempClassLoader.destroy();
            }
            if (tmpDir != null) {
                FileUtils.recursiveDelete(tmpDir);
            }
        }
    }
}
