/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.elite;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.commands.elite.eddn.EddnHandler;
import org.lizardirc.beancounter.commands.elite.edsm.ApiHandler;
import org.lizardirc.beancounter.commands.elite.edsm.responses.ApiResponse;
import org.lizardirc.beancounter.commands.elite.edsm.responses.CmdrLastPosition;
import org.lizardirc.beancounter.commands.elite.edsm.responses.CmdrRanks;
import org.lizardirc.beancounter.commands.elite.edsm.responses.RanksNumeric;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.IrcColors;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class DangerousCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_CMDR = "cmdr";
    private static final String CMD_CFGEDDN = "cfgeddn";
    private static final String CFGEDDN_ENABLE = "enable"; // Enables EDDN tracking output in the current channel
    private static final String CFGEDDN_DISABLE = "disable"; // Disables EDDN tracking output in the current channel, without deleting the list of tracked CMDRs for that channel
    private static final String CFGEDDN_TRACK = "track"; // Adds the given Commander to the current channel's tracking
    private static final String CFGEDDN_REMOVE = "remove"; // Removes the given Commander from the current channel's tracking
    private static final String CFGEDDN_GET = "get"; // Lists all commanders tracked in the current channel
    private static final String CFGEDDN_GETALL = "getall"; // Privileged command - list all channels with EDDN tracking enabled
    private static final String CFGEDDN_HUTTON = "hutton"; // Track all docking events at Hutton Orbital; aka the poor sod filter
    private static final Set<String> CMDS_CFGEDDN = ImmutableSet.of(CFGEDDN_ENABLE, CFGEDDN_DISABLE, CFGEDDN_TRACK, CFGEDDN_REMOVE,
        CFGEDDN_GET, CFGEDDN_GETALL, CFGEDDN_HUTTON);
    private static final String PERM_CFGEDDN = "cfgeddn";
    private static final String PERSISTENCE_KEY = "eliteCommands";

    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final EddnHandler eddnHandler; // WARNING - this will be null to indicate that EDDN access is disabled!
    private final Configuration config;

    public DangerousCommandHandler(PersistenceManager pm, AccessControl<T> acl, EddnHandler eddnHandler) {
        this.pm = pm;
        this.acl = acl;
        this.eddnHandler = eddnHandler;

        if (pm.get(PERSISTENCE_KEY).isPresent()) {
            Gson gson = new Gson();
            config = gson.fromJson(pm.get(PERSISTENCE_KEY).get(), Configuration.class);
        } else {
            config = Configuration.init();
        }
        if (eddnHandler != null) {
            eddnHandler.setConfig(config);
        }
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            Set<String> retval = Sets.newHashSet(CMD_CMDR);
            if (eddnHandler != null) {
                retval.add(CMD_CFGEDDN);
            }

            return ImmutableSet.copyOf(retval);
        } else if (commands.size() == 1 && eddnHandler != null && commands.get(0).equals(CMD_CFGEDDN)) {
            return CMDS_CFGEDDN;
        } else if (commands.size() == 2 && eddnHandler != null && commands.get(0).equals(CMD_CFGEDDN) && commands.get(1).equals(CFGEDDN_HUTTON)) {
            return ImmutableSet.of(CFGEDDN_ENABLE, CFGEDDN_DISABLE);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public synchronized void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() >= 1) {
            remainder = remainder.trim();

            switch (commands.get(0)) {
                case CMD_CMDR:
                    if (!remainder.isEmpty()) {
                        try {
                            CmdrLastPosition lastPos = ApiHandler.getLastPositionOf(remainder);
                            CmdrRanks ranks = ApiHandler.getRanksOf(remainder);

                            String lastPosInfo;
                            String ranksInfo;
                            String profileUrl = null;
                            boolean error = false;

                            if (lastPos.getMsgnum() == ApiResponse.API_RESPONSE_OK) {
                                if (lastPos.getSystem() != null) {
                                    StringBuilder sb = new StringBuilder("Last reported position: ")
                                        .append(lastPos.getSystem());

                                    if (lastPos.isFirstDiscover()) {
                                        sb.append(" (")
                                            .append(IrcColors.YELLOW)
                                            .append("first discovery!")
                                            .append(IrcColors.RESET)
                                            .append(")");
                                    }

                                    if (lastPos.getCoordinates() != null) {
                                        sb.append(" at coordinates [")
                                            .append(lastPos.getCoordinates().getX())
                                            .append(", ")
                                            .append(lastPos.getCoordinates().getY())
                                            .append(", ")
                                            .append(lastPos.getCoordinates().getZ())
                                            .append(']');
                                    }

                                    lastPosInfo = sb.toString();
                                } else {
                                    lastPosInfo = "Last position not yet recorded by EDSM, or not made public by this user";
                                }
                                profileUrl = lastPos.getUrl();
                            } else {
                                lastPosInfo = "Could not look up last position info: " + lastPos.getMsg();
                                error = true;
                            }

                            if (ranks.getMsgnum() == ApiResponse.API_RESPONSE_OK) {
                                if (ranks.getRanks() != null) {
                                    ranksInfo = "Ranks: " +
                                        IrcColors.YELLOW +
                                        "Combat: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getCombat() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getCombat() +
                                        "/" +
                                        RanksNumeric.ELITE +
                                        ' ' +
                                        ranks.getProgress().getCombat() +
                                        "%)" +
                                        IrcColors.RESET +
                                        ' ' +
                                        IrcColors.PURPLE +
                                        "Trade: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getTrade() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getTrade() +
                                        "/" +
                                        RanksNumeric.ELITE +
                                        ' ' +
                                        ranks.getProgress().getTrade() +
                                        "%)" +
                                        IrcColors.RESET +
                                        ' ' +
                                        IrcColors.CYAN +
                                        "Exploration: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getExploration() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getExploration() +
                                        "/" +
                                        RanksNumeric.ELITE +
                                        ' ' +
                                        ranks.getProgress().getExploration() +
                                        "%)" +
                                        IrcColors.RESET +
                                        ' ' +
                                        IrcColors.RED +
                                        "CQC: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getCqc() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getCqc() +
                                        "/" +
                                        RanksNumeric.ELITE +
                                        ' ' +
                                        ranks.getProgress().getCqc() +
                                        "%)" +
                                        IrcColors.RESET +
                                        " Federal Navy: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getFederalNavy() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getFederalNavy() +
                                        "/" +
                                        RanksNumeric.MAX_NAVAL_RANK +
                                        ' ' +
                                        ranks.getProgress().getFederalNavy() +
                                        "%)" +
                                        IrcColors.RESET +
                                        " Imperial Navy: " +
                                        IrcColors.BOLD +
                                        ranks.getRanksVerbose().getImperialNavy() +
                                        IrcColors.BOLD +
                                        " (" +
                                        ranks.getRanks().getImperialNavy() +
                                        "/" +
                                        RanksNumeric.MAX_NAVAL_RANK +
                                        ' ' +
                                        ranks.getProgress().getImperialNavy() +
                                        "%)" +
                                        IrcColors.RESET;
                                } else {
                                    ranksInfo = "Ranks info not yet recorded, or not made public by this user";
                                }
                            } else {
                                ranksInfo = "Could not look up ranks info: " + ranks.getMsg();
                                error = true;
                            }

                            String output = "CMDR " + remainder + ": " + lastPosInfo + "; " + ranksInfo;

                            if (profileUrl != null) {
                                output += "; " + profileUrl;
                            }

                            if (error) {
                                output += " - To resolve errors, please verify that this Commander has a public EDSM profile.";
                            }

                            event.respond(output);
                        } catch (Exception e) {
                            event.respond("Error: Caught an exception while looking up information from EDSM: " + e.toString());
                        }
                    } else {
                        event.respond("Who do you want to look up?  Note that the player you specify must have a public EDSM profile.  Do not include the CMDR prefix.  Example: " + CMD_CMDR + " Commander McCommanderface");
                    }
                    break;
                case CMD_CFGEDDN:
                    if (commands.size() >= 2) {
                        switch (commands.get(1)) {
                            case CFGEDDN_ENABLE:
                                if (checkIfInChannel(event) && checkIfAuthorized(event)) {
                                    config.enableOutputIn(((GenericChannelEvent) event).getChannel().getName());
                                    sync();
                                    event.respond("Done.");
                                }
                                break;
                            case CFGEDDN_DISABLE:
                                if (checkIfInChannel(event) && checkIfAuthorized(event)) {
                                    config.disableOutputIn(((GenericChannelEvent) event).getChannel().getName());
                                    sync();
                                    event.respond("Done.");
                                }
                                break;
                            case CFGEDDN_TRACK:
                                trackOrRemoveCommander(event, remainder, true);
                                break;
                            case CFGEDDN_REMOVE:
                                trackOrRemoveCommander(event, remainder, false);
                                break;
                            case CFGEDDN_GET:
                                if (checkIfInChannel(event)) {
                                    String channel = ((GenericChannelEvent) event).getChannel().getName();
                                    Set<String> trackedCmdrs = config.getTrackedCommandersMap().get(channel);

                                    event.respond("CMDRs tracked in this channel: " +
                                            Miscellaneous.getStringRepresentation(trackedCmdrs == null ? Collections.emptySet() : trackedCmdrs) +
                                            "; Hutton tracking is " + (config.isChannelTrackingHutton(channel) ? "enabled" : "disabled") + "; EDDN output is " +
                                            (config.isChannelEnabled(channel) ? "enabled" : "disabled"));
                                    // Wow, that's a mouthful
                                }
                                break;
                            case CFGEDDN_GETALL:
                                if (checkIfAuthorized(true, event)) {
                                    event.respond("List of all channels tracking EDDN events:");
                                    config.getTrackedCommandersMap().forEach((key, value) -> {
                                        if (value != null && (!value.isEmpty() || config.isChannelTrackingHutton(key))) {
                                            event.respond("Channel " +
                                                key + " tracking CMDRs: " + Miscellaneous.getStringRepresentation(value) +
                                                "; Hutton tracking is " + (config.isChannelTrackingHutton(key) ? "enabled" : "disabled") +
                                                "; output is " + (config.isChannelEnabled(key) ? "enabled" : "disabled"));
                                        }
                                    });
                                }
                                break;
                            case CFGEDDN_HUTTON:
                                if (commands.size() == 3) {
                                    switch (commands.get(2)) {
                                        case CFGEDDN_ENABLE:
                                            if (checkIfInChannel(event) && checkIfAuthorized(event)) {
                                                config.enableTrackingHutton(((GenericChannelEvent) event).getChannel().getName());
                                                sync();
                                                event.respond("Done");
                                            }
                                            break;
                                        case CFGEDDN_DISABLE:
                                            if (checkIfInChannel(event) && checkIfAuthorized(event)) {
                                                config.disableTrackingHutton(((GenericChannelEvent) event).getChannel().getName());
                                                sync();
                                                event.respond("Done");
                                            }
                                            break;
                                    }
                                } else {
                                    event.respond("Too few arguments; syntax: " + CMD_CFGEDDN + ' ' + CFGEDDN_HUTTON + " <" + CFGEDDN_ENABLE + '|' + CFGEDDN_DISABLE + '>');
                                }
                                break;
                        }
                    } else {
                        event.respond("Too few arguments.  Syntax: " + CMD_CFGEDDN + " <" +
                            Miscellaneous.getStringRepresentation(CMDS_CFGEDDN, "|") + '>');
                    }
            }
        }
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set(PERSISTENCE_KEY, gson.toJson(config, Configuration.class));
        pm.sync();
    }

    private boolean checkIfInChannel(GenericMessageEvent<?> event) {
        if (event instanceof GenericChannelEvent) {
            return true;
        } else {
            event.respond("This command must be run in the channel you wish to affect.");
            return false;
        }
    }

    private boolean checkIfAuthorized(boolean onlyWithGlobalPermission, GenericMessageEvent<?> event) {
        // First check if the command was given in channel, and if so, if the user is opped
        if (!onlyWithGlobalPermission && event instanceof GenericChannelEvent) {
            if (((GenericChannelEvent) event).getChannel().getOps().contains(event.getUser())) {
                return true;
            }
        }

        // Next check if the user has the global permission, but only if the allowUsersWithGlobalPermission parameter is
        // true
        if (acl.hasPermission(event, PERM_CFGEDDN)) {
            return true;
        }

        StringBuilder sb = new StringBuilder("Sorry, you do not have permission to run this command.  You must have the global \"")
            .append(PERM_CFGEDDN)
            .append("\" bot permission");

        if (!onlyWithGlobalPermission) {
            sb.append(", or run this command in a channel you have operator status in.");
        } else {
            sb.append('.');
        }

        event.respond(sb.toString());
        return false;
    }

    private boolean checkIfAuthorized(GenericMessageEvent<?> event) {
        return checkIfAuthorized(false, event);
    }

    private void trackOrRemoveCommander(GenericMessageEvent<?> event, String commander, boolean addToTracking) {
        if (!commander.isEmpty()) {
            if (checkIfInChannel(event) && checkIfAuthorized(event)) {
                if (addToTracking) {
                    config.trackCommander(((GenericChannelEvent) event).getChannel().getName(), commander);
                } else {
                    config.untrackCommander(((GenericChannelEvent) event).getChannel().getName(), commander);
                }
                sync();
                String response = "Done";
                if (!config.isChannelEnabled(((GenericChannelEvent) event).getChannel().getName())) {
                    response += " - Warning: EDDN output is disabled in this channel.  Enable with command: " + CMD_CFGEDDN + ' ' + CFGEDDN_ENABLE;
                }
                event.respond(response);
            }
        } else {
            event.respond("Please provide the name of the Commander (without the \"CMDR\" prefix) you wish to "+
                (addToTracking ? "track" : "remove from tracking") + ".  Syntax: " + CMD_CFGEDDN + ' ' +
                (addToTracking ? CFGEDDN_TRACK : CFGEDDN_REMOVE) + " Commander McCommanderface");
        }
    }
}
