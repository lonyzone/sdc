package org.openecomp.sdc.be.servlets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.BeforeClass;
import org.openecomp.sdc.be.servlets.exception.ComponentExceptionMapper;
import org.openecomp.sdc.be.servlets.exception.DefaultExceptionMapper;
import org.openecomp.sdc.be.servlets.exception.StorageExceptionMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Feature;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
public abstract class JerseySpringBaseTest extends JerseyTest {

    private static final Logger log = Logger.getLogger(JerseySpringBaseTest.class.getName());
    protected static HttpServletRequest request;
    private final static JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static Feature loggingFeature = new LoggingFeature(log, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, null);

    @BeforeClass
    public static void initBaseClass() {
        request = mock(HttpServletRequest.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class)
              .register(loggingFeature)
              .register(jacksonJsonProvider);
    }

    protected ResourceConfig configure() {
        return configure(BaseTestConfig.class);
    }

    protected ResourceConfig configure(Class<?> springConfig) {
        ApplicationContext context = new AnnotationConfigApplicationContext(springConfig);
        forceSet(TestProperties.CONTAINER_PORT, "0");
        return new ResourceConfig()
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(request).to(HttpServletRequest.class);
                    }
                })
                .register(DefaultExceptionMapper.class)
                .register(ComponentExceptionMapper.class)
                .register(StorageExceptionMapper.class)
                .register(MultiPartFeature.class)
                .register(jacksonJsonProvider)
                .register(loggingFeature)
                .property("jersey.config.server.provider.classnames", "org.openecomp.sdc.be.view.MixinModelWriter")
                .property("contextConfig", context);
    }

}
