package com.epam.deltix.tbwg.webapp;

import com.epam.deltix.anvil.util.CloseHelper;
import com.epam.deltix.gflog.LogConfigurator;
import com.epam.deltix.tbwg.webapp.config.LogConfigurer;
import com.epam.deltix.tbwg.webapp.utils.ShutdownSignal;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;


@ServletComponentScan
@SpringBootApplication
@EnableWebSocket
@EnableScheduling
@EnableWebSocketMessageBroker
public class Application {

    private static String           version = Application.class.getPackage().getImplementationVersion();

    public static String            getVersion() {
        return version;
    }

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

        LogConfigurer.configureLogging("timebase-web-gateway");

        ConfigurableApplicationContext context = null;
        try {
            final ShutdownSignal shutdownSignal = new ShutdownSignal();
            final SpringApplication application = new SpringApplication(Application.class);
            application.setBannerMode(Banner.Mode.LOG);
            application.setBanner(new TimebaseWSBanner());
            application.setRegisterShutdownHook(false);
            context = application.run(args);
            shutdownSignal.await();
        } finally {
            CloseHelper.close(context);
            LogConfigurator.unconfigure();
        }
    }

}
