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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.pircbotx.hooks.types.GenericChannelEvent;

class WikipediaUrlSummariser extends UrlSummariserBase implements UrlSummariser {
    private Pattern wikipediaPattern = Pattern.compile("(?<lang>[a-z-]+)\\.wikipedia\\.org");

    @Override
    public boolean summariseUrl(URL url, GenericChannelEvent event) {
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
                return handleQueryPath(event, language, url.toExternalForm());
            }
        }

        return false;
    }

    private boolean handleQueryPath(GenericChannelEvent event, String language, String url) {
        URIBuilder builder;

        try {
            builder = new URIBuilder(url);
        } catch (URISyntaxException poo) {
            return false;
        }

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
            URL apiCall = new URIBuilder("https://" + language + ".wikipedia.org/w/api.php")
                    .addParameter("format", "json")
                    .addParameter("action", "query")
                    .addParameter("prop", "extracts")
                    .addParameter("exintro", null)
                    .addParameter("explaintext", null)
                    .addParameter("exchars", "300")
                    .addParameter("redirects", null)
                    .addParameter("titles", article)
                    .build()
                    .toURL();

            String pageContent = this.getPageContent(apiCall);
            JsonElement element = new JsonParser().parse(pageContent);
            JsonObject obj = element.getAsJsonObject();
            JsonObject pages = obj.getAsJsonObject("query").getAsJsonObject("pages");
            Set<Map.Entry<String, JsonElement>> entries = pages.entrySet();

            JsonObject page = entries.iterator().next().getValue().getAsJsonObject();

            String message = String.format(
                    "[WIKIPEDIA] %1$s | %2$s",
                    page.get("title").getAsString(),
                    page.get("extract").getAsString());

            event.getChannel().send().message(message);

            return true;
        } catch (URISyntaxException poo) {
            // wat.
            return false;
        } catch (MalformedURLException poo) {
            return false;
        } catch (IOException poo) {
            return false;
        }
    }
}
