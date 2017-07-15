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

package org.apache.geronimo.deployment.util.osgi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.geronimo.common.IllegalConfigurationException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.InternalKernelException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.system.configuration.OsgiMetaDataProvider;
import org.apache.xbean.osgi.bundle.util.BundleDescription.ExportPackage;
import org.apache.xbean.osgi.bundle.util.HeaderParser;
import org.apache.xbean.osgi.bundle.util.HeaderParser.HeaderElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev$ $Date$
 */
public class OSGiMetaDataBuilder {

    private static final Logger logger = LoggerFactory.getLogger(OSGiMetaDataBuilder.class);

    private static final boolean USE_FIXED_IMPORT_PACKAGE_VERSION = Boolean.parseBoolean(System.getProperty("org.apache.geronimo.deployment.import_package.fixed_version", "true"));

    private static final Version ZERO_VERSION = new Version(0, 0, 0);

    private BundleContext bundleContext;

    private ExportPackagesSelector exportPackagesSelector;

    public OSGiMetaDataBuilder(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.exportPackagesSelector = new HighVersionFirstExportPackagesSelector();
    }

    public OSGiMetaDataBuilder(BundleContext bundleContext, ExportPackagesSelector exportPackagesSelector) {
        this.bundleContext = bundleContext;
        this.exportPackagesSelector = exportPackagesSelector;
    }

    public void build(Environment environment) throws IllegalConfigurationException {
        build(environment, false);
    }

    public void build(Environment environment, boolean clientModule) throws IllegalConfigurationException {
        processClassloadingRules(environment);
        ServiceReference serviceReference = null;
        try {
            serviceReference = bundleContext.getServiceReference(OsgiMetaDataProvider.class.getName());
            OsgiMetaDataProvider osgiMetaDataProvider = null;
            if (serviceReference != null) {
                osgiMetaDataProvider = (OsgiMetaDataProvider) bundleContext.getService(serviceReference);
            }
            OSGiBuildContext context = createOSGiBuildContext(environment, osgiMetaDataProvider, clientModule);
            processImportPackages(context);
        } finally {
            if (serviceReference != null) {
                bundleContext.ungetService(serviceReference);
            }
        }
    }

    protected void processClassloadingRules(Environment environment) {
        //Process Hidden Class
        for (String hiddenClassPrefix : environment.getClassLoadingRules().getHiddenRule().getClassPrefixes()) {
            String inversedImportPackage = hiddenClassPrefix.endsWith("*") ? hiddenClassPrefix : hiddenClassPrefix + "*";
            environment.addImportPackage("!" + inversedImportPackage);
        }
        //Non-Overridable-Classes
        /*for (String hiddenClassPrefix : environment.getClassLoadingRules().getHiddenRule().getClassPrefixes()) {
            environment.getImportPackages().add(hiddenClassPrefix);
        }*/
    }

