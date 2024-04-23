package lt.lb.jpaspringconfig;

import lt.lb.TolerantConfig;
import lt.lb.commons.io.ResourceFallbackLocator;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author laim0nas100
 */
public class PropertyLoader {

    static Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

    public static class PropertyConfig {

        public final String PROPERTY;
        public final String PROPERTIES_FILE;
        public final String DEV_PROPERTIES_FILE;
        private final ResourceFallbackLocator locator;

        public PropertyConfig(String name) {
            PROPERTY = name;
            PROPERTIES_FILE = PROPERTY + ".properties";
            DEV_PROPERTIES_FILE = PROPERTY + "-dev" + ".properties";
            locator = new ResourceFallbackLocator()
                    .withSystemPropertyPath(PROPERTY)
                    .withSystemPropertyURL(PROPERTY)
                    .withResource(PROPERTIES_FILE)
                    .withResource(DEV_PROPERTIES_FILE);
        }

        public TolerantConfig<ImmutableConfiguration> loadConfig() {
            final ResourceFallbackLocator.Loader loader = locator.getFirstSuccessfulLoader();
            if (loader == null) {
                throw new IllegalStateException("failed to load " + PROPERTY + " settings");
            }
            return TolerantConfig.ofSuplierCached(() -> {
                logger.info("loaded " + PROPERTY + " settings from " + loader.getDescription());

                PropertiesBuilderParameters prop = new Parameters()
                        .properties()
                        .setURL(loader.get());
                return new Configurations().propertiesBuilder(prop).getConfiguration();
            });

        }
    }
}
