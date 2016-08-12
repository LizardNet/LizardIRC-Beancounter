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
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class WikipediaSummaryService {
    public WikipediaPage getSummary(String language, String article) throws URISyntaxException, IOException {
        URI apiCall = new URIBuilder("https://" + language + ".wikipedia.org/w/api.php")
                .addParameter("format", "json")
                .addParameter("action", "query")
                .addParameter("prop", "extracts")
                .addParameter("exintro", null)
                .addParameter("explaintext", null)
                .addParameter("exchars", "300")
                .addParameter("redirects", null)
                .addParameter("titles", article)
                .build();

        String pageContent = getPageContent(apiCall);
        JsonElement element = new JsonParser().parse(pageContent);
        JsonObject obj = element.getAsJsonObject();
        JsonObject pages = obj.getAsJsonObject("query").getAsJsonObject("pages");
        Set<Map.Entry<String, JsonElement>> entries = pages.entrySet();

        Map.Entry<String, JsonElement> pageEntry = entries.iterator().next();
        int pageId = Integer.parseInt(pageEntry.getKey());

        JsonObject page = pageEntry.getValue().getAsJsonObject();

        String title = page.get("title").getAsString();
        String extract = page.get("extract").getAsString();

        return new WikipediaPage(title, pageId, extract);
    }

    private String getPageContent(URI apiCall) throws IOException {
        HttpGet request = new HttpGet(apiCall);
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        HttpClient httpClient = httpClientBuilder.build();
        HttpResponse response = httpClient.execute(request);

        return EntityUtils.toString(response.getEntity());
    }
}
