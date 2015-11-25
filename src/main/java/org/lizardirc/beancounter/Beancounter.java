/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
 *
 * License GPLv3+: GNU General Public License version 3 or later (at your choice):
 * <http://gnu.org/licenses/gpl.html>. This is free software: you are free to
 * change and redistribute it at your will provided that your redistribution, with
 * or without modifications, is also licensed under the GNU GPL. (Although not
 * required by the license, we also ask that you attribute us!) There is NO
 * WARRANTY FOR THIS SOFTWARE to the extent permitted by law.
 *
 * Note that this is an official project of the LizardIRC IRC network.  For more
 * information about LizardIRC, please visit our website at
 * <https://www.lizardirc.org>.
 *
 * This is an open source project. The source Git repositories, which you are
 * welcome to contribute to, can be found here:
 * <https://gerrit.fastlizard4.org/r/gitweb?p=LizardIRC%2FBeancounter.git;a=summary>
 * <https://git.fastlizard4.org/gitblit/summary/?r=LizardIRC/Beancounter.git>
 *
 * Gerrit Code Review for the project:
 * <https://gerrit.fastlizard4.org/r/#/q/project:LizardIRC/Beancounter,n,z>
 *
 * Alternatively, the project source code can be found on the PUBLISH-ONLY mirror
 * on GitHub: <https://github.com/LizardNet/LizardIRC-Beancounter>
 *
 * Note: Pull requests and patches submitted to GitHub will be transferred by a
 * developer to Gerrit before they are acted upon.
 */

package org.lizardirc.beancounter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.cap.SASLCapHandler;
import org.pircbotx.exception.CAPException;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.managers.ThreadedListenerManager;

import org.lizardirc.beancounter.security.FingerprintingSslSocketFactory;
import org.lizardirc.beancounter.security.VerifyingSslSocketFactory;

public class Beancounter {
    public static final String PROJECT_NAME = "LizardIRC/Beancounter";
    public static final String PROJECT_URL = "https://www.lizardirc.org/index.php?page=beancounter";
    private final PircBotX bot;

    public Beancounter(Properties properties) {
        String botName = properties.getProperty("botName", "Beancounter");
        String botUsername = properties.getProperty("botUsername", "beancounter");
        String serverHost = properties.getProperty("serverHost");
        boolean useTls = Boolean.parseBoolean(properties.getProperty("useTls", "false"));
        boolean verifyHostname = Boolean.parseBoolean(properties.getProperty("verifyHostname", "true"));
        String allowedCertificates = properties.getProperty("allowedCertificates", "");
        int serverPort = Integer.parseInt(properties.getProperty("serverPort", useTls ? "6697" : "6667"));
        String[] autoJoinChannels = properties.getProperty("autoJoinChannels", "").split(",");
        String saslUsername = properties.getProperty("sasl.username", "");
        String saslPassword = properties.getProperty("sasl.password", "");
        boolean autoReconnect = Boolean.parseBoolean(properties.getProperty("autoReconnect", "true"));
        String serverPassword = properties.getProperty("serverPassword", "");

        ExecutorService executorService = constructExecutorService();
        ListenerManager<PircBotX> listenerManager = new ThreadedListenerManager<>(executorService);

        Configuration.Builder<PircBotX> confBuilder = new Configuration.Builder<>()
            .setAutoReconnect(autoReconnect)
            .setName(botName)
            .setLogin(botUsername)
            .setServerHostname(serverHost)
            .setServerPort(serverPort)
            .setListenerManager(listenerManager)
            .setCapEnabled(true) // Of course, the PircBotX documentation doesn't indicate this is necessary....
            .setAutoNickChange(true);

        Listeners<PircBotX> listeners = new Listeners<>(executorService, constructScheduledExecutorService(), confBuilder.getListenerManager(), properties);
        listeners.register();

        if (useTls) {
            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            if (verifyHostname) {
                socketFactory = new VerifyingSslSocketFactory(serverHost, socketFactory);
            }
            List<String> fingerprints = Arrays.stream(allowedCertificates.split(","))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (fingerprints.size() > 0) {
                socketFactory = new FingerprintingSslSocketFactory(fingerprints, socketFactory);
            }
            confBuilder.setSocketFactory(socketFactory);
        }

        for (String channel : autoJoinChannels) {
            confBuilder.addAutoJoinChannel(channel);
        }

        if (!saslUsername.isEmpty() && !saslPassword.isEmpty()) {
            confBuilder.addCapHandler(new SASLCapHandler(saslUsername, saslPassword));
        }

        if (!serverPassword.isEmpty()) {
            confBuilder.setServerPassword(serverPassword);
        }

        setVersionString(confBuilder);

        bot = new PircBotX(confBuilder.buildConfiguration());
    }

