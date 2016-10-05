package org.restcomm.connect.commons.configuration;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.connect.commons.configuration.sets.CustomConfigurationSet;
import org.restcomm.connect.commons.configuration.sources.EditableConfigurationSource;

public class ConfigurationSetTest {

    public ConfigurationSetTest() {
        // TODO Auto-generated constructor stub
    }

    @Test
    public void testValueRetrievalAndSetLifecycle() {
        //
        EditableConfigurationSource source = new EditableConfigurationSource();
        source.setProperty(CustomConfigurationSet.PROPERTY1_KEY, "value1");
        // test value retrieval
        CustomConfigurationSet customSet = new CustomConfigurationSet(source);
        assertTrue( customSet.getProperty1().equals("value1") );
        // make sure values are properly cached by the configuration set and are not changed when source changes
        source.setProperty(CustomConfigurationSet.PROPERTY1_KEY, "changed_value1");
        assertTrue( customSet.getProperty1().equals("value1") );
        // test conf set lifecycle
        RestcommConfiguration conf = new RestcommConfiguration();
        conf.addConfigurationSet("custom", customSet);
        CustomConfigurationSet customSet2 = conf.get("custom",CustomConfigurationSet.class);
        assertNotNull(customSet2);
        assertTrue( customSet.getProperty1().equals("value1") );
    }

}
