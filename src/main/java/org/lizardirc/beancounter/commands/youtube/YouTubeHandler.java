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

package org.lizardirc.beancounter.commands.youtube;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.security.AccessControl;

public class YouTubeHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String PERM_CFGYOUTUBE = "configureYouTube";

    private static final String COMMAND_YOUTUBE = "YouTube";
    private static final String COMMAND_CONFIGURE = "cfgyoutube";

    private static final String SUBCOMMAND_CONFIGURE_APIKEY = "apikey";
    private static final String SUBCOMMAND_CONFIGURE_ENABLE = "enable";
    private static final String SUBCOMMAND_CONFIGURE_DISABLE = "disable";

    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_YOUTUBE, COMMAND_CONFIGURE);
    private static final Set<String> SUBCOMMANDS_CONFIGURE = ImmutableSet.of(SUBCOMMAND_CONFIGURE_APIKEY,
            SUBCOMMAND_CONFIGURE_ENABLE,
            SUBCOMMAND_CONFIGURE_DISABLE);

    private static final List<String> VIDEO_IDS = ImmutableList.of("9bZkp7q19f0", "jofNR_WkoCE", "kfVsfOSbJY0", "dQw4w9WgXcQ", "0GIwTG8V-Ko", "gXB84fpWzg8", "z9Uz1icjwrM");

    private final AccessControl<T> acl;
    private final YouTubeService youTubeService;

    private final Random random = new Random();

    public YouTubeHandler(AccessControl<T> acl, YouTubeService youTubeService) {
        this.acl = acl;
        this.youTubeService = youTubeService;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(COMMAND_CONFIGURE)) {
            return SUBCOMMANDS_CONFIGURE;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        switch (commands.get(0)) {
            case COMMAND_CONFIGURE:
                if (commands.size() == 1) {
                    String enabledDescriptor = youTubeService.isEnabled() ? "ENABLED" : "DISABLED";
                    event.respond(String.format("YouTube commands are currently %1$s", enabledDescriptor));
                    break;
                }

                if (!acl.hasPermission(event, PERM_CFGYOUTUBE)) {
                    event.respond("You don't have the permission required to do this.");
                    return;
                }

                switch (commands.get(1)) {
                    case SUBCOMMAND_CONFIGURE_APIKEY:
                        youTubeService.setApiKey(remainder);
                        event.respond("Done.");
                        break;
                    case SUBCOMMAND_CONFIGURE_ENABLE:
                        try {
                            this.youTubeService.setEnabled(true);
                            event.respond("Enabled YouTube services");
                        } catch (IllegalStateException e) {
                            event.respond(e.toString());
                        }
                        break;
                    case SUBCOMMAND_CONFIGURE_DISABLE:
                        this.youTubeService.setEnabled(false);
                        event.respond("Disabled YouTube services");
                        break;
                }

                break;
            case COMMAND_YOUTUBE:
                if (!youTubeService.isEnabled()) {
                    event.respond("Sorry, this command is currently disabled.");
                    break;
                }

                String channel = event.getUser().getNick();
                if (event instanceof GenericChannelEvent) {
                    channel = ((GenericChannelEvent) event).getChannel().getName();
                }

                String search = remainder.trim();

                if (search.isEmpty()) {
                    String video = VIDEO_IDS.get(random.nextInt(VIDEO_IDS.size()));
                    event.respond("Did you mean to search for something? Perhaps you wanted https://youtu.be/" + video + " ?");
                    break;
                }

                try {
                    if (search.length() == 11 && search.matches("^[a-zA-Z0-9-_]{11}$")) {
                        // Probably an ID?
                        YouTubeVideo videoInformation = this.youTubeService.getVideoInformation(remainder);

                        if (videoInformation != null) {
                            event.getBot().sendIRC().message(channel, videoInformation.toString() + " | https://youtu.be/" + videoInformation.getId());
                            break;
                        }
                    }

                    // not an ID, or not found. Let's do a search instead. This is expensive.
                    YouTubeVideo videoInformation = this.youTubeService.search(remainder);
                    if (videoInformation != null) {
                        event.getBot().sendIRC().message(channel, videoInformation.toString() + " | https://youtu.be/" + videoInformation.getId());
                        break;
                    }

                } catch (IOException | URISyntaxException ex) {
                    event.respond("Something went wrong: " + ex.getMessage());
                    break;
                }
                break;
        }
    }
}
