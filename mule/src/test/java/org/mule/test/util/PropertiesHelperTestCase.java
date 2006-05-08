/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.util;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.SystemUtils;
import org.mule.util.CollectionUtil;
import org.mule.util.PropertiesHelper;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PropertiesHelperTestCase extends TestCase
{

    public void testRemoveNameSpacePrefix()
    {
        String temp = "this.is.a.namespace";
        String result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this.namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this_is-a-namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("this_is-a-namespace", result);
    }

    public void testRemoveXMLNameSpacePrefix()
    {
        String temp = "j:namespace";
        String result = PropertiesHelper.removeXmlNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this-namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("this-namespace", result);

        temp = "namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);
    }

    public void testRemoveNamespaces() throws Exception
    {
        Map props = new HashMap();

        props.put("blah.booleanProperty", "true");
        props.put("blah.blah.doubleProperty", "0.1243");
        props.put("blah.blah.Blah.intProperty", "14");
        props.put("longProperty", "999999999");
        props.put("3456.stringProperty", "string");

        props = PropertiesHelper.removeNamespaces(props);

        assertTrue(MapUtils.getBooleanValue(props, "booleanProperty", false));
        assertEquals(0.1243, 0, MapUtils.getDoubleValue(props, "doubleProperty", 0));
        assertEquals(14, MapUtils.getIntValue(props, "intProperty", 0));
        assertEquals(999999999, 0, MapUtils.getLongValue(props, "longProperty", 0));
        assertEquals("string", MapUtils.getString(props, "stringProperty", ""));
    }

    public void testMapNull() throws Exception
    {
        Map props = null;
        assertEquals("{}", PropertiesHelper.propertiesToString(props, false));
        assertEquals("{}", PropertiesHelper.propertiesToString(props, true));
    }

    public void testMapEmpty() throws Exception
    {
        Map props = new HashMap();
        assertEquals("{}", PropertiesHelper.propertiesToString(props, false));
        assertEquals("{}", PropertiesHelper.propertiesToString(props, true));
    }

    public void testMapSingleElement() throws Exception
    {
        Map props = CollectionUtil.mapWithKeysAndValues(HashMap.class, new Object[]{"foo"},
                new Object[]{"bar"});

        assertEquals("{foo=bar}", PropertiesHelper.propertiesToString(props, false));
        assertEquals("{" + SystemUtils.LINE_SEPARATOR + "foo=bar" + SystemUtils.LINE_SEPARATOR + "}",
                PropertiesHelper.propertiesToString(props, true));
    }

    public void testMapTwoElements() throws Exception
    {
        Map props = CollectionUtil.mapWithKeysAndValues(HashMap.class, new Object[]{"foo","foozle"},
                new Object[]{"bar","doozle"});

        assertEquals("{foo=bar, foozle=doozle}", PropertiesHelper.propertiesToString(props, false));

        assertEquals("{" + SystemUtils.LINE_SEPARATOR + "foo=bar" + SystemUtils.LINE_SEPARATOR
                + "foozle=doozle" + SystemUtils.LINE_SEPARATOR + "}", PropertiesHelper
                .propertiesToString(props, true));
    }

}
