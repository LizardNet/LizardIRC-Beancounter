/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2020 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.reddit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;

import org.lizardirc.beancounter.utils.Miscellaneous;

public class RedditService {
    Gson gson = new Gson();

    public RedditUser getRedditUser(String username) throws URISyntaxException, IOException {
        URI url = new URIBuilder()
                .setScheme("https")
                .setHost("www.reddit.com")
                .setPath("/user/" + username + "/about.json")
                .build();

        String pageContent = Miscellaneous.getHttpData(url);

        JsonObject element = JsonParser.parseString(pageContent).getAsJsonObject();
        return gson.fromJson(element.get("data"), RedditUser.class);
    }

    public RedditPost getRedditPost(String postId) throws URISyntaxException, IOException {
        URI url = new URIBuilder()
                .setScheme("https")
                .setHost("www.reddit.com")
                .setPath("/comments/" + postId + ".json")
                .addParameter("limit", "1")
                .build();

        String pageContent = Miscellaneous.getHttpData(url);

        JsonArray document = JsonParser.parseString(pageContent).getAsJsonArray();
        JsonObject element = document.get(0).getAsJsonObject();
        JsonArray children = element.get("data").getAsJsonObject().getAsJsonArray("children");
        JsonElement data = children.get(0).getAsJsonObject().getAsJsonObject("data");
        return gson.fromJson(data, RedditPost.class);
    }
}
