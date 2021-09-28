/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
public class DefaultExpressionManagerTestCase extends AbstractMuleContextTestCase {

  private static final String MY_VAR = "myVar";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamingManager streamingManager;

  private ExtendedExpressionManager expressionManager;

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = new DefaultExpressionManager();
    initialiseIfNeeded(expressionManager, muleContext);
  }

  @Test
  @Description("Verifies that global bindings can be added.")
  public void globals() {
    DataType integerType = fromType(Integer.class);

    ExpressionFunction multiply = new ExpressionFunction() {

      @Override
      public Object call(Object[] objects, BindingContext bindingContext) {
        return ((Integer) objects[0]) * ((Integer) objects[1]);
      }

      @Override
      public Optional<DataType> returnType() {
        return of(integerType);
      }

      @Override
      public List<FunctionParameter> parameters() {
        return asList(new FunctionParameter("x", integerType),
                      new FunctionParameter("y", integerType));
      }
    };

    BindingContext globalContext = builder()
        .addBinding("aNum", new TypedValue<>(4, fromType(Integer.class)))
        .addBinding("times", new TypedValue<>(multiply, fromFunction(multiply)))
        .build();

    expressionManager.addGlobalBindings(globalContext);

    TypedValue result = expressionManager.evaluate("aNum times 5");
    assertThat(result.getValue(), is(20));

    expressionManager.addGlobalBindings(builder().addBinding("otherNum", new TypedValue(3, integerType)).build());

    result = expressionManager.evaluate("(times(7, 3) + otherNum) / aNum");
    assertThat(result.getValue(), is(6));
  }

  @Test
  @Description("Verifies that a simple literal expression is successful.")
  public void simple() {
    String expression = "\"wow\"";
    assertString(expression);
  }

  @Test
  @Description("Verifies that a simple literal expression is successful when using brackets.")
  public void simpleEnclosed() {
    assertString("#[\"wow\"]");
  }

  private void assertString(String expression) {
    assertThat(expressionManager.evaluate(expression).getValue(), is("wow"));
  }

  @Test
  @Description("Verifies that a map expression is successful.")
  public void map() {
    String expression = "{\'name\' : \'Sarah\', \'surname\' : \'Manning\'}";
    Object result = expressionManager.evaluate(expression).getValue();
    assertThat(result, is(instanceOf(Map.class)));
    assertThat((Map<String, String>) result, hasEntry("name", "Sarah"));
    assertThat((Map<String, String>) result, hasEntry("surname", "Manning"));
  }

  @Test
  @Description("Verifies that custom variables are considered.")
  public void simpleCustomVariable() {
    Object object = new Object();
    BindingContext context = builder().addBinding(MY_VAR, new TypedValue(object, OBJECT)).build();
    assertThat(expressionManager.evaluate("#[myVar]", context).getValue(), equalTo(object));
  }

  @Test
  @Description("Verifies that the flow variable exposing it's name works.")
  public void flowName() throws MuleException {
    FlowConstruct mockFlowConstruct = mock(FlowConstruct.class);
    when(mockFlowConstruct.getName()).thenReturn("myFlowName");

    String result = (String) expressionManager
        .evaluate("#[flow.name]", testEvent(), fromSingleComponent(mockFlowConstruct.getName())).getValue();
    assertThat(result, is(mockFlowConstruct.getName()));
  }

  @Test
  @Description("Verifies that payload variable works.")
  public void payloadVariable() throws MuleException {
    assertThat(expressionManager.evaluate("payload", testEvent()).getValue(), is(TEST_PAYLOAD));
  }

  @Test
  @Description("Verifies that flowVars work, returning null for non existent ones and it's value for those that do.")
  public void flowVars() throws MuleException {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(testEvent());
    String flowVars = "vars.myVar";
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), nullValue());
    String value = "Leda";
    eventBuilder.addVariable(MY_VAR, value);
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), is(value));
  }

  @Test
  @Description("Verifies that a simple transformation works.")
  public void transformation() throws MuleException {
    String expression = "payload";
    TypedValue result = expressionManager.evaluate(expression, BYTE_ARRAY, builder().build(), testEvent());
    assertThat(result.getValue(), is(TEST_PAYLOAD.getBytes()));
    assertThat(result.getDataType(), is(BYTE_ARRAY));
  }

  @Test
  @Description("Verifies that a simple transformation works even when it's not required.")
  public void transformationNotNeeded() throws MuleException {
    String expression = "payload";
    TypedValue result = expressionManager.evaluate(expression, STRING, builder().build(), testEvent());
    assertThat(result.getValue(), is(TEST_PAYLOAD));
    assertThat(result.getDataType(), is(STRING));
  }

  @Test
  @Description("Verifies that parsing works with inner expressions in MVEL but only with regular ones in DW.")
  public void parseCompatibility() throws MuleException {
    assertThat(expressionManager.parse("#['this is ' ++ payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
    expectedException.expect(RuntimeException.class);
    expressionManager.parse("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION);
  }

  @Test
  @Description("Verifies that parsing works for plain String scenarios.")
  public void parse() throws MuleException {
    String expression = "this is a test";
    assertThat(expressionManager.parse(expression, testEvent(), TEST_CONNECTOR_LOCATION), is(expression));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL.")
  public void parseLog() throws MuleException {
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL using the message.")
  public void parseLogMessage() throws MuleException {
    String expectedOutput =
        "current message is \norg.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation\n{"
            + "\n  payload=test\n  mediaType=*/*\n  attributes=<not set>\n  attributesMediaType=*/*\n}";
    assertThat(expressionManager.parseLogTemplate("current message is #[message]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(equalToIgnoringLineBreaks(expectedOutput)));
  }

  @Test
  public void isValid() {
    String expression = "2*2";
    assertThat(expressionManager.isValid(expression), is(true));
  }

  @Test
  public void isInvalid() {
    String expression = "2*'2";
    assertThat(expressionManager.isValid(expression), is(false));
  }

  @Test
  public void isExpression() {
    assertThat(expressionManager.isExpression("2*2 + #[var]"), is(true));
    assertThat(expressionManager.isExpression("#[var]"), is(true));
    assertThat(expressionManager.isExpression("${var}"), is(false));
  }

  protected void disableMel() throws InitialisationException {
    Registry registry = mock(Registry.class);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class))
        .thenReturn(of(new WeaveDefaultExpressionLanguageFactoryService(null)));
    when(registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED)).thenReturn(empty());

    final MuleContextWithRegistry mockMuleContext = mockMuleContext();
    MuleConfiguration config = mockMuleContext.getConfiguration();
    doReturn(true).when(config).isValidateExpressions();

    expressionManager = new DefaultExpressionManager();
    ((DefaultExpressionManager) expressionManager).setRegistry(registry);
    ((DefaultExpressionManager) expressionManager).setMuleContext(mockMuleContext);
    ((DefaultExpressionManager) expressionManager).setEncodingSupplier(() -> UTF_8);
    initialiseIfNeeded(expressionManager, false, mockMuleContext);
  }

  @Test
  public void melPrefixIsValidDw() throws MuleException {
    disableMel();

    assertThat(expressionManager.isValid("#[mel:1]"), is(true));
    expressionManager.evaluate("#[mel:1]");
  }

  @Test
  public void session() throws MuleException {
    Object object = new Object();
    BindingContext context = builder().addBinding(MY_VAR, new TypedValue(object, OBJECT)).build();

    ExpressionManagerSession session = expressionManager.openSession(context);

    assertThat(session.evaluate("#[myVar]").getValue(), equalTo(object));
  }
}
