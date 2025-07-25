/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitsInApplication;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.puparser.PersistenceUnitXmlParser;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.vfs.VirtualFile;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

/**
 * Handle parsing of Persistence unit persistence.xml files
 * <p/>
 * The jar file/directory whose META-INF directory contains the persistence.xml file is termed the root of the persistence
 * unit.
 * root of a persistence unit must be one of the following:
 * EJB-JAR file
 * the WEB-INF/classes directory of a WAR file
 * jar file in the WEB-INF/lib directory of a WAR file
 * jar file in the EAR library directory
 * application client jar file
 *
 * @author Scott Marlow
 */
public class PersistenceUnitParseProcessor implements DeploymentUnitProcessor {

    private static final String WEB_PERSISTENCE_XML = "WEB-INF/classes/META-INF/persistence.xml";
    private static final String META_INF_PERSISTENCE_XML = "META-INF/persistence.xml";
    private static final String JAR_FILE_EXTENSION = ".jar";
    private static final String LIB_FOLDER = "lib";
    private static final PersistenceUnitMetadataHolder emptyholder = new PersistenceUnitMetadataHolder(Collections.emptyList());
    private final boolean inAppClientContainer;

    public PersistenceUnitParseProcessor(boolean inAppClientContainer) {
        this.inAppClientContainer = inAppClientContainer;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        handleWarDeployment(phaseContext);
        handleEarDeployment(phaseContext);
        handleJarDeployment(phaseContext);

        CapabilityServiceSupport capabilitySupport = phaseContext.getDeploymentUnit().getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        phaseContext.addDeploymentDependency(capabilitySupport.getCapabilityServiceName(JPAServiceNames.LOCAL_TRANSACTION_PROVIDER_CAPABILITY), JpaAttachments.LOCAL_TRANSACTION_PROVIDER);
        phaseContext.addDeploymentDependency(capabilitySupport.getCapabilityServiceName(JPAServiceNames.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY), JpaAttachments.TRANSACTION_SYNCHRONIZATION_REGISTRY);
    }

