package org.lizardirc.beancounter.commands.url;

import net.dean.jraw.http.UserAgent;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;

import java.net.URL;

import net.dean.jraw.RedditClient;

public class RedditUrlSummariser implements UrlSummariser {
    @Override
    public void summariseUrl(URL url, GenericChannelEvent event) {
        UserAgent agent = UserAgent.of("Beancounter/0.1");

        RedditClient r = new RedditClient(agent);
        
    }
}
