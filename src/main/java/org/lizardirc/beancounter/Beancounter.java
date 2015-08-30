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
import java.util.Properties;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;

public class Beancounter {
    private final PircBotX bot;

    public Beancounter(Properties properties) {
        String botName = properties.getProperty("botName", "Beancounter");
        String serverHost = properties.getProperty("serverHost");
        boolean useTls = Boolean.parseBoolean(properties.getProperty("useTls", "false"));
        int serverPort = Integer.parseInt(properties.getProperty("serverPort", useTls ? "6697" : "6667"));
        String[] autoJoinChannels = properties.getProperty("autoJoinChannels", "").split(",");

        Configuration.Builder<PircBotX> confBuilder = new Configuration.Builder<>()
            .setName(botName)
            .setServerHostname(serverHost)
            .setServerPort(serverPort);

        Listeners<PircBotX> listeners = new Listeners<>(confBuilder.getListenerManager(), properties);
        listeners.register();

        if (useTls) {
            // TODO add support for certificate pinning
            confBuilder.setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates());
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
