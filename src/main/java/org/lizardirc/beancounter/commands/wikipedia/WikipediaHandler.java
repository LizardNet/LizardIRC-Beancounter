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
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.utils.URIBuilder;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.IrcColors;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.lizardirc.beancounter.utils.MoreStrings;

public class WikipediaHandler<T extends PircBotX> extends ListenerAdapter<T> implements CommandHandler<T> {
    private static final String CMD_WIKIPEDIA = "WikiPedia";
    private static final String CMD_CFGWIKILINKS = "cfgwikilinks";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_WIKIPEDIA, CMD_CFGWIKILINKS);
    private static final String CFG_DISABLE = "disable";
    private static final String CFG_ENABLE = "enable";
    private static final String CFG_API = "api";

    private static final Set<String> CFG_OPTIONS = ImmutableSet.of(CFG_DISABLE, CFG_ENABLE, CFG_API);

    private static final Pattern PATTERN_WIKILINK = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");

    private final WikipediaSummaryService wikipediaSummaryService = new WikipediaSummaryService();

    private final PersistenceManager pm;
    private final Set<String> disabledWikilinkExpansionChannels;
    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Set<String>>(){}.getType();
    private static final String PERSISTENCE_KEY = "disabledWikilinkExpansionChannels";

    private final AccessControl<T> acl;
    private static final String PERMISSION_CFGWIKILINKS = "cfgwikilinks";

    public WikipediaHandler(PersistenceManager pm, AccessControl<T> acl) {
        this.pm = pm;
        this.acl = acl;

        Gson gson = new Gson();
        Optional<String> persistedState = pm.get(PERSISTENCE_KEY);
        if (persistedState.isPresent()) {
            Set<String> disabledWikilinkExpansionChannels = gson.fromJson(persistedState.get(), PERSISTENCE_TYPE_TOKEN);
            // Normalize to lowercase
            this.disabledWikilinkExpansionChannels = new HashSet<>(disabledWikilinkExpansionChannels.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
            );
        } else {
            disabledWikilinkExpansionChannels = new HashSet<>();
        }
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && CMD_CFGWIKILINKS.equals(commands.get(0))) {
            return CFG_OPTIONS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.size() == 1 && CMD_WIKIPEDIA.equals(commands.get(0))) {
            String channel = null;
            if (event instanceof GenericChannelEvent) {
                GenericChannelEvent gce = (GenericChannelEvent) event;
                channel = gce.getChannel().getName().toLowerCase();
            }

            // get the local MW URI, or the default if none set/not in channel/
            URI localMediaWikiAPi = getLocalMediaWikiAPi(channel);

            event.respond(summarizeWikiPage(remainder, localMediaWikiAPi));
        } else if (commands.size() >= 1 && CMD_CFGWIKILINKS.equals(commands.get(0))) {
            if (event instanceof GenericChannelEvent) {
                GenericChannelEvent gce = (GenericChannelEvent) event;

                if (commands.size() == 1) {
                    boolean disabled = disabledWikilinkExpansionChannels.contains(gce.getChannel().getName().toLowerCase());

                    event.respond("Wikilink expansion is " + (disabled ? "disabled" : "enabled") + " in this channel.");

                    if (acl.hasPermission(event, PERMISSION_CFGWIKILINKS) || gce.getChannel().isOp(event.getUser())) {
                        event.respond("You can enable or disable wikilink expansion by using the command \"" +
                            CMD_CFGWIKILINKS + " <" + Miscellaneous.getStringRepresentation(CFG_OPTIONS, "|") + ">\"");
                    }
                } else {
                    if (acl.hasPermission(event, PERMISSION_CFGWIKILINKS) || gce.getChannel().isOp(event.getUser())) {
                        try {
                            switch (commands.get(1)) {
                                case CFG_DISABLE:
                                    disabledWikilinkExpansionChannels.add(gce.getChannel().getName().toLowerCase());
                                    break;
                                case CFG_ENABLE:
                                    disabledWikilinkExpansionChannels.remove(gce.getChannel().getName().toLowerCase());
                                    break;
                                case CFG_API:
                                    setLocalMediaWikiApi(gce.getChannel().getName().toLowerCase(), remainder.trim());
                                    break;
                            }

                            sync();
                            event.respond("Done");
                        }
                        catch (MediaWikiApiError e) {
                            // oops
                            event.respond(e.getMessage());
                        }
                    } else {
                        event.respond("You don't have the necessary permissions to do this.  If you are a channel" +
                            "operator, please op up first.");
                    }
                }
            } else {
                event.respond("This command may only be run in a channel.");
            }
        }
    }

    private void setLocalMediaWikiApi(String channel, String apiPath) throws MediaWikiApiError {
        // Let's be smart
        if (apiPath.equals("default")) {
            // TODO: short circuit and remove this config, forcing back to enwiki.

        }

        // OK, assuming we have a valid URL.
        URIBuilder b;

        try {
            // Let's test that assumption.
            b = new URIBuilder(apiPath);
        } catch (URISyntaxException e) {
            throw new MediaWikiApiError("Could not find API from URI provided (" + apiPath + ").");
        }

        // OK. It's a URL. Is it an API url?
        if (b.getPath().endsWith("api.php")) {
            // TODO: set config. SUCCESS!
            return;
        }

        // Okay... not an API path. This is either because a) user is an idiot or b) user is an idiot.
        // Taking case a) - user is an idiot and didn't specify the API path when requested. Let's do
        // a fetch and use RSD to try and find the real API url (as it might not be where we expect.
        URI rsdPath;
        try {
            rsdPath = wikipediaSummaryService.handleReallySimpleDiscovery(apiPath);
        } catch (URISyntaxException | IOException e) {
            // :'(
            throw new MediaWikiApiError(e.getMessage(), e);
        }

        if (rsdPath == null) {
            // case b) user is an idiot and tried this with a non-mediawiki wiki (probably)
            throw new MediaWikiApiError("Couldn't find API from URL - is this a MediaWiki wiki?");
        }

        // TODO: set config. SUCCESS!
    }

    private URI getLocalMediaWikiAPi(String channel) {
        if (channel != null) {
            // TODO: get the needful
        }

        try {
            // Yay defaults
            return new URI("https://en.wikipedia.org/w/api.php");
        } catch (URISyntaxException e) {
            return null;
            // <rant>
            // wat... java why do I have to handle this hard-coded case and do silly catches for something
            // which cannot fail at runtime? You could compile-time check this and ignore the throws clause, or you
            // could just let me specify a sane default without going batshit insane and forcing me to handle an
            // exception which quite literally cannot be thrown without a recompile or a java bug.
            // </rant>
            // TODO: remove rant after it's been... appreciated... by the needful.
        }
    }

    @Override
    public void onGenericMessage(GenericMessageEvent<T> event) {
        String channelName = null;

        if (event instanceof GenericChannelEvent) {
            Channel channel = ((GenericChannelEvent) event).getChannel();
            if (channel != null) {
                channelName = channel.getName().toLowerCase();

                if (disabledWikilinkExpansionChannels.contains(channelName)) {
                    return;
                }
            }
        }

        String message = IrcColors.stripFormatting(event.getMessage());
        Matcher m = PATTERN_WIKILINK.matcher(message);

        URI api = getLocalMediaWikiAPi(channelName);

        while (m.find()) {
            event.respond(summarizeWikiPage(m.group(1), api));
        }
    }

    private String summarizeWikiPage(String pageName, URI api) {
        try {
            if (MoreStrings.isNullOrWhitespace(pageName)) {
                // apply ointment to burned area...
                return "Wikipedia - well, it's kinda big. Please tell me what you want to look up.";
            }

            WikipediaPage page = wikipediaSummaryService.getSummary(pageName, api);

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

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set(PERSISTENCE_KEY, gson.toJson(disabledWikilinkExpansionChannels, PERSISTENCE_TYPE_TOKEN));
        pm.sync();
    }
}
