/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.adoption.metrics.internal;

import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Once the server has started, and then once a day, writes the adoption
 * metrics files that IBM License Metric Tool collects. Nothing is sent over the
 * network by this component.
 */
@Component(immediate = true, configurationPid = "io.openliberty.adoption.metrics.AdoptionMetricsComponent", configurationPolicy = ConfigurationPolicy.OPTIONAL)
@SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CheckpointPhase.CONDITION_PROCESS_RUNNING_ID + ")")
public class AdoptionMetricsComponent {

    private static final TraceComponent tc = Tr.register(AdoptionMetricsComponent.class);

    /** Component ids from the SWID tags that identify each product to License Metric Tool. */
    static final String COMPONENT_ID_OPEN_LIBERTY = "0ca5af184810448c851299b2c615ebb6";
    static final String COMPONENT_ID_WEBSPHERE_LIBERTY = "fbf6a96d49214c0abc6a3bc5da6e48cd";

    static final String PRODUCT_ID_WEBSPHERE = "com.ibm.websphere.appserver";
    static final String PRODUCT_ID_OPEN_LIBERTY = "io.openliberty";

    private static final String SPRING_APP_FACTORY_PID = "com.ibm.ws.app.manager.springappcfg";
    private static final String SPRING_APP_TYPE = "spring";
    /** Applications are configured under the generic factory, or under an extending one such as Spring Boot. */
    private static final String APP_FACTORY_FILTER = "(|(service.factoryPid=com.ibm.ws.app.manager)(service.factoryPid="
                                                     + SPRING_APP_FACTORY_PID + "))";
    private static final String FILE_PREFIX = "liberty";

    private final FeatureProvisioner featureProvisioner;
    private final ServerInfoMBean serverInfo;
    private final ConfigurationAdmin configAdmin;
    private final WsLocationAdmin locationAdmin;
    private final ScheduledExecutorService scheduledExecutor;

    private volatile Map<String, Object> properties;
    private ScheduledFuture<?> future;

    @Activate
    public AdoptionMetricsComponent(ComponentContext ignored, Map<String, Object> properties,
                                    @Reference ServerStartedPhase2 justAMarker,
                                    @Reference FeatureProvisioner featureProvisioner,
                                    @Reference ServerInfoMBean serverInfo,
                                    @Reference ConfigurationAdmin configAdmin,
                                    @Reference WsLocationAdmin locationAdmin,
                                    @Reference(target = "(deferrable=true)") ScheduledExecutorService scheduledExecutor) {
        this.featureProvisioner = featureProvisioner;
        this.serverInfo = serverInfo;
        this.configAdmin = configAdmin;
        this.locationAdmin = locationAdmin;
        this.scheduledExecutor = scheduledExecutor;
        this.properties = properties;
        schedule();
    }

    @Modified
    protected synchronized void modified(Map<String, Object> properties) {
        this.properties = properties;
        cancel();
        schedule();
    }

    @Deactivate
    protected synchronized void deactivate() {
        cancel();
    }

