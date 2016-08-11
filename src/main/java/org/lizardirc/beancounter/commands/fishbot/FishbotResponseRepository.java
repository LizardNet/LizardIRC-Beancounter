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
 * <https://git.fqastlizard4.org/gitblit/summary/?r=LizardIRC/Beancounter.git>
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
package org.lizardirc.beancounter.commands.fishbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import org.lizardirc.beancounter.Beancounter;

public class FishbotResponseRepository {
    private List<FishbotResponse> messages;
    private List<FishbotResponse> actions;

    private FishbotResponseRepository(List<FishbotResponse> messages, List<FishbotResponse> actions) {
        this.messages = messages;
        this.actions = actions;

    }

    public static FishbotResponseRepository initialise() {
        try (InputStream fishbotFile = Beancounter.class.getResourceAsStream("/fishbot.json")) {
            Gson fishbotDeserialiser = new Gson();
            InputStreamReader repositoryReader = new InputStreamReader(fishbotFile);

            return fishbotDeserialiser.fromJson(repositoryReader, FishbotResponseRepository.class);
        } catch (IOException | NullPointerException e) {
            System.err.println("Caught IOException or NullPointerException trying to read in fishb0t m00s: " + e.getMessage());
            e.printStackTrace();
            return new FishbotResponseRepository(new ArrayList<>(), new ArrayList<>());
        }
    }

    List<FishbotResponse> getMessages() {
        return this.messages;
    }

    List<FishbotResponse> getActions() {
        return this.actions;
    }

    synchronized void recompile(String nickname) {
        this.messages.forEach(r -> r.compile(nickname));
        this.actions.forEach(r -> r.compile(nickname));
    }
}
