/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