    protected OSGiBuildContext createOSGiBuildContext(Environment environment, OsgiMetaDataProvider osgiMetaDataProvider, boolean clientModule) throws IllegalConfigurationException {
        List<String> hiddenImportPackageNamePrefixes = new ArrayList<String>();
        Set<String> hiddenImportPackageNames = new HashSet<String>();

        Set<String> removedImportPackages = new HashSet<String>();
        for (String initialImportPackageName : environment.getImportPackages()) {
            String importPackageName = initialImportPackageName.trim();
            if (importPackageName.length() == 0) {
                removedImportPackages.add(initialImportPackageName);
                continue;
            }
            if (importPackageName.startsWith("!")) {
                importPackageName = importPackageName.substring(1);
                 boolean wildcardImportPakcageName = importPackageName.endsWith("*");
                if (wildcardImportPakcageName) {
                    importPackageName = importPackageName.substring(0, importPackageName.length() - 1);
                }
                if (importPackageName.endsWith(".")) {
                    importPackageName = importPackageName.substring(0, importPackageName.length() - 1);
                }
                if (wildcardImportPakcageName) {
                    hiddenImportPackageNamePrefixes.add(importPackageName);
                } else {
                    hiddenImportPackageNames.add(importPackageName);
                }
                removedImportPackages.add(initialImportPackageName);
            } else {
                if (importPackageName.endsWith("*")) {
                    throw new IllegalConfigurationException("wildchar * could not be used in the import-package " + importPackageName + " without ! prefix");
                }
            }
        }
        environment.removeImportPackages(removedImportPackages);

        //Use current filter configurations to re-validate existing import packages
        //This is used to handle the scenario that org.test and !org.test are configured at the same time
        removedImportPackages.clear();
        for (String importPackageName : environment.getImportPackages()) {
            if (hiddenImportPackageNames.contains(importPackageName)) {
                removedImportPackages.add(importPackageName);
                continue;
            }
            for (String hiddenImportPackageNamePrefix : hiddenImportPackageNamePrefixes) {
                if (importPackageName.startsWith(hiddenImportPackageNamePrefix)) {
                    removedImportPackages.add(importPackageName);
                    break;
                }
            }
        }
        environment.removeImportPackages(removedImportPackages);

        //If the users configured import packages, those will take the highest precedence.
        //It has the same effect as !org.test is configured.
        for (String importPackageName : environment.getImportPackages()) {
            List<HeaderElement> elements = HeaderParser.parseHeader(importPackageName);
            for (HeaderElement headerElement : elements) {
                hiddenImportPackageNames.add(headerElement.getName());
            }
        }

        Set<String> removedDynamicImportPackages = new HashSet<String>();
        for (String initialDynamicImportPackageName : environment.getDynamicImportPackages()) {
            String dynamicImportPackageName = initialDynamicImportPackageName.trim();
            if (dynamicImportPackageName.length() == 0) {
                removedDynamicImportPackages.add(initialDynamicImportPackageName);
                continue;
            }
            if (dynamicImportPackageName.startsWith("!")) {
                throw new IllegalConfigurationException("DynamicImport-Package " + dynamicImportPackageName + " could not be configured with ! prefix");
            } else {
                List<HeaderElement> elements = HeaderParser.parseHeader(dynamicImportPackageName);
                for (HeaderElement headerElement : elements) {
                    hiddenImportPackageNames.add(headerElement.getName());
                }
            }
        }
        environment.removeDynamicImportPackages(removedDynamicImportPackages);

        OSGiBuildContext osgiBuildContext = new OSGiBuildContext(environment, hiddenImportPackageNamePrefixes, hiddenImportPackageNames, osgiMetaDataProvider, environment.getClassLoadingRules()
                .isInverseClassLoading());
        osgiBuildContext.setClientModule(clientModule);
        if (clientModule) {
            osgiBuildContext.setClientArtifactResolver(getClientArtifactResolver());
        }
        return osgiBuildContext;
    }

    protected void processImportPackages(OSGiBuildContext context) {
        Environment environment = context.getEnvironment();
        Map<Long, Set<ExportPackage>> selectedExportPackages = exportPackagesSelector.select(context);
        for (Map.Entry<Long, Set<ExportPackage>> entry : selectedExportPackages.entrySet()) {
            if (context.isInverseClassLoading()) {
                for (ExportPackage exportPackage : entry.getValue()) {
                    String importPackageName = toImportPackageName(exportPackage);
                    if (importPackageName == null) {
                        continue;
                    }
                    environment.addDynamicImportPackage(importPackageName);
                }
            } else {
                for (ExportPackage exportPackage : entry.getValue()) {
                    String importPackageName = toImportPackageName(exportPackage);
                    if (importPackageName == null) {
                        continue;
                    }
                    environment.addImportPackage(importPackageName);
                }
            }
        }
        selectedExportPackages.clear();
    }

    protected String toImportPackageName(ExportPackage exportPackage) {
        /* If mandatory attribute exists, do we need to handle it ?
        if (exportPackage.getAttributes().get("mandatory") != null) {
            logger.info("Export package " + exportPackage.getName() + " has mandatory attribute, it will not be considered, if it is required, please add in the deployment plan");
            return null;
        }*/
        if (USE_FIXED_IMPORT_PACKAGE_VERSION) {
            return exportPackage.getName() + ";version=" + "\"[" + exportPackage.getVersion() + "," + exportPackage.getVersion() + "]\"";
        } else {
            return exportPackage.getVersion().equals(ZERO_VERSION) ? exportPackage.getName() : exportPackage.getName() + ";version=" + exportPackage.getVersion();
        }
    }

    private ArtifactResolver getClientArtifactResolver() {
        ServiceReference kernelReference = null;
        try {
            kernelReference = bundleContext.getServiceReference(Kernel.class.getName());
            if (kernelReference == null) {
                return null;
            }
            Kernel kernel = (Kernel) bundleContext.getService(kernelReference);
            return (ArtifactResolver) kernel.getGBean("ClientArtifactResolver");
        } catch (GBeanNotFoundException e) {
            logger.warn("Fail to get client artifact resolver, OSGi metadata for client module might not generate correctly", e);
            return null;
        } catch (InternalKernelException e) {
            logger.warn("Fail to get client artifact resolver, OSGi metadata for client module might not generate correctly", e);
            return null;
        } catch (IllegalStateException e) {
            logger.warn("Fail to get client artifact resolver, OSGi metadata for client module might not generate correctly", e);
            return null;
        } finally {
            if (kernelReference != null)
                try {
                    bundleContext.ungetService(kernelReference);
                } catch (Exception e) {
                }
        }
    }
}
