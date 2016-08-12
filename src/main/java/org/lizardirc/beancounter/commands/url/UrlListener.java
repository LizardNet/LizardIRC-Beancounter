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

import java.net.URI;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class UrlListener<T extends PircBotX> extends ListenerAdapter<T> {

    private final HashMap<String, UrlSummariser> domainMap;
    private final DefaultUrlSummariser defaultSummariser;

    private Pattern urlPattern;

    public UrlListener() {
        urlPattern = Pattern.compile("(https?://\\S+)");

        this.defaultSummariser = new DefaultUrlSummariser();
        this.domainMap = new HashMap<>();

        UrlSummariser enwikiSummariser = new WikipediaUrlSummariser();
        domainMap.put("enwp.org", enwikiSummariser);
        domainMap.put("en.wikipedia.org", enwikiSummariser);


        RedditUrlSummariser redditUrlSummariser = new RedditUrlSummariser();
        domainMap.put("www.reddit.com", redditUrlSummariser);
        domainMap.put("reddit.com", redditUrlSummariser);

        UrlSummariser youtubeSummariser = this.defaultSummariser;
        domainMap.put("www.youtube.com", youtubeSummariser);
        domainMap.put("youtu.be", youtubeSummariser);
    }

    @Override
    public void onGenericMessage(GenericMessageEvent<T> event) throws Exception {
        // not a channel event
        if (!(event instanceof GenericChannelEvent) || (!(event instanceof MessageEvent))) {
            return;
        }

        String message = event.getMessage();
        Matcher matcher = urlPattern.matcher(message);

        if (!matcher.find()) {
            return;
        }

        URI url = new URI(matcher.group(1));

        UrlSummariser s = defaultSummariser;
        if(domainMap.containsKey(url.getHost())){
            s = domainMap.get(url.getHost());
        }

        boolean result;

        // Use the specialised summariser, and if it fails, fall back to the default one
        try {
            result = s.summariseUrl(url, (GenericChannelEvent) event);
        }
        catch(Throwable ex)
        {
            // log the error
            System.err.println(ex.getMessage());

            // fall back to the generic handler
            result = false;
        }

        if(!result && !(s instanceof DefaultUrlSummariser)){
            defaultSummariser.summariseUrl(url, (GenericChannelEvent) event);
        }
    }
}
