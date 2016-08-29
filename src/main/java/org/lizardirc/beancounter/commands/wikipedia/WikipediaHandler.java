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

package org.lizardirc.beancounter.commands.wikipedia;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.utils.MoreStrings;

public class WikipediaHandler<T extends PircBotX> extends ListenerAdapter<T> implements CommandHandler<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("WikiPedia");
    private static final Pattern PATTERN_WIKILINK = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");

    private final WikipediaSummaryService wikipediaSummaryService = new WikipediaSummaryService();

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.size() == 1 && COMMANDS.contains(commands.get(0))) {
            event.respond(summarizeWikiPage(remainder));
        }
    }

    @Override
    public void onGenericMessage(GenericMessageEvent<T> event) {
        String message = event.getMessage();
        Matcher m = PATTERN_WIKILINK.matcher(message);

        while (m.find()) {
            event.respond(summarizeWikiPage(m.group(1)));
        }
    }

    private String summarizeWikiPage(String pageName) {
        try {
            if (MoreStrings.isNullOrWhitespace(pageName)) {
                // apply ointment to burned area...
                return "Wikipedia - well, it's kinda big. Please tell me what you want to look up.";
            }

            WikipediaPage page = wikipediaSummaryService.getSummary("en", pageName);

            if (page == null) {
                return String.format("Cannot find page with title: %1$s", pageName);
            }

            StringBuilder stringBuilder = new StringBuilder();

            if (page.getSiteName() != null) {
                stringBuilder.append(page.getSiteName())
                    .append(" | ");
            }

            stringBuilder.append(page.getDisplayTitle())
                .append(" | ");

            if (page.getSummary() != null) {
                stringBuilder.append(page.getSummary())
                    .append(" | ");
            }

            stringBuilder.append(page.getCanonicalUrl());

            return stringBuilder.toString();
        } catch (URISyntaxException | IOException e) {
            return e.getMessage();
        }
    }
}