    private synchronized void schedule() {
        if (!isEnabled(properties)) {
            return;
        }
        future = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                writeOnce();
            }
        }, 0, 1, TimeUnit.DAYS);
    }

    private synchronized void cancel() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    static boolean isEnabled(Map<String, Object> properties) {
        return !Boolean.FALSE.equals(properties.get("enabled"));
    }

    /** Never throws: a failure here must not stop the daily schedule or the server. */
    void writeOnce() {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    try {
                        write();
                    } catch (IOException e) {
                        debug("Could not write adoption metrics files: " + e);
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            debug("Unexpected failure collecting adoption metrics: " + e);
        }
    }

    private void write() throws IOException {
        Map<String, ? extends ProductInfo> products;
        try {
            products = ProductInfo.getAllProductInfo();
        } catch (Exception e) {
            debug("Could not read product information: " + e);
            return;
        }

        ProductInfo product = products.get(PRODUCT_ID_WEBSPHERE);
        String componentId = COMPONENT_ID_WEBSPHERE_LIBERTY;
        if (product == null) {
            product = products.get(PRODUCT_ID_OPEN_LIBERTY);
            componentId = COMPONENT_ID_OPEN_LIBERTY;
        }
        if (product == null) {
            debug("No Liberty product information found; skipping adoption metrics");
            return;
        }

        Map<String, Object> props = properties;
        String installDir = serverInfo.getInstallDirectory();

        // The product's own SWID tag is the source of truth; the constants are only a fallback.
        SwidTag tag = SwidTag.find(new File(installDir, "lib/versions/tags"));
        if (tag != null) {
            componentId = tag.persistentId;
        }
        String componentName = tag != null && tag.name != null ? tag.name : product.getName();

        AdoptionDataCollector.Inputs in = new AdoptionDataCollector.Inputs();
        in.productName = product.getName();
        in.productVersion = product.getVersion();
        in.productEdition = product.getEdition();
        in.beta = product.isBeta();
        in.features = publicFeatures();
        Configuration[] appConfigs = applicationConfigurations();
        in.applicationCount = appConfigs.length;
        in.springBootDeployed = hasSpringBootApplication(appConfigs);
        ApplicationInspector.Result found = inspectApplications(appConfigs);
        in.springBootDeployed |= found.springBoot;
        in.microProfileDeployed = found.microProfile;
        in.jakartaEeDeployed = found.jakartaEe;
        in.environmentType = asString(props.get("environmentType"));
        in.customerId = asString(props.get("customerId"));
        in.installDirectory = installDir;
        in.serverName = serverInfo.getName();
        in.hostName = hostName();

        Map<String, String> sysProps = new HashMap<String, String>();
        for (String key : new String[] { "java.version", "java.vendor", "java.vm.name", "os.name", "os.version" }) {
            sysProps.put(key, System.getProperty(key));
        }

        Map<String, Object> data = new AdoptionDataCollector(EnvironmentDetector.system(), sysProps).collect(in);

        String outputDir = asString(props.get("outputDirectory"));
        File base = new File(outputDir == null || outputDir.isEmpty() ? installDir : outputDir);
        PayloadWriter writer = new PayloadWriter(Paths.get(base.getPath()), FILE_PREFIX);
        writer.writeMetadata(componentId, componentName, installDir);
        writer.appendRecord(data, System.currentTimeMillis());
    }

    private Set<String> publicFeatures() {
        Set<String> result = new TreeSet<String>();
        for (String feature : featureProvisioner.getInstalledFeatures()) {
            FeatureDefinition fd = featureProvisioner.getFeatureDefinition(feature);
            if (fd != null && fd.getVisibility() == Visibility.PUBLIC
                && !fd.getSymbolicName().contains("versionless")
                && !fd.getFeatureName().contains(":")) {
                result.add(fd.getFeatureName());
            }
        }
        return result;
    }

    /** Application configurations, which include dropins applications. */
    private Configuration[] applicationConfigurations() {
        try {
            Configuration[] configs = configAdmin.listConfigurations(APP_FACTORY_FILTER);
            return configs == null ? new Configuration[0] : configs;
        } catch (IOException | InvalidSyntaxException e) {
            debug("Could not list applications: " + e);
            return new Configuration[0];
        }
    }

    /** Looks inside each deployed application for the frameworks it is built on. */
    private ApplicationInspector.Result inspectApplications(Configuration[] configs) {
        ApplicationInspector.Resolver resolver = new ApplicationInspector.Resolver() {
            @Override
            public String resolve(String value) {
                return locationAdmin.resolveString(value);
            }
        };
        ApplicationInspector.Result all = new ApplicationInspector.Result();
        for (Configuration config : configs) {
            try {
                Dictionary<String, Object> p = config.getProperties();
                Object location = p == null ? null : p.get("location");
                if (location != null) {
                    all.merge(ApplicationInspector.inspect(ApplicationInspector.locate(location.toString(), resolver)));
                }
            } catch (RuntimeException e) {
                debug("Could not inspect an application: " + e);
            }
        }
        return all;
    }

    /** True when at least one deployed application is a Spring Boot application. */
    static boolean hasSpringBootApplication(Configuration[] configs) {
        for (Configuration config : configs) {
            if (SPRING_APP_FACTORY_PID.equals(config.getFactoryPid())) {
                return true;
            }
            Dictionary<String, Object> p = config.getProperties();
            if (p != null && SPRING_APP_TYPE.equals(p.get("type"))) {
                return true;
            }
        }
        return false;
    }

    private static String hostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException | RuntimeException e) {
            return "";
        }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static void debug(String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, message);
        }
    }
}
