/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.domain.AmbiguousDomainReferenceException;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultApplicationFactoryTestCase extends AbstractMuleTestCase {

  private static final String DOMAIN_NAME = "test-domain";
  private static final String APP_NAME = "test-app";
  private static final String FAKE_ARTIFACT_PLUGIN = "fake-artifact-plugin";
  public static final String APP_ID = "test-app-id";
  private static final String DOMAIN_ARTIFACT_FILE_NAME = "test-domain-1.0.0-mule-domain";

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);
  private final DefaultDomainManager domainRepository = spy(new DefaultDomainManager());
  private final ApplicationDescriptorFactory applicationDescriptorFactory = mock(ApplicationDescriptorFactory.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository = mock(ExtensionModelLoaderRepository.class);
  private final ClassLoaderRepository classLoaderRepository = mock(ClassLoaderRepository.class);
  private final PluginDependenciesResolver pluginDependenciesResolver = mock(PluginDependenciesResolver.class);
  private final PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory =
      mock(PolicyTemplateClassLoaderBuilderFactory.class);
  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader = mock(ArtifactPluginDescriptorLoader.class);
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                    domainRepository, serviceRepository,
                                    extensionModelLoaderRepository,
                                    classLoaderRepository, policyTemplateClassLoaderBuilderFactory, pluginDependenciesResolver,
                                    artifactPluginDescriptorLoader,
                                    discoverLicenseValidator(getClass().getClassLoader()),
                                    getRuntimeLockFactory(),
                                    mock(MemoryManagementService.class),
                                    mock(ArtifactConfigurationProcessor.class));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createsApplication() throws Exception {
    final ApplicationDescriptor descriptor = new ApplicationDescriptor(APP_NAME);
    descriptor.setClassLoaderModel(createClassLoaderModelWithDomain());
    final File[] resourceFiles = new File[] {new File("mule-config.xml")};
    when(applicationDescriptorFactory.create(any(), any())).thenReturn(descriptor);

    final ArtifactPluginDescriptor coreArtifactPluginDescriptor = new ArtifactPluginDescriptor(FAKE_ARTIFACT_PLUGIN);
    coreArtifactPluginDescriptor.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().build());

    final ArtifactPlugin appPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(appPlugin.getArtifactClassLoader()).thenReturn(artifactClassLoader);
    when(artifactClassLoader.getArtifactId()).thenReturn(FAKE_ARTIFACT_PLUGIN);
    when(appPlugin.getDescriptor()).thenReturn(coreArtifactPluginDescriptor);

    final Domain domain = createDomain(DOMAIN_NAME);

    final ClassLoaderLookupPolicy sharedLibLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(domain.getArtifactClassLoader().getClassLoaderLookupPolicy().extend(anyMap()))
        .thenReturn(sharedLibLookupPolicy);

    final MuleApplicationClassLoader applicationArtifactClassLoader = mock(MuleApplicationClassLoader.class);
    when(applicationArtifactClassLoader.getArtifactId()).thenReturn(APP_ID);

    ApplicationClassLoaderBuilder applicationClassLoaderBuilderMock = mock(ApplicationClassLoaderBuilder.class);
    when(applicationClassLoaderBuilderMock.setDomainParentClassLoader(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactDescriptor(any()))
        .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0])))
            .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.build()).thenReturn(applicationArtifactClassLoader);
    when(applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder())
        .thenReturn(applicationClassLoaderBuilderMock);

    List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
    pluginClassLoaders.add(artifactClassLoader);
    when(applicationArtifactClassLoader.getArtifactPluginClassLoaders()).thenReturn(pluginClassLoaders);

    final Application application = applicationFactory.createArtifact(new File(APP_NAME), empty());

    assertThat(application.getDomain(), is(domain));
    assertThat(application.getArtifactClassLoader(), is(applicationArtifactClassLoader));
    assertThat(application.getDescriptor(), is(descriptor));
    assertThat(application.getArtifactName(), is(APP_NAME));
    assertThat(application.getResourceFiles(), is(resourceFiles));

    verify(domainRepository, times(1)).getCompatibleDomain(any());
    verify(domainRepository, times(1)).getDomain(any());
    verify(applicationClassLoaderBuilderMock)
        .setDomainParentClassLoader((ArtifactClassLoader) domain.getArtifactClassLoader().getClassLoader().getParent());
    verify(applicationClassLoaderBuilderMock)
        .addArtifactPluginDescriptors(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0]));
    verify(applicationClassLoaderBuilderMock).setArtifactDescriptor(descriptor);
  }

  private ClassLoaderModel createClassLoaderModelWithDomain() {
    BundleDescriptor domainDescriptor = createDomainBundleDescriptor(DOMAIN_NAME);
    Set<BundleDependency> domainDependency =
        Collections.singleton(new BundleDependency.Builder().setDescriptor(domainDescriptor).build());
    return new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(domainDependency).build();
  }

  private BundleDescriptor createDomainBundleDescriptor(String domainName) {
    return new BundleDescriptor.Builder().setArtifactId(domainName).setGroupId("test")
        .setVersion("1.0.0").setClassifier(MULE_DOMAIN_CLASSIFIER).build();
  }

  private Domain createDomain(String name) throws DomainNotFoundException, AmbiguousDomainReferenceException {
    final Domain domain = mock(Domain.class);
    final ArtifactClassLoader domainArtifactClassLoader = mock(ArtifactClassLoader.class);
    when(domainArtifactClassLoader.getClassLoader()).thenReturn(mock(ClassLoader.class));

    final ClassLoaderLookupPolicy domainLookupPolicy = mock(ClassLoaderLookupPolicy.class);
    when(domainArtifactClassLoader.getClassLoaderLookupPolicy()).thenReturn(domainLookupPolicy);
    when(domain.getArtifactClassLoader()).thenReturn(domainArtifactClassLoader);

    DomainDescriptor descriptor = new DomainDescriptor(DOMAIN_ARTIFACT_FILE_NAME);
    descriptor.setBundleDescriptor(createDomainBundleDescriptor(name));
    when(domain.getDescriptor()).thenReturn(descriptor);

    domainRepository.addDomain(domain);

    return domain;
  }

  @Test
  public void applicationDeployFailDueToDomainNotDeployed() throws Exception {
    final ApplicationDescriptor descriptor = new ApplicationDescriptor(APP_NAME);
    descriptor.setClassLoaderModel(createClassLoaderModelWithDomain());
    when(applicationDescriptorFactory.create(any(), any())).thenReturn(descriptor);
    expectedException.expect(DeploymentException.class);
    applicationFactory.createArtifact(new File(APP_NAME), empty());
  }
}
