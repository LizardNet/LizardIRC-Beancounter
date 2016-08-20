package org.lizardirc.beancounter.commands.url;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;
import org.pircbotx.hooks.types.GenericChannelEvent;

import org.lizardirc.beancounter.commands.reddit.RedditPost;
import org.lizardirc.beancounter.commands.reddit.RedditUser;
import org.lizardirc.beancounter.utils.HttpFetcher;
import org.lizardirc.beancounter.utils.IrcColors;

class RedditUrlSummariser implements UrlSummariser {
    private Pattern urlPattern = Pattern.compile("https?://(?:(?:.*?)\\.)?reddit\\.com/(r|u|user)/(.+)");

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
        // https://www.reddit.com/user/stwalkerster/about.json

        String pageContent;

        try {
            URI url = new URIBuilder()
                    .setScheme("https")
                    .setHost("www.reddit.com")
                    .setPath("/user/" + username + "/about.json")
                    .build();

            pageContent = HttpFetcher.getPageContent(url);
        } catch (URISyntaxException | IOException e) {
            return false;
        }

        JsonObject element = new JsonParser().parse(pageContent).getAsJsonObject();
        Gson gson = new Gson();
        RedditUser redditUser = gson.fromJson(element.get("data"), RedditUser.class);

        boolean hasGold = redditUser.isGold();
        boolean isMod = redditUser.isMod();
        int linkKarma = redditUser.getLinkKarma();
        int commentKarma = redditUser.getCommentKarma();

        // TODO: implement me.
        String cakeday = "";

        String gold = hasGold ? " | " + IrcColors.YELLOW + "Gold" + IrcColors.RESET : "";
        String mod = isMod ? " | " + IrcColors.RED + "Mod" + IrcColors.RESET : "";

        String message = String.format(
                "[REDDITOR] %1$s%2$s%3$s%4$s | Link: %5$d | Comment: %6$d",
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
        String pageContent;

        try {
            URI url = new URIBuilder()
                    .setScheme("https")
                    .setHost("www.reddit.com")
                    .setPath("/by_id/t3_" + postId + ".json")
                    .build();

            pageContent = HttpFetcher.getPageContent(url);
        } catch (URISyntaxException | IOException e) {
            return false;
        }

        JsonObject element = new JsonParser().parse(pageContent).getAsJsonObject();
        Gson gson = new Gson();
        JsonArray children = element.get("data").getAsJsonObject().getAsJsonArray("children");
        JsonElement data = children.get(0).getAsJsonObject().getAsJsonObject("data");
        RedditPost post = gson.fromJson(data, RedditPost.class);

        String subredditName = post.getSubreddit();
        String url = post.getUrl();
        String title = post.getTitle();
        String author = post.getAuthor();
        boolean isSelfPost = post.isSelf();
        boolean isNsfw = post.isNsfw();
        double upvoteRatio = post.getUps() / (post.getDowns() + post.getUps());
        int score = post.getScore();
        int commentCount = post.getNumComments();
        Date created = Date.from(Instant.ofEpochSecond(post.getCreatedUtc()));

        String link;
        if (isSelfPost) {
            link = "(self." + subredditName + ")";
        } else {
            link = String.format("( %1$s to r/%2$s )", url, subredditName);
        }

        String nsfw = isNsfw ? " [" + IrcColors.RED + "NSFW" + IrcColors.RESET + "]" : "";
        String percent = String.format(
                "%2$s%1$.2f%%%3$s",
                upvoteRatio * 100,
                score > 0 ? IrcColors.DARKGREEN : IrcColors.RED,
                IrcColors.RESET);

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
