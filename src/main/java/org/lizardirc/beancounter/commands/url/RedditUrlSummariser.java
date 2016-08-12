package org.lizardirc.beancounter.commands.url;

import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Account;
import net.dean.jraw.models.Submission;
import org.pircbotx.hooks.types.GenericChannelEvent;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dean.jraw.RedditClient;

import org.lizardirc.beancounter.utils.IrcColors;

class RedditUrlSummariser implements UrlSummariser {
    // TODO: externalise this somewhere?
    private static final String USER_AGENT = "Beancounter/0.1";
    private final RedditClient reddit;

    private Pattern urlPattern = Pattern.compile("https?://(?:.*?)\\.reddit\\.com/([ru])/(.+)");

    RedditUrlSummariser() {
        UserAgent agent = UserAgent.of(USER_AGENT);
        reddit = new RedditClient(agent);
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
                String username = context[0];
                return handleUser(event, username);
        }

        return false;
    }

    private boolean handleUser(GenericChannelEvent event, String username) {
        Account user = reddit.getUser(username);

        // TODO: implement me.
        String cakeday = "";

        String gold = user.hasGold() ? " | " + IrcColors.COLOR_YELLOW + "Gold" + IrcColors.COLOR_RESET : "";
        String mod = user.isMod() ? " | " + IrcColors.COLOR_RED + "Mod" + IrcColors.COLOR_RESET : "";

        String message = String.format(
                "[REDDITOR] %1$s%2$s%3$s%4$s | Link: %5%d | Comment: %6$d",
                username,
                cakeday,
                gold,
                mod,
                user.getLinkKarma(),
                user.getCommentKarma());

        event.getChannel().send().message(message);

        return true;
    }

    private boolean handlePost(GenericChannelEvent event, String postId) {
        Submission submission = reddit.getSubmission(postId);

        String link;
        if (submission.isSelfPost()) {
            link = "(self." + submission.getSubredditName() + ")";
        } else {
            link = String.format("%1$s to r/%2$s", submission.getUrl(), submission.getSubredditName());
        }

        String nsfw = submission.isNsfw() ? " [" + IrcColors.COLOR_RED + "NSFW" + IrcColors.COLOR_RESET + "]" : "";
        String percent = String.format(
                "%2$s%1$.2f%%%3$s",
                submission.getUpvoteRatio() * 100,
                submission.getScore() > 0 ? IrcColors.COLOR_GREEN : IrcColors.COLOR_RED,
                IrcColors.COLOR_RESET);

        String message = String.format(
                "[REDDIT] %1$s %2$s%3$s | %4$d points (%5$s) | %6$d comments | Posted by %7$s | Created at %8$tF %8$tT",
                submission.getTitle(),
                link,
                nsfw,
                submission.getScore(),
                percent,
                submission.getCommentCount(),
                submission.getAuthor() == null ? "[deleted]" : submission.getAuthor(),
                submission.getCreated());

        event.getChannel().send().message(message);

        return true;
    }
}
