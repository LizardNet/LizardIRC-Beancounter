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

package org.lizardirc.beancounter.commands.elite.eddn.schema.journal;

import com.google.gson.JsonElement;

import org.lizardirc.beancounter.commands.elite.eddn.Parser;
import org.lizardirc.beancounter.commands.elite.eddn.SchemaMismatchException;
import org.lizardirc.beancounter.commands.elite.eddn.schema.Header;
import org.lizardirc.beancounter.commands.elite.eddn.schema.SchemaRef;
import org.lizardirc.beancounter.utils.IrcColors;

public class JournalEntry extends SchemaRef {
    public static final String SCHEMA_ID = "http://schemas.elite-markets.net/eddn/journal/1";

    // These are part of the EDDN schema
    protected Header header;
    protected JsonElement message;

    // These are for our own use
    private Message messageAsMessage = null;
    private FsdJumpEvent messageAsFsdJumpEvent = null;
    private DockedEvent messageAsDockedEvent = null;

    public static JournalEntry of(Parser parser, String jsonString) throws SchemaMismatchException {
        return parser.parseJson(jsonString, JournalEntry.class);
    }

    @Override
    public String getExpectedSchemaId() {
        return SCHEMA_ID;
    }

    public Header getHeader() {
        return header;
    }

    public Message getMessage(Parser parser) {
        if (messageAsMessage == null) {
            messageAsMessage = parser.getGson().fromJson(message, Message.class);
        }

        return messageAsMessage;
    }

    public FsdJumpEvent getMessageAsFsdJumpEvent(Parser parser) {
        EventType eventType = getMessage(parser).event;

        if (!EventType.FSD_JUMP.equals(eventType)) {
            throw new IllegalStateException("A Message of EventType " + eventType.toString() + " cannot be converted to FsdJumpEvent");
        }

        if (messageAsFsdJumpEvent == null) {
            messageAsFsdJumpEvent = parser.getGson().fromJson(message, FsdJumpEvent.class);
        }

        return messageAsFsdJumpEvent;
    }

    public DockedEvent getMessageAsDockedEvent(Parser parser) {
        EventType eventType = getMessage(parser).event;

        if (!EventType.DOCKED.equals(eventType)) {
            throw new IllegalStateException("A Message of EventType " + eventType.toString() + " cannot be converted to DockedEvent");
        }

        if (messageAsDockedEvent == null) {
            messageAsDockedEvent = parser.getGson().fromJson(message, DockedEvent.class);
        }

        return messageAsDockedEvent;
    }

    public static String asString(Parser parser, JournalEntry journalEntry) {
        StringBuilder sb = new StringBuilder("CMDR ")
            .append(IrcColors.BOLD)
            .append(journalEntry.getHeader().getUploaderId())
            .append(IrcColors.RESET)
            .append(' ');

        switch (journalEntry.getMessage(parser).getEventType()) {
            case DOCKED:
                DockedEvent dockedEvent = journalEntry.getMessageAsDockedEvent(parser);

                sb.append(IrcColors.GREEN)
                    .append("docked")
                    .append(IrcColors.RESET)
                    .append(" at ")
                    .append(dockedEvent.getStationName())
                    .append(" (")
                    .append(dockedEvent.getStationType())
                    .append(" Starport) in the ")
                    .append(dockedEvent.getStarSystem())
                    .append(" star system; ")
                    .append(dockedEvent.getLightsecondsFromStar())
                    .append(" Ls from the main star");
                return sb.toString();
            case FSD_JUMP:
                FsdJumpEvent fsdJumpEvent = journalEntry.getMessageAsFsdJumpEvent(parser);

                sb.append(IrcColors.CYAN)
                    .append("jumped")
                    .append(IrcColors.RESET)
                    .append(" to the ")
                    .append(fsdJumpEvent.getStarSystem())
                    .append(" star system at position [")
                    .append(fsdJumpEvent.getStarPosition()[0])
                    .append(", ")
                    .append(fsdJumpEvent.getStarPosition()[1])
                    .append(", ")
                    .append(fsdJumpEvent.getStarPosition()[2])
                    .append("] (")
                    .append(fsdJumpEvent.getSystemSecurity())
                    .append(" / ");

                if (!fsdJumpEvent.getSystemAllegiance().isEmpty()) {
                    sb.append(fsdJumpEvent.getSystemAllegiance())
                        .append(" / ");
                }

                sb.append(fsdJumpEvent.getSystemGovernment())
                    .append(" / ")
                    .append(fsdJumpEvent.getSystemEconomy())
                    .append(')');

                return sb.toString();
            default:
                throw new IllegalArgumentException();
        }
    }
}
