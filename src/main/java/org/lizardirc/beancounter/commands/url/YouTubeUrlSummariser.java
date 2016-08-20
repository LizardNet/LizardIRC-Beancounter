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
package org.lizardirc.beancounter.commands.url;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.types.GenericChannelEvent;

import org.lizardirc.beancounter.commands.youtube.YouTubeService;
import org.lizardirc.beancounter.commands.youtube.YouTubeVideo;

class YouTubeUrlSummariser implements UrlSummariser {
    private Pattern urlPattern = Pattern.compile("https?://(?:www.)?youtu(?:\\.be/|be\\.com/watch\\?v=)(.+)");

    private final YouTubeService youTubeService;

    YouTubeUrlSummariser(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @Override
    public boolean summariseUrl(URI url, GenericChannelEvent event) {
        Matcher m = urlPattern.matcher(url.toString());
        if (!m.matches()) {
            return false;
        }

        try {
            YouTubeVideo videoInformation = this.youTubeService.getVideoInformation(m.group(1));
            event.getChannel().send().message(videoInformation.toString());

            return true;
        } catch (IOException | URISyntaxException poo) {
            return false;
        }
    }

}
