/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;

/**
 * Ensures consistent access to the {@link ErrorTypeRepository} from validations.
 */
public abstract class AbstractErrorTypesValidation implements Validation {

  protected static final String RAISE_ERROR = "raise-error";

  protected static final String ON_ERROR = "on-error";
  protected static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  protected static final String ON_ERROR_CONTINUE = "on-error-continue";

  protected static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();

  protected static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  protected static final ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  protected static final ComponentIdentifier ON_ERROR_CONTINUE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();

  protected static boolean errorMappingPresent(ComponentAst operationComponent) {
    final ComponentParameterAst errorMappingsAst = operationComponent.getParameter(ERROR_MAPPINGS_PARAMETER_NAME);
    if (errorMappingsAst == null) {
      return false;
    }
    return errorMappingsAst != null && !((List<ErrorMapping>) errorMappingsAst.getValue().getRight()).isEmpty();
  }

  protected static List<ErrorMapping> getErrorMappings(ComponentAst component) {
    return (List<ErrorMapping>) component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME).getValue().getRight();
  }

  protected static Optional<ErrorType> lookup(ComponentAst component, String errorTypeParamName, ArtifactAst artifact) {
    return artifact.getErrorTypeRepository()
        .lookupErrorType(parserErrorType(component.getParameter("type").getResolvedRawValue()));
  }

  protected static ComponentIdentifier parserErrorType(String representation) {
    int separator = representation.indexOf(':');
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_PREFIX.toUpperCase();
      identifier = representation.toUpperCase();
    }

    return builder().name(identifier).namespace(namespace).build();
  }


}
