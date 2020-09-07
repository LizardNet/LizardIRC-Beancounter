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

package org.lizardirc.beancounter.commands.memes;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import org.lizardirc.beancounter.utils.Strings;

class Lamborghini {
    private final String quote;
    @SerializedName("default_replacement") private final String defaultReplacement;
    private final String additional;
    private final LamborghiniTransformers[] transformers;

    public Lamborghini(String quote, String defaultReplacement) {
        this.quote = Objects.requireNonNull(quote);
        this.defaultReplacement = Objects.requireNonNull(defaultReplacement);
        additional = null;
        transformers = null;
    }

    public Lamborghini(String quote, String defaultReplacement, String additional) {
        this.quote = Objects.requireNonNull(quote);
        this.defaultReplacement = Objects.requireNonNull(defaultReplacement);
        this.additional = additional;
        this.transformers = null;
    }

    public Lamborghini(String quote, String defaultReplacement, String additional, LamborghiniTransformers... transformers) {
        this.quote = Objects.requireNonNull(quote);
        this.defaultReplacement = Objects.requireNonNull(defaultReplacement);
        this.additional = additional;
        this.transformers = transformers;
    }

    public String getQuote() {
        return getQuote(null);
    }

    public String getQuote(String replacementWord) {
        String word;

        if (replacementWord == null) {
            word = defaultReplacement;
        } else {
            word = transform(replacementWord);
        }

        String indefiniteArticleSuffix = Strings.startsWithVowel(word) ? "n" : "";
        return String.format(quote, word, indefiniteArticleSuffix);
    }

    private String transform(final String input) {
        if (transformers == null || input.isEmpty()) {
            return input;
        } else {
            StringBuilder output = new StringBuilder(input);

            for (LamborghiniTransformers transformer : transformers) {
                switch (transformer) {
                    case ADDITIONAL_PREPEND:
                        // Silently do nothing if the "additional" is unspecified
                        if (additional != null) {
                            output.insert(0, additional);
                        }
                        break;
                    case ADDITIONAL_APPEND:
                        // Silently do nothing if the "additional" is unspecified
                        if (additional != null) {
                            output.append(additional);
                        }
                        break;
                    case ALL_CAPS:
                        output = new StringBuilder(output.toString().toUpperCase());
                        break;
                    case PAD_LEFT:
                        output.insert(0, ' ');
                        break;
                    case PAD_RIGHT:
                        output.append(' ');
                        break;
                }
            }

            return output.toString();
        }
    }
}