    private void handleJarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isEarDeployment(deploymentUnit) && !isWarDeployment(deploymentUnit) ) {

            // handle META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (!inAppClientContainer && DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
                // attach empty persistence unit holder
                deploymentRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, emptyholder);
                addApplicationDependenciesOnProvider (deploymentUnit, emptyholder);
                // if not in appclient container, do not deploy persistence units contained in appclient container archive
                VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                if (persistence_xml.exists() && persistence_xml.isFile() ) {
                    // log that the (app client container) persistence.xml will be ignored since we are in server mode
                    ROOT_LOGGER.ignoreAppclientPersistenceUnitsInServer(deploymentUnit.getName());
                }
                return;
            }
            // ordered list of PUs
            List<PersistenceUnitMetadataHolder> listPUHolders = new ArrayList<>(1);

            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders, deploymentUnit);
            PersistenceUnitMetadataHolder holder = normalize(listPUHolders);
            // save the persistent unit definitions
            deploymentRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, holder);
            markDU(holder, deploymentUnit);
            ROOT_LOGGER.tracef("parsed persistence unit definitions for jar %s", deploymentRoot.getRootName());

            incrementPersistenceUnitCount(deploymentUnit, holder.getPersistenceUnits().size());
            addApplicationDependenciesOnProvider( deploymentUnit, holder);
        }
    }

    private void handleWarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWarDeployment(deploymentUnit)) {

            int puCount;
            // ordered list of PUs
            List<PersistenceUnitMetadataHolder> listPUHolders = new ArrayList<PersistenceUnitMetadataHolder>(1);

            // handle WEB-INF/classes/META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders, deploymentUnit);
            PersistenceUnitMetadataHolder holder = normalize(listPUHolders);
            deploymentRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, holder);
            addApplicationDependenciesOnProvider( deploymentUnit, holder);
            markDU(holder, deploymentUnit);
            puCount = holder.getPersistenceUnits().size();

            // look for persistence.xml in jar files in the META-INF/persistence.xml directory (these are not currently
            // handled as subdeployments)
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getName().toLowerCase(Locale.ENGLISH).endsWith(JAR_FILE_EXTENSION)) {
                    listPUHolders = new ArrayList<PersistenceUnitMetadataHolder>(1);
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders, deploymentUnit);
                    holder = normalize(listPUHolders);
                    resourceRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, holder);
                    addApplicationDependenciesOnProvider( deploymentUnit, holder);
                    markDU(holder, deploymentUnit);
                    puCount += holder.getPersistenceUnits().size();
                }
            }
            ROOT_LOGGER.tracef("parsed persistence unit definitions for war %s", deploymentRoot.getRootName());

            incrementPersistenceUnitCount(deploymentUnit, puCount);
        }
    }

    private void handleEarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isEarDeployment(deploymentUnit)) {

            int puCount = 0;
            // ordered list of PUs
            List<PersistenceUnitMetadataHolder> listPUHolders = new ArrayList<PersistenceUnitMetadataHolder>(1);
            // handle META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
            parse(persistence_xml, listPUHolders, deploymentUnit);
            PersistenceUnitMetadataHolder holder = normalize(listPUHolders);
            deploymentRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, holder);
            addApplicationDependenciesOnProvider( deploymentUnit, holder);
            markDU(holder, deploymentUnit);
            puCount = holder.getPersistenceUnits().size();
            // Parsing persistence.xml in EJB jar/war files is handled as subdeployments.
            // We need to handle jars in the EAR/lib folder here
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot resourceRoot : resourceRoots) {
                // look at lib/*.jar files that aren't subdeployments (subdeployments are passed
                // to deploy(DeploymentPhaseContext)).
                if (!SubDeploymentMarker.isSubDeployment(resourceRoot) &&
                    resourceRoot.getRoot().getName().toLowerCase(Locale.ENGLISH).endsWith(JAR_FILE_EXTENSION) &&
                    resourceRoot.getRoot().getParent().getName().equals(LIB_FOLDER)) {
                    listPUHolders = new ArrayList<PersistenceUnitMetadataHolder>(1);
                    persistence_xml = resourceRoot.getRoot().getChild(META_INF_PERSISTENCE_XML);
                    parse(persistence_xml, listPUHolders, deploymentUnit);
                    holder = normalize(listPUHolders);
                    resourceRoot.putAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS, holder);
                    addApplicationDependenciesOnProvider( deploymentUnit, holder);
                    markDU(holder, deploymentUnit);
                    puCount += holder.getPersistenceUnits().size();
                }
            }
            ROOT_LOGGER.tracef("parsed persistence unit definitions for ear %s", deploymentRoot.getRootName());
            incrementPersistenceUnitCount(deploymentUnit, puCount);
        }
    }

    private void parse(
            final VirtualFile persistence_xml,
            final List<PersistenceUnitMetadataHolder> listPUHolders,
            final DeploymentUnit deploymentUnit)
        throws DeploymentUnitProcessingException {

        ROOT_LOGGER.tracef("parse checking if %s exists, result = %b",persistence_xml.toString(), persistence_xml.exists());
        if (persistence_xml.exists() && persistence_xml.isFile()) {
            InputStream is = null;
            try {
                is = persistence_xml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                PersistenceUnitMetadataHolder puHolder = PersistenceUnitXmlParser.parse(xmlReader, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));

                postParseSteps(persistence_xml, puHolder, deploymentUnit);
                listPUHolders.add(puHolder);
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(JpaLogger.ROOT_LOGGER.failedToParse(persistence_xml), e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Some of this might need to move to the install phase
     *
     * @param persistence_xml
     * @param puHolder
     */
    private void postParseSteps(
        final VirtualFile persistence_xml,
        final PersistenceUnitMetadataHolder puHolder,
        final DeploymentUnit deploymentUnit ) {

        for (PersistenceUnitMetadata pu : puHolder.getPersistenceUnits()) {
            // set URLs
            List<URL> jarfilesUrls = new ArrayList<URL>();
            if (pu.getJarFiles() != null) {
                for (String jar : pu.getJarFiles()) {
                    jarfilesUrls.add(getRelativeURL(persistence_xml, jar));
                }
            }
            pu.setJarFileUrls(jarfilesUrls);
            URL url = getPersistenceUnitURL(persistence_xml);
            pu.setPersistenceUnitRootUrl(url);
            String scopedPersistenceUnitName;

            /**
             * WFLY-5478 allow custom scoped persistence unit name hint in persistence unit definition.
             * Specified scoped persistence unit name needs to be unique across application server deployments.
             * Application is responsible for picking a unique name.
             * Currently, a non-unique name will result in a DuplicateServiceException deployment failure:
             *   org.jboss.msc.service.DuplicateServiceException: Service jboss.persistenceunit.my2lccustom#test_pu.__FIRST_PHASE__ is already registered
             */
            scopedPersistenceUnitName = Configuration.getScopedPersistenceUnitName(pu);
            if (scopedPersistenceUnitName == null) {
                scopedPersistenceUnitName = createBeanName(deploymentUnit, pu.getPersistenceUnitName());
            } else {
                ROOT_LOGGER.tracef("persistence unit '%s' specified a custom scoped persistence unit name hint " +
                        "(jboss.as.jpa.scopedname=%s).  The specified name *must* be unique across all application server deployments.",
                        pu.getPersistenceUnitName(),
                        scopedPersistenceUnitName);
                if (scopedPersistenceUnitName.indexOf('/') != -1) {
                    throw JpaLogger.ROOT_LOGGER.invalidScopedName(scopedPersistenceUnitName, '/');
                }
            }

            pu.setScopedPersistenceUnitName(scopedPersistenceUnitName);

            pu.setContainingModuleName(getContainingModuleName(deploymentUnit));
        }
    }

    private static URL getRelativeURL(VirtualFile persistence_xml, String jar) {
        try {
            return new URL(jar);
        } catch (MalformedURLException e) {
            try {
                VirtualFile deploymentUnitFile = persistence_xml;
                //we need the parent 3 units up, 1 is META-INF, 2nd is the actual jar, 3rd is the jar files parent
                VirtualFile parent = deploymentUnitFile.getParent().getParent().getParent();
                VirtualFile baseDir = (parent != null ? parent : deploymentUnitFile);
                VirtualFile jarFile = baseDir.getChild(jar);
                if (jarFile == null)
                    throw JpaLogger.ROOT_LOGGER.childNotFound(jar, baseDir);
                return jarFile.toURL();
            } catch (Exception e1) {
                throw JpaLogger.ROOT_LOGGER.relativePathNotFound(e1, jar);
            }
        }
    }

    private URL getPersistenceUnitURL(VirtualFile persistence_xml) {
        try {
            VirtualFile metaData = persistence_xml;// di.getMetaDataFile("persistence.xml");
            return metaData.getParent().getParent().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Eliminate duplicate PU definitions from clustering the deployment (first definition will win)
     * <p/>
     * Jakarta Persistence 8.2  A persistence unit must have a name. Only one persistence unit of any given name must be defined
     * within a single EJB-JAR file, within a single WAR file, within a single application client jar, or within
     * an EAR. See Section 8.2.2, “Persistence Unit Scope”.
     *
     * @param listPUHolders
     * @return
     */
    private PersistenceUnitMetadataHolder normalize(List<PersistenceUnitMetadataHolder> listPUHolders) {
        // eliminate duplicates (keeping the first instance of each PU by name)
        Map<String, PersistenceUnitMetadata> flattened = new HashMap<String, PersistenceUnitMetadata>();
        for (PersistenceUnitMetadataHolder puHolder : listPUHolders) {
            for (PersistenceUnitMetadata pu : puHolder.getPersistenceUnits()) {
                if (!flattened.containsKey(pu.getPersistenceUnitName())) {
                    flattened.put(pu.getPersistenceUnitName(), pu);
                } else {
                    PersistenceUnitMetadata first = flattened.get(pu.getPersistenceUnitName());
                    PersistenceUnitMetadata duplicate = pu;
                    ROOT_LOGGER.duplicatePersistenceUnitDefinition(duplicate.getPersistenceUnitName(), first.getScopedPersistenceUnitName(), duplicate.getScopedPersistenceUnitName());
                }
            }
        }
        PersistenceUnitMetadataHolder holder = new PersistenceUnitMetadataHolder(new ArrayList<PersistenceUnitMetadata>(flattened.values()));
        return holder;
    }

    static boolean isEarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.EAR, context));
    }

    static boolean isWarDeployment(final DeploymentUnit context) {
        return (DeploymentTypeMarker.isType(DeploymentType.WAR, context));
    }

    private static String getScopedDeploymentUnitPath(DeploymentUnit deploymentUnit) {
        ArrayList<String> parts = new ArrayList<String>();  // order of deployment elements will start with parent

        do {
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            DeploymentUnit parentdeploymentUnit = deploymentUnit.getParent();
            if (parentdeploymentUnit != null) {
                ResourceRoot parentDeploymentRoot = parentdeploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                parts.add(0, deploymentRoot.getRoot().getPathNameRelativeTo(parentDeploymentRoot.getRoot()));
            } else {
                parts.add(0, deploymentRoot.getRoot().getName());
            }
        }
        while ((deploymentUnit = deploymentUnit.getParent()) != null);

        StringBuilder result = new StringBuilder();
        boolean needSeparator = false;
        for (String part : parts) {
            if (needSeparator) {
                result.append('/');
            }
            result.append(part);
            needSeparator = true;
        }
        return result.toString();
    }

    // make scoped pu name (e.g. test2.ear/w2.war#warPUnit_PU or test3.jar#CTS-EXT-UNIT)
    // old as6 names looked like:  persistence.unit:unitName=ejb3_ext_propagation.ear/lib/ejb3_ext_propagation.jar#CTS-EXT-UNIT
    public static String createBeanName(DeploymentUnit deploymentUnit, String persistenceUnitName) {
        // persistenceUnitName must be a simple name
        if (persistenceUnitName.indexOf('/') != -1) {
            throw JpaLogger.ROOT_LOGGER.invalidPersistenceUnitName(persistenceUnitName, '/');
        }
        if (persistenceUnitName.indexOf('#') != -1) {
            throw JpaLogger.ROOT_LOGGER.invalidPersistenceUnitName(persistenceUnitName, '#');
        }

        String unitName = getScopedDeploymentUnitPath(deploymentUnit) + "#" + persistenceUnitName;
        return unitName;
    }

    // returns the EE deployment module name.
    // For top level deployments, only one element will be returned representing the top level module.
    // For sub-deployments, the first element identifies the top level module and the second element is the sub-module.
    private ArrayList<String> getContainingModuleName(DeploymentUnit deploymentUnit) {
        ArrayList<String> parts = new ArrayList<String>();  // order of deployment elements will start with parent

        do {
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            DeploymentUnit parentdeploymentUnit = deploymentUnit.getParent();
            if (parentdeploymentUnit != null) {
                ResourceRoot parentDeploymentRoot = parentdeploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                parts.add(0, deploymentRoot.getRoot().getPathNameRelativeTo(parentDeploymentRoot.getRoot()));
            } else {
                parts.add(0, deploymentRoot.getRoot().getName());
            }
        }
        while ((deploymentUnit = deploymentUnit.getParent()) != null);

        return parts;

    }


    private void markDU(PersistenceUnitMetadataHolder holder, DeploymentUnit deploymentUnit) {
        if (holder.getPersistenceUnits() != null && !holder.getPersistenceUnits().isEmpty()) {
            JPADeploymentMarker.mark(deploymentUnit);

            // Hibernate Search backend type detection
            for (PersistenceUnitMetadata persistenceUnit : holder.getPersistenceUnits()) {
                Properties properties = persistenceUnit.getProperties();
                String backendType = properties == null ? null
                        : trimToNull(properties.getProperty(Configuration.HIBERNATE_SEARCH_BACKEND_TYPE));
                if (backendType != null) {
                    HibernateSearchDeploymentMarker.markBackendType(deploymentUnit, backendType);
                }
                String coordinationStrategy = properties == null ? null
                        : trimToNull(properties.getProperty(Configuration.HIBERNATE_SEARCH_COORDINATION_STRATEGY));
                if (coordinationStrategy != null) {
                    HibernateSearchDeploymentMarker.markCoordinationStrategy(deploymentUnit, coordinationStrategy);
                }
            }
        }
    }

    private String trimToNull(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }
        return string;
    }

    private void incrementPersistenceUnitCount(DeploymentUnit topDeploymentUnit, int persistenceUnitCount) {
        topDeploymentUnit = DeploymentUtils.getTopDeploymentUnit(topDeploymentUnit);

        // create persistence unit counter if not done already
        synchronized (topDeploymentUnit) {  // ensure that only one deployment thread sets this at a time
            PersistenceUnitsInApplication persistenceUnitsInApplication = getPersistenceUnitsInApplication(topDeploymentUnit);
            persistenceUnitsInApplication.increment(persistenceUnitCount);
        }
        ROOT_LOGGER.tracef("incrementing PU count for %s by %d", topDeploymentUnit.getName(), persistenceUnitCount);
    }

    private void addApplicationDependenciesOnProvider(DeploymentUnit topDeploymentUnit, PersistenceUnitMetadataHolder holder) {
        topDeploymentUnit = DeploymentUtils.getTopDeploymentUnit(topDeploymentUnit);

        synchronized (topDeploymentUnit) {  // ensure that only one deployment thread sets this at a time
            PersistenceUnitsInApplication persistenceUnitsInApplication = getPersistenceUnitsInApplication(topDeploymentUnit);
            persistenceUnitsInApplication.addPersistenceUnitHolder(holder);
        }
    }

    private PersistenceUnitsInApplication getPersistenceUnitsInApplication(DeploymentUnit topDeploymentUnit) {

        PersistenceUnitsInApplication persistenceUnitsInApplication = topDeploymentUnit.getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);
        if (persistenceUnitsInApplication == null) {
            persistenceUnitsInApplication = new PersistenceUnitsInApplication();
            topDeploymentUnit.putAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION, persistenceUnitsInApplication);
        }
        return persistenceUnitsInApplication;
    }
}
