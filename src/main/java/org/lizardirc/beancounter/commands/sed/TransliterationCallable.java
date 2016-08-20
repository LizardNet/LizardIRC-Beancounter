/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.sed;

import org.lizardirc.beancounter.utils.InterruptibleCharSequence;
import org.lizardirc.beancounter.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

class TransliterationCallable implements Callable<Optional<UserMessage>> {
    private final String toReplace;
    private final String replacement;
    private final List<Pair<UserMessageType, ? extends CharSequence>> window;

    public TransliterationCallable(String toReplace, String replacement, Queue<UserMessage> window) {
        this.toReplace = toReplace;
        this.replacement = replacement;
        this.window = window.stream()
            .map(entry -> entry.mapRight(InterruptibleCharSequence::new))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<UserMessage> call() throws Exception {
        String[] toReplace = this.toReplace.split("");
        String[] replacements = this.replacement.split("");

        HashMap<String, String> h = new HashMap<>();
        for (int i = toReplace.length - 1; i >= 0; i--) {
            h.put(toReplace[i], replacements[i]);
        }

        return window.stream()
            .filter(item -> {
                String message = item.getRight().toString();
                for (String aToReplace : toReplace) {
                    if (!message.contains(aToReplace)) {
                        return false;
                    }
                }
                return true;
            })
            .reduce((x, y) -> y) // findLast()
            .map(item -> {
                String[] message = item.getRight().toString().split("");
                StringBuilder sb = new StringBuilder();
                for (String key : message) {
                    if (h.containsKey(key)) {
                        sb.append(h.get(key));
                    } else {
                        sb.append(key);
                    }
                }

                return new UserMessage(item.getLeft(), sb.toString());
            });
    }
}
