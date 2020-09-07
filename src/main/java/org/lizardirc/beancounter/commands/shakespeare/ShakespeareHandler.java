/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.shakespeare;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

public class ShakespeareHandler implements CommandHandler {
    private static final String COMMAND_INSULT = "insult";
    private static final String COMMAND_SHAKESPEARE = "shakespeare";

    public static final ImmutableSet<String> COMMANDS = ImmutableSet.of(COMMAND_INSULT, COMMAND_SHAKESPEARE);

    @Override
    public Set<String> getSubCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (!(event instanceof GenericChannelEvent)) {
            return Collections.emptySet();
        }

        GenericChannelEvent gce = (GenericChannelEvent) event;
        return gce.getChannel().getUsers().stream()
                .map(User::getNick)
                .collect(Collectors.toSet());
    }

    @Override
    public void handleCommand(GenericMessageEvent event, List<String> commands, String remainder) {
        if (!COMMANDS.contains(commands.get(0))) {
            return;
        }

        String target = event.getUser().getNick();

        if (commands.size() >= 2 && !commands.get(1).equalsIgnoreCase(event.getBot().getNick())) {
            target = commands.get(1);
        }

        String channel = event.getUser().getNick();
        boolean isChannelEvent = false;

        if (event instanceof GenericChannelEvent) {
            channel = ((GenericChannelEvent) event).getChannel().getName();
            isChannelEvent = true;
        }

        try {
            Document document = Jsoup.connect("http://www.pangloss.com/seidel/Shaker/index.html").get();
            Optional<Element> firstFontElement = document.getElementsByTag("font")
                    .stream()
                    .filter(x -> x.hasAttr("size") && x.attr("size").equals("+2"))
                    .findFirst();

            if (!firstFontElement.isPresent()) {
                event.respond("No insult found... has the page changed?");
                return;
            }

            Element fontElement = firstFontElement.get();
            String data = fontElement.text().trim();

            String format = "%1$s: %2$s";
            if (!isChannelEvent) {
                format = "%2$s";
            }

            event.getBot().sendIRC().message(channel, String.format(format, target, data));
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }
    }
}
