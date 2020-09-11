/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;


import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SampleDataAliasedParameterGroup {

  @Parameter
  @Alias("aliasedPayload")
  private String payload;

  @Parameter
  @Optional
  @Alias("aliasedAttributes")
  private String attributes;

  public String getPayload() {
    return payload;
  }

  public String getAttributes() {
    return attributes;
  }
}
