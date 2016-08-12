package org.lizardirc.beancounter.commands.url;

import java.net.URI;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.types.GenericChannelEvent;

import org.lizardirc.beancounter.utils.IrcColors;

class RedditUrlSummariser implements UrlSummariser {
    private Pattern urlPattern = Pattern.compile("https?://(?:.*?)\\.reddit\\.com/([ru])/(.+)");

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
                String username = context[0];
                return handleUser(event, username);
        }

        return false;
    }

    private boolean handleUser(GenericChannelEvent event, String username) {
        // https://www.reddit.com/user/stwalkerster/about.json

        // TODO: implement me.
        String cakeday = "";
        boolean hasGold = false, isMod = false;
        int linkKarma = 0, commentKarma = 0;

        String gold = hasGold ? " | " + IrcColors.COLOR_YELLOW + "Gold" + IrcColors.COLOR_RESET : "";
        String mod = isMod ? " | " + IrcColors.COLOR_RED + "Mod" + IrcColors.COLOR_RESET : "";

        String message = String.format(
                "[REDDITOR] %1$s%2$s%3$s%4$s | Link: %5%d | Comment: %6$d",
                username,
                cakeday,
                gold,
                mod,
                linkKarma,
                commentKarma);

        event.getChannel().send().message(message);

        return true;
    }

    private boolean handlePost(GenericChannelEvent event, String postId) {
        // https://www.reddit.com/by_id/t3_4xzrvv.json  - self
        // https://www.reddit.com/by_id/t3_4y05or.json  - image

        String subredditName = "", url = "", title = "", author = "";
        boolean isSelfPost = false, isNsfw = false;
        double upvoteRatio = 0;
        int score = 0, commentCount = 0;
        Date created = null;

        String link;
        if (isSelfPost) {
            link = "(self." + subredditName + ")";
        } else {
            link = String.format("%1$s to r/%2$s", url, subredditName);
        }

        String nsfw = isNsfw ? " [" + IrcColors.COLOR_RED + "NSFW" + IrcColors.COLOR_RESET + "]" : "";
        String percent = String.format(
                "%2$s%1$.2f%%%3$s",
                upvoteRatio * 100,
                score > 0 ? IrcColors.COLOR_GREEN : IrcColors.COLOR_RED,
                IrcColors.COLOR_RESET);

        String message = String.format(
                "[REDDIT] %1$s %2$s%3$s | %4$d points (%5$s) | %6$d comments | Posted by %7$s | Created at %8$tF %8$tT",
                title,
                link,
                nsfw,
                score,
                percent,
                commentCount,
                author == null ? "[deleted]" : author,
                created);

        event.getChannel().send().message(message);

        return true;
    }
}
