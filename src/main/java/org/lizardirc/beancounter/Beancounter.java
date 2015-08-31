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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.net.ssl.SSLSocketFactory;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import org.lizardirc.beancounter.security.FingerprintingSslSocketFactory;
import org.lizardirc.beancounter.security.VerifyingSslSocketFactory;

public class Beancounter {
    private final PircBotX bot;

    public Beancounter(Properties properties) {
        String botName = properties.getProperty("botName", "Beancounter");
        String serverHost = properties.getProperty("serverHost");
        boolean useTls = Boolean.parseBoolean(properties.getProperty("useTls", "false"));
        boolean verifyHostname = Boolean.parseBoolean(properties.getProperty("verifyHostname", "true"));
        String allowedCertificates = properties.getProperty("allowedCertificates", "");
        int serverPort = Integer.parseInt(properties.getProperty("serverPort", useTls ? "6697" : "6667"));
        String[] autoJoinChannels = properties.getProperty("autoJoinChannels", "").split(",");

        Configuration.Builder<PircBotX> confBuilder = new Configuration.Builder<>()
            .setName(botName)
            .setServerHostname(serverHost)
            .setServerPort(serverPort);

        Listeners<PircBotX> listeners = new Listeners<>(confBuilder.getListenerManager(), properties);
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

        bot = new PircBotX(confBuilder.buildConfiguration());
    }

    public void run() throws IOException, IrcException {
        bot.startBot();
    }

    public static void main(String[] args) {
        System.out.println("Configuring bot....");
        Properties properties = new Properties();

        //Eventually, we will have a parsable configuration file from which we will get
        //things like what server to connect to, etc.
        //For now, though, it's sufficient to just default the bot to these settings for
        //testing.
        properties.setProperty("serverHost", "irc.lizardirc.org");
        properties.setProperty("useTls", "true");
        properties.setProperty("allowedCertificates", "7E:00:C0:1A:C0:11:46:F3:99:47:EE:9C:7C:E9:CB:0F:86:26:B4:14:69:7D:D2:4F:A7:2F:F2:85:23:D1:12:B0:36:C1:2F:9C:65:41:04:25:06:B6:41:49:78:E2:D6:98:C5:F5:9F:73:CD:9F:A4:0C:4E:A5:E2:54:69:DA:51:6E");
        properties.setProperty("autoJoinChannels", "#botspam");

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
}
