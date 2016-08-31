/*
 * === BOT CODE LICENSE ===
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
 *
 * === GAME CODE LICENSE ===
 * Secret Hitler was created by Mike Boxleiter, Tommy Maranges, Max Temkin, and Mac Schubert.
 * <http://www.secrethitler.com/>
 *
 * Secret Hitler is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 *
 * YOU ARE FREE TO:
 * • Share
 *  — copy and redistribute the game in any medium or format
 * • Adapt
 *  — remix, transform, and build upon the game
 *
 * UNDER THE FOLLOWING TERMS:
 * • Attribution
 *  — If you make something using our game, you need to give us credit and link back to us, and you need to explain what
 *    you changed.
 * • Non-Commercial
 *  — You can’t use our game to make money.
 * • Share Alike
 *  — If you remix, transform, or build upon our game, you have to release your work under the same Creative Commons
 *    license that we use (BY-NC-SA 4.0).
 * • No additional restrictions
 *  — You can’t apply legal terms or technological measures to your work that legally restrict others from doing anything
 *    our license allows. That means you can’t submit anything using our game to any app store without our approval
 *
 * You can learn more about Creative Commons at CreativeCommons.org. (Our license is available at
 * CreativeCommons.org/licenses/by-nc-sa/4.0/legalcode).
 *
 * == SUMMARY OF CHANGES MADE FROM ORIGINAL GAME IN BEANCOUNTER ==
 * The changes made from the original game in Beancounter are basically those necessary to make the game work on the IRC
 * medium.  As much as possible, gameplay follows the exact same rules as the original game.
 */

package org.lizardirc.beancounter.games.secrethitler;

import org.lizardirc.beancounter.utils.IrcColors;

enum Role {
    LIBERAL(Party.LIBERALS, "Liberal", "You are a " + IrcColors.BOLD + "Liberal" + IrcColors.BOLD + ".  It is your job " +
        "to figure out who the Hitler is!  The Liberals win if five Liberal policies are enacted, OR the Hitler is " +
        "assassinated."),
    FASCIST(Party.FASCISTS, "Fascist", "You are a " + IrcColors.BOLD + "Fascist" + IrcColors.BOLD + ".  It is your job " +
        "to be manipulative, create confusion, and take control of the government!  The Fascists and the Hitler win if " +
        "six Fascist policies are enacted, or if the Hitler is elected Chancellor any time after the third Fascist " +
        "policy has been enacted."),
    HITLER(Party.FASCISTS, "Hitler", "You are the " + IrcColors.BOLD + "Hitler" + IrcColors.BOLD + "!  It is your job " +
        "to not draw suspicion to yourself, and to be elected Chancellor!  You and the Fascists win if six Fascist " +
        "policies are enacted, or if you are elected Chancellor any time after the third Fascist policy has been enacted.");

    private final Party party;
    private final String asString;
    private final String description;

    Role(Party party, String asString, String description) {
        this.party = party;
        this.asString = asString;
        this.description = description;
    }

    public Party getParty() {
        return party;
    }

    @Override
    public String toString() {
        return asString;
    }

    public String getDescription() {
        return description;
    }
}