    public void run() throws IOException, IrcException, CAPException {
        bot.startBot();
    }

    public static void main(String[] args) {
        Path configurationFile = Paths.get("config.props");

        // Expect 0 or 1 arguments.  If present, argument is the location of the startup configuration file to use
        if (args.length > 1) {
            System.err.println("Error: Too many arguments.");
            System.err.println("Usage: java -jar beancounter.jar [configurationFile]");
            System.err.println("Where: configurationFile is the optional path to a startup configuration file.");
            System.exit(2);
        } else if (args.length == 1) {
            configurationFile = Paths.get(args[0]);
        }

        System.out.println("Reading configuration file " + configurationFile + "....");
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(configurationFile)) {
            properties.load(is);
        } catch (NoSuchFileException e) {
            System.err.println("Error: Could not find configuration file " + configurationFile + " (NoSuchFileException). A default configuration file has been created for you at that location.");
            System.err.println("The bot will now terminate to give you an opportunity to edit the configuration file.");
            try (InputStream defaultConfig = Beancounter.class.getResourceAsStream("/default.config.props")) {
                Files.copy(defaultConfig, configurationFile);
            } catch (IOException e1) {
                System.err.println("Error while writing out default configuration file.  Stack trace follows.");
                e1.printStackTrace();
            }
            System.exit(3);
        } catch (IOException e) {
            System.err.println("Error: Could not read configuration file " + configurationFile + ".  A stack trace follows.  The bot will now terminate.");
            e.printStackTrace();
            System.exit(3);
        }

        System.out.println("Creating bot....");
        Beancounter beancounter = new Beancounter(properties);

        System.out.println("Launching bot....");
        try {
            beancounter.run();
        } catch (Exception e) {
            System.err.println("Exception occurred launching bot: " + e.getMessage());
            System.err.println("Stack trace follows:");
            e.printStackTrace();
        }
    }

    private void setVersionString(Configuration.Builder confBuilder) {
        String artifactVersion = getClass().getPackage().getImplementationVersion();

        StringBuilder beancounterVersion = new StringBuilder(PROJECT_NAME);
        if (artifactVersion == null) {
            // We're probably running in an IDE instead of as a jarfile; don't try to get the artifact version
            beancounterVersion.append(" (development/unknown version)");
        } else {
            beancounterVersion.append(" version ").append(artifactVersion);
        }
        beancounterVersion.append(" <").append(PROJECT_URL).append("> using ");
        beancounterVersion.append(confBuilder.getVersion());

        StringBuilder beancounterGecos = new StringBuilder(PROJECT_NAME).append(" version ");
        if (artifactVersion == null) {
            beancounterGecos.append("(unknown)");
        } else {
            beancounterGecos.append(artifactVersion);
        }
        beancounterGecos.append(", ").append(PROJECT_URL);

        confBuilder.setVersion(beancounterVersion.toString());
        confBuilder.setRealName(beancounterGecos.toString());
    }

    private ExecutorService constructExecutorService() {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
            .namingPattern("primaryListenerPool-thread%d")
            .daemon(true)
            .build();
        ThreadPoolExecutor ret = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory);
        ret.allowCoreThreadTimeOut(true);
        return ret;
    }

    private ScheduledExecutorService constructScheduledExecutorService() {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
            .namingPattern("scheduledExecutorPool-thread%d")
            .daemon(true)
            .build();
        return Executors.newScheduledThreadPool(5, factory); // This seems like it should be enough for the reasonable future
    }
}
