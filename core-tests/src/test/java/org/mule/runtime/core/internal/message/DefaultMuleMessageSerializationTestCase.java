/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;

import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.apache.commons.lang3.SerializationUtils;

import org.junit.After;
import org.junit.Test;

public class DefaultMuleMessageSerializationTestCase extends AbstractMuleContextTestCase {

  @After
  public void teardown() {
    currentMuleContext.set(null);
  }

  @Test
  public void testSerializablePayload() throws Exception {
    final Message message = InternalMessage.builder().value(TEST_MESSAGE).addOutboundProperty("foo", "bar").build();
    Message deserializedMessage = serializationRoundtrip(message);

    assertEquals(TEST_MESSAGE, deserializedMessage.getPayload().getValue());
    assertEquals("bar", ((InternalMessage) deserializedMessage).getOutboundProperty("foo"));
  }

  private InternalMessage serializationRoundtrip(Message message) throws Exception {
    return (InternalMessage) SerializationUtils.deserialize(SerializationUtils.serialize(message));
  }

}
