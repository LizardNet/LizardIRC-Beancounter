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
import java.net.URISyntaxException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.pircbotx.hooks.types.GenericChannelEvent;

import org.lizardirc.beancounter.commands.wikipedia.WikipediaPage;
import org.lizardirc.beancounter.commands.wikipedia.WikipediaSummaryService;

class WikipediaUrlSummariser implements UrlSummariser {
    private final Pattern wikipediaPattern = Pattern.compile("(?<lang>[a-z-]+)\\.wikipedia\\.org");

    private final WikipediaSummaryService wikipediaSummaryService = new WikipediaSummaryService();

    @Override
    public boolean summariseUrl(URI url, GenericChannelEvent event) {
        // http://enwp.org/ARTICLE
        // https://en.wikipedia.org/wiki/ARTICLE
        // https://en.wikipedia.org/w/index.php?diff=REVISION
        // https://en.wikipedia.org/w/index.php?title=ARTICLE

        if (url.getHost().equals("enwp.org")) {
            String article = url.getPath().substring(1);
            String language = "en";

            return handleArticle(event, language, article);
        }

        Matcher wikipediaMatcher = wikipediaPattern.matcher(url.getHost());
        if (wikipediaMatcher.matches()) {
            String language = wikipediaMatcher.group("lang");

            // Test the standard article path
            if (url.getPath().startsWith("/wiki/")) {
                String article = url.getPath().substring(6);
                return handleArticle(event, language, article);
            }

            // Test the index path
            if (url.getPath().equals("/w/index.php")) {
                return handleQueryPath(event, language, url);
            }
        }

        return false;
    }

    private boolean handleQueryPath(GenericChannelEvent event, String language, URI url) {
        URIBuilder builder = new URIBuilder(url);
        List<NameValuePair> params = builder.getQueryParams();

        String title = null;
        for (NameValuePair param : params) {
            if (param.getName().equals("title")) {
                title = param.getValue();
                break;
            }
        }

        return title != null && handleArticle(event, language, title);
    }

    private boolean handleArticle(GenericChannelEvent event, String language, String article) {

        try {
            WikipediaPage wikipediaPage = wikipediaSummaryService.getSummary(language, article, true);

            if (wikipediaPage == null) {
                // page doesn't exist.
                return false;
            }

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(wikipediaPage.getSiteName())
                    .append(" | ")
                    .append(wikipediaPage.getDisplayTitle());

            if (wikipediaPage.getSummary() != null) {
                stringBuilder.append(" | ")
                        .append(wikipediaPage.getSummary());
            }

            String response = stringBuilder.toString();

            event.getChannel().send().message(response);

            return true;
        } catch (URISyntaxException | IOException poo) {
            return false;
        }
    }
}
