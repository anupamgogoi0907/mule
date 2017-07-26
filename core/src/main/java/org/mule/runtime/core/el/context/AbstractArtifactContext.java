/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;

import java.util.Map;

/**
 * Base class for exposing the artifact context to the expression language.
 * 
 * Exposes information about the current Mule Application:
 * <li><b>encoding</b> <i>Artifact default encoding</i>
 * <li><b>name</b> <i>Artifact name</i>
 * <li><b>registry</b> <i>Artifact registry (as a map)</i>
 * <li><b>standalone</b> <i>If Mule is running standalone</i>
 * <li><b>workdir</b> <i>Artifact work directory</i>
 * 
 * @since 4.0
 */
public abstract class AbstractArtifactContext {

  protected MuleContext muleContext;

  public AbstractArtifactContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public String getName() {
    return muleContext.getConfiguration().getId();
  }

  public String getWorkDir() {
    return muleContext.getConfiguration().getWorkingDirectory();
  }

  public String getEncoding() {
    return muleContext.getConfiguration().getDefaultEncoding();
  }

  public boolean isStandalone() {
    return muleContext.getConfiguration().isStandalone();
  }

  public Map<String, Object> getRegistry() {
    return createRegistry(muleContext.getRegistry());
  }

  /**
   * Creates the registry wrapper to expose to the EL.
   * 
   * @param registry the mule internal registry
   * @return the registry wrapper.
   */
  protected abstract Map<String, Object> createRegistry(MuleRegistry registry);

}
