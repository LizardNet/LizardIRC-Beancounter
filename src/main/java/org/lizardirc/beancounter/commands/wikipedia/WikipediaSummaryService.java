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

package org.lizardirc.beancounter.commands.wikipedia;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.lizardirc.beancounter.utils.Miscellaneous;

public class WikipediaSummaryService {
    public WikipediaPage getSummary(String language, String article) throws URISyntaxException, IOException {
        URI apiUrl = URI.create("https://" + language + ".wikipedia.org/w/api.php");

        return getSummary(article, apiUrl, false);
    }

    public WikipediaPage getSummary(String language, String article, boolean getSiteName) throws URISyntaxException, IOException {
        URI apiUrl = URI.create("https://" + language + ".wikipedia.org/w/api.php");

        return getSummary(article, apiUrl, getSiteName);
    }

    public WikipediaPage getSummary(String article, URI apiUrl) throws URISyntaxException, IOException {
        return getSummary(article, apiUrl, false);
    }

    public WikipediaPage getSummary(String article, URI apiUrl, boolean getSiteName) throws URISyntaxException, IOException {
        URI apiCall = new URIBuilder(apiUrl)
                .addParameter("format", "json")
                .addParameter("action", "query")
                .addParameter("prop", "info|extracts")
                .addParameter("inprop", "url|displaytitle")
                .addParameter("exintro", null)
                .addParameter("explaintext", null)
                .addParameter("exchars", "250")
                .addParameter("redirects", null)
                .addParameter("titles", article)
                .addParameter("iwurl", "1")
                .addParameter("meta", "siteinfo")
                .build();

        String pageContent = Miscellaneous.getHttpData(apiCall);
        JsonElement element = new JsonParser().parse(pageContent);
        JsonObject obj = element.getAsJsonObject();
        JsonObject query = obj.getAsJsonObject("query");

        // Check this isn't an interwiki
        JsonElement interwiki = query.get("interwiki");
        if (interwiki != null) {
            JsonArray interwikiArray = interwiki.getAsJsonArray();
            JsonObject firstInterwiki = interwikiArray.get(0).getAsJsonObject();
            String url = firstInterwiki.get("url").getAsString();

            // trim off the interwiki prefix
            article = article.substring(firstInterwiki.get("iw").getAsString().length() + 1);
            return handleReallySimpleDiscovery(article, url);
        }

        JsonObject pages = query.getAsJsonObject("pages");
        if (pages == null) {
            throw new MediaWikiApiError("No page requested");
        }

        Set<Map.Entry<String, JsonElement>> entries = pages.entrySet();
        Map.Entry<String, JsonElement> pageEntry = entries.iterator().next();
        JsonObject page = pageEntry.getValue().getAsJsonObject();

        String siteName = null;
        if (getSiteName) {
            // get the site name
            siteName = query.get("general").getAsJsonObject().get("sitename").getAsString();
        }

        return constructWikipediaPageFromJson(page, siteName);
    }

    private WikipediaPage constructWikipediaPageFromJson(JsonObject page, String siteName) throws MediaWikiApiError {
        // Check the page exists
        JsonElement missing = page.get("missing");
        if (missing != null) {
            return null;
        }

        JsonElement special = page.get("special");
        if (special != null) {
            return null;
        }

        JsonElement invalidreason = page.get("invalidreason");
        if (invalidreason != null) {
            throw new MediaWikiApiError(invalidreason.getAsString());
        }

        JsonElement extractElement = page.get("extract");
        String extract = null;
        if (extractElement != null) {
            extract = extractElement.getAsString();

            // Fix for extracts containing a new line. We probably only want the first paragraph anyway.
            String[] split = extract.split("[\r\n]");
            if (split.length > 1) {
                extract = split[0];
            }
        }

        JsonElement displayTitleElement = page.get("displaytitle");
        if (displayTitleElement == null) {
            // Hrm. Old version of MW. Fall back to the normal title
            displayTitleElement = page.get("title");
        }
        String displayTitle = Jsoup.parse(displayTitleElement.getAsString()).text();

        String canonicalUrl;
        JsonElement canonicalUrlElement = page.get("canonicalurl");
        if (canonicalUrlElement == null) {
            // Hrm. Old version of MW. Fall back to the full url
            canonicalUrlElement = page.get("fullurl");
        }

        canonicalUrl = canonicalUrlElement.getAsString();

        return new WikipediaPage(displayTitle, canonicalUrl, extract, siteName);
    }

    /**
     * This method used when we get an interwiki response from the API.
     * <p>
     * The interwiki gives us a URL to check, so we go there and parse the HTML for the API link using RSD.
     * <p>
     * Then, we just re-enter the code above and execute the request again on the new API.
     *
     * @param article   The article name
     * @param targetUrl The URL of the interwiki-redirected article
     * @return A Wikipedia page object, or null if not found
     * @throws IOException
     * @throws URISyntaxException
     */
    private WikipediaPage handleReallySimpleDiscovery(String article, String targetUrl) throws IOException, URISyntaxException {

        // Fetch and parse the HTML document, ignoring HTTP errors - we still want to parse the 404 (etc)!
        Document document = Jsoup.connect(targetUrl).ignoreHttpErrors(true)
                .userAgent(Miscellaneous.generateHttpUserAgent()).get();

        Optional<Element> rsdLink = document.getElementsByTag("link").stream()
                .filter(x -> x.hasAttr("rel") && x.attr("rel").equals("EditURI") && x.hasAttr("type") && x.attr("type")
                        .equals("application/rsd+xml"))
                .findFirst();

        String apiPath = null;

        if (rsdLink.isPresent()) {
            // Use autodiscover path
            apiPath = rsdLink.get().attr("href");

            // We probably *ought* to check the destination API is actually MediaWiki api, but I'm lazy.
            // TODO: stop being lazy.
        } else {
            // OK.
            // AutoDiscovery failed, let's be more naive about this.
            // either a) the target has RSD and/or the API disabled, or b) they're not using MW
            // We can check the latter with <meta rel="generator" content="MediaWiki ..." />
            Optional<Element> metaGenerator = document.getElementsByTag("meta")
                    .stream()
                    .filter(x -> x.hasAttr("name") && x.attr("name").equals("generator") && x.hasAttr("content"))
                    .findFirst();

            if (!metaGenerator.isPresent() || !metaGenerator.get().attr("content").startsWith("MediaWiki ")) {
                // Not MediaWiki or not exposing that we're MediaWiki.
                return null;
            }

            // Try to naively get the API url.
            if (targetUrl.contains("/wiki/")) {
                if (targetUrl.contains("index.php")) {
                    // urgh. either a) not MW, or b) lrn2read MW install documentation
                    apiPath = targetUrl.replaceFirst("index\\.php.*$", "api.php");
                } else {
                    apiPath = targetUrl.replaceFirst("/wiki/.*", "/w/api.php");
                }

                // Test our assumptions - somewhat naively and slowly.
                if (!Jsoup.connect(apiPath)
                        .userAgent(Miscellaneous.generateHttpUserAgent())
                        .get()
                        .title()
                        .contains("MediaWiki API")) {
                    apiPath = null;
                }
            }
        }

        if (apiPath == null) {
            return null;
        }

        URIBuilder builder = new URIBuilder(apiPath).removeQuery();

        if (builder.getScheme() == null) {
            builder.setScheme("https");
        }

        return getSummary(article, builder.build(), true);
    }
}
