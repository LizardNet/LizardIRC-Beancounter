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

import org.lizardirc.beancounter.commands.reddit.RedditPost;
import org.lizardirc.beancounter.commands.reddit.RedditService;
import org.lizardirc.beancounter.commands.reddit.RedditUser;

class RedditUrlSummariser implements UrlSummariser {
    private Pattern urlPattern = Pattern.compile("https?://(?:(?:.*?)\\.)?reddit\\.com/(r|u|user)/(.+)");

    private final RedditService redditService;

    RedditUrlSummariser(RedditService redditService) {
        this.redditService = redditService;
    }


    @Override
    public boolean summariseUrl(URI url, GenericChannelEvent event) {
        Matcher m = urlPattern.matcher(url.toString());
        if (!m.matches()) {
            return false;
        }

        String[] context = m.group(2).split("/");
        switch (m.group(1)) {
            case "r":
                String postId = context[2];
                return handlePost(event, postId);
            case "u":
            case "user":
                String username = context[0];
                return handleUser(event, username);
        }

        return false;
    }

    private boolean handleUser(GenericChannelEvent event, String username) {
        try {
            RedditUser redditUser = redditService.getRedditUser(username);
            event.getChannel().send().message(redditUser.toString());
            return true;
        } catch (URISyntaxException | IOException e) {
            return false;
        }
    }

    private boolean handlePost(GenericChannelEvent event, String postId) {
        try {
            RedditPost redditPost = redditService.getRedditPost(postId);
            event.getChannel().send().message(redditPost.toString());
            return true;
        } catch (URISyntaxException | IOException e) {
            return false;
        }
    }

}
