/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 * <p>
 * Copyright (C) 2015-2016 by the LizardIRC Development Team. Some rights reserved.
 * <p>
 * License GPLv3+: GNU General Public License version 3 or later (at your choice):
 * <http://gnu.org/licenses/gpl.html>. This is free software: you are free to
 * change and redistribute it at your will provided that your redistribution, with
 * or without modifications, is also licensed under the GNU GPL. (Although not
 * required by the license, we also ask that you attribute us!) There is NO
 * WARRANTY FOR THIS SOFTWARE to the extent permitted by law.
 * <p>
 * Note that this is an official project of the LizardIRC IRC network.  For more
 * information about LizardIRC, please visit our website at
 * <https://www.lizardirc.org>.
 * <p>
 * This is an open source project. The source Git repositories, which you are
 * welcome to contribute to, can be found here:
 * <https://gerrit.fastlizard4.org/r/gitweb?p=LizardIRC%2FBeancounter.git;a=summary>
 * <https://git.fastlizard4.org/gitblit/summary/?r=LizardIRC/Beancounter.git>
 * <p>
 * Gerrit Code Review for the project:
 * <https://gerrit.fastlizard4.org/r/#/q/project:LizardIRC/Beancounter,n,z>
 * <p>
 * Alternatively, the project source code can be found on the PUBLISH-ONLY mirror
 * on GitHub: <https://github.com/LizardNet/LizardIRC-Beancounter>
 * <p>
 * Note: Pull requests and patches submitted to GitHub will be transferred by a
 * developer to Gerrit before they are acted upon.
 */

package org.lizardirc.beancounter.commands.fishbot;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FishbotHandler<T extends PircBotX> extends ListenerAdapter<T> {

    private HashMap<Pattern, String> messageResponses;
    private HashMap<Pattern, String> actionResponses;

    public FishbotHandler(String botNick) {
        // Init the response maps
        this.initialiseResponses(botNick);
    }

    /**
     * Initialises the hashmaps of responses, pre-compiling regexes
     * <p>
     * TODO: move this out to a resource
     */
    private void initialiseResponses(String botNick) {
        this.messageResponses = new HashMap<>();
        this.actionResponses = new HashMap<>();

        // first, the messages
        this.messageResponses.put(Pattern.compile("hampster", Pattern.CASE_INSENSITIVE), "%n: There is no 'p' in hamster you retard.");
        this.messageResponses.put(Pattern.compile("vinegar.*aftershock", Pattern.CASE_INSENSITIVE), "Ah, a true connoisseur!");
        this.messageResponses.put(Pattern.compile("aftershock.*vinegar", Pattern.CASE_INSENSITIVE), "Ah, a true connoisseur!");
        this.messageResponses.put(Pattern.compile("^some people are being fangoriously devoured by a gelatinous monster$", Pattern.CASE_INSENSITIVE), "Hillary's legs are being digested.");
        this.messageResponses.put(Pattern.compile("^ag$", Pattern.CASE_INSENSITIVE), "Ag, ag ag ag ag ag AG AG AG!");
        this.messageResponses.put(Pattern.compile("^(fishbot|%f) owns$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), "Aye, I do.");
        this.messageResponses.put(Pattern.compile("vinegar", Pattern.CASE_INSENSITIVE), "Nope, too sober for vinegar.  Try later.");
        this.messageResponses.put(Pattern.compile("martian", Pattern.CASE_INSENSITIVE), "Don't run! We are your friends!");
        this.messageResponses.put(Pattern.compile("^just then, he fell into the sea$", Pattern.CASE_INSENSITIVE), "Ooops!");
        this.messageResponses.put(Pattern.compile("aftershock", Pattern.CASE_INSENSITIVE), "mmmm, Aftershock.");
        this.messageResponses.put(Pattern.compile("^why are you here\\?$", Pattern.CASE_INSENSITIVE), "Same reason.  I love candy.");
        this.messageResponses.put(Pattern.compile("^spoon$", Pattern.CASE_INSENSITIVE), "There is no spoon.");
        this.messageResponses.put(Pattern.compile("^(bounce|wertle)$", Pattern.CASE_INSENSITIVE), "moo");
        this.messageResponses.put(Pattern.compile("^crack$", Pattern.CASE_INSENSITIVE), "Doh, there goes another bench!");
        this.messageResponses.put(Pattern.compile("^you can't just pick people at random!$", Pattern.CASE_INSENSITIVE), "I can do anything I like, %n, I'm eccentric!  Rrarrrrrgh!  Go!");
        this.messageResponses.put(Pattern.compile("^flibble$", Pattern.CASE_INSENSITIVE), "plob");
        this.messageResponses.put(Pattern.compile("(the fishbot has created splidge|fishbot created splidge)", Pattern.CASE_INSENSITIVE), "omg no! Think I could show my face around here if I was responsible for THAT?");
        this.messageResponses.put(Pattern.compile("^now there's more than one of them\\?$", Pattern.CASE_INSENSITIVE), "A lot more.");
        this.messageResponses.put(Pattern.compile("^i want everything$", Pattern.CASE_INSENSITIVE), "Would that include a bullet from this gun?");
        this.messageResponses.put(Pattern.compile("we are getting aggravated", Pattern.CASE_INSENSITIVE), "Yes, we are.");
        this.messageResponses.put(Pattern.compile("^how old are you, (fishbot|%f)\\?$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION is older than time itself!" + (char) 0x1);
        this.messageResponses.put(Pattern.compile("^atlantis$", Pattern.CASE_INSENSITIVE), "Beware the underwater headquarters of the trout and their bass henchmen. From there they plan their attacks on other continents.");
        this.messageResponses.put(Pattern.compile("^oh god$", Pattern.CASE_INSENSITIVE), "fishbot will suffice.");
        this.messageResponses.put(Pattern.compile("^(fishbot|%f)$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), "Yes?");
        this.messageResponses.put(Pattern.compile("^what is the matrix\\?$", Pattern.CASE_INSENSITIVE), "No-one can be told what the matrix is.  You have to see it for yourself.");
        this.messageResponses.put(Pattern.compile("^what do you need\\?$", Pattern.CASE_INSENSITIVE), "Guns. Lots of guns.");
        this.messageResponses.put(Pattern.compile("^i know kungfu$", Pattern.CASE_INSENSITIVE), "Show me.");
        this.messageResponses.put(Pattern.compile("^cake$", Pattern.CASE_INSENSITIVE), "fish");
        this.messageResponses.put(Pattern.compile("^trout go m[o0][o0]$", Pattern.CASE_INSENSITIVE), "Aye, that's cos they're fish.");
        this.messageResponses.put(Pattern.compile("^kangaroo$", Pattern.CASE_INSENSITIVE), "The kangaroo is a four winged stinging insect.");
        this.messageResponses.put(Pattern.compile("^sea bass$", Pattern.CASE_INSENSITIVE), "Beware of the mutant sea bass and their laser cannons!");
        this.messageResponses.put(Pattern.compile("^trout$", Pattern.CASE_INSENSITIVE), "Trout are freshwater fish and have underwater weapons.");
        this.messageResponses.put(Pattern.compile("has returned from playing counterstrike", Pattern.CASE_INSENSITIVE), "like we care fs :(");
        this.messageResponses.put(Pattern.compile("^where are we\\?$", Pattern.CASE_INSENSITIVE), "Last time I looked, we were in %c.");
        this.messageResponses.put(Pattern.compile("^where do you want to go today\\?$", Pattern.CASE_INSENSITIVE), "anywhere but redmond :(.");
        this.messageResponses.put(Pattern.compile("^fish go m[o0][o0]$", Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION notes that %n is truly enlightened." + (char) 0x1);
        this.messageResponses.put(Pattern.compile("^(.*) go m[o0][o0]$", Pattern.CASE_INSENSITIVE), "%n: only when they are impersonating fish.");
        this.messageResponses.put(Pattern.compile("^fish go (.+)$", Pattern.CASE_INSENSITIVE), "%n LIES! Fish don't go %1! fish go m00!");
        this.messageResponses.put(Pattern.compile("^you know who else (.*)$", Pattern.CASE_INSENSITIVE), "%n: YA MUM!");
        this.messageResponses.put(Pattern.compile("^if there's one thing i know for sure, it's that fish don't m00\\.?$", Pattern.CASE_INSENSITIVE), "%n: HERETIC! UNBELIEVER!");
        this.messageResponses.put(Pattern.compile("^(fishbot|%f): muahahaha\\. ph33r the dark side\\. :\\)$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), "%n: You smell :P");
        this.messageResponses.put(Pattern.compile("^ammuu\\?$", Pattern.CASE_INSENSITIVE), "%n: fish go m00 oh yes they do!");
        this.messageResponses.put(Pattern.compile("^fish$", Pattern.CASE_INSENSITIVE), "%n: fish go m00!");
        this.messageResponses.put(Pattern.compile("^snake$", Pattern.CASE_INSENSITIVE), "Ah snake a snake! Snake, a snake! Ooooh, it's a snake!");
        this.messageResponses.put(Pattern.compile("^carrots handbags cheese$", Pattern.CASE_INSENSITIVE), "toilets russians planets hamsters weddings poets stalin KUALA LUMPUR! pygmies budgies KUALA LUMPUR!");
        this.messageResponses.put(Pattern.compile("sledgehammer", Pattern.CASE_INSENSITIVE), "sledgehammers go quack!");
        this.messageResponses.put(Pattern.compile("^badger badger badger badger badger badger badger badger badger badger badger badger$", Pattern.CASE_INSENSITIVE), "mushroom mushroom!");
        this.messageResponses.put(Pattern.compile("^moo\\?$", Pattern.CASE_INSENSITIVE), "To moo, or not to moo, that is the question. Whether 'tis nobler in the mind to suffer the slings and arrows of outrageous fish...");
        this.messageResponses.put(Pattern.compile("^herring$", Pattern.CASE_INSENSITIVE), "herring(n): Useful device for chopping down tall trees. Also moos (see fish).");
        this.messageResponses.put(Pattern.compile("www\\.outwar\\.com", Pattern.CASE_INSENSITIVE), "would you please GO AWAY with that outwar rubbish!");
        this.messageResponses.put(Pattern.compile("^god$", Pattern.CASE_INSENSITIVE), "Sometimes the garbage disposal gods demand a spoon.");
        this.messageResponses.put(Pattern.compile("stupid bot[!?.]*$", Pattern.CASE_INSENSITIVE), "%n: Stupid human.");
        this.messageResponses.put(Pattern.compile("fail bot[!?.]*$", Pattern.CASE_INSENSITIVE), "%n: Fail human.");
        this.messageResponses.put(Pattern.compile("good bot[!?.]*$", Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION purrs at %n" + (char) 0x1);
        this.messageResponses.put(Pattern.compile("^I am the Doctor,? and you are the Daleks!?$", Pattern.CASE_INSENSITIVE), "WE ARE THE DALEKS!! Exterminate! EXTEEERRRRMIIINAAAATE!");
        this.messageResponses.put(Pattern.compile("^ping$", Pattern.CASE_INSENSITIVE), "pong");
        this.messageResponses.put(Pattern.compile("^pong$", Pattern.CASE_INSENSITIVE), "pang");
        this.messageResponses.put(Pattern.compile("^pang$", Pattern.CASE_INSENSITIVE), "pung");
        this.messageResponses.put(Pattern.compile("^pung$", Pattern.CASE_INSENSITIVE), "derp");

        // ... and now the actions.
        this.actionResponses.put(Pattern.compile("hampster", Pattern.CASE_INSENSITIVE), "%n: There is no 'p' in hamster you retard.");
        this.actionResponses.put(Pattern.compile("^feeds (fishbot|%f) hundreds and thousands$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), "MEDI.. er.. FISHBOT");
        this.actionResponses.put(Pattern.compile("(vinegar.*aftershock|aftershock.*vinegar)", Pattern.CASE_INSENSITIVE), "Ah, a true connoisseur!");
        this.actionResponses.put(Pattern.compile("vinegar", Pattern.CASE_INSENSITIVE), "Nope, too sober for vinegar.  Try later.");
        this.actionResponses.put(Pattern.compile("martians", Pattern.CASE_INSENSITIVE), "Don't run! We are your friends!");
        this.actionResponses.put(Pattern.compile("aftershock", Pattern.CASE_INSENSITIVE), "mmmm, Aftershock.");
        this.actionResponses.put(Pattern.compile("(the fishbot has created splidge|fishbot created splidge)", Pattern.CASE_INSENSITIVE), "omg no! Think I could show my face around here if I was responsible for THAT?");
        this.actionResponses.put(Pattern.compile("we are getting aggravated", Pattern.CASE_INSENSITIVE), "Yes, we are.");
        this.actionResponses.put(Pattern.compile("^strokes (fishbot|%f)$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION m00s loudly at %n." + (char) 0x1);
        this.actionResponses.put(Pattern.compile("^slaps (.*) around a bit with a large trout$", Pattern.CASE_INSENSITIVE), "trouted!");
        this.actionResponses.put(Pattern.compile("has returned from playing counterstrike", Pattern.CASE_INSENSITIVE), "like we care fs :(");
        this.actionResponses.put(Pattern.compile("^fish go m[o0][o0]$", Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION notes that %n is truly enlightened." + (char) 0x1);
        this.actionResponses.put(Pattern.compile("^(.*) go m[o0][o0]$", Pattern.CASE_INSENSITIVE), "%n: only when they are impersonating fish.");
        this.actionResponses.put(Pattern.compile("^fish go (.+)$", Pattern.CASE_INSENSITIVE), "%n LIES! Fish don't go %1! fish go m00!");
        this.actionResponses.put(Pattern.compile("^you know who else (.*)$", Pattern.CASE_INSENSITIVE), "%n: YA MUM!");
        this.actionResponses.put(Pattern.compile("^thinks happy thoughts about pretty (.*)$", Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION has plenty of pretty %1. Would you like one %n?" + (char) 0x1);
        this.actionResponses.put(Pattern.compile("^snaffles a (.*) off (fishbot|%f).?$".replace("%f", botNick), Pattern.CASE_INSENSITIVE), ":(");
        this.actionResponses.put(Pattern.compile("stupid bot[!?.]*$", Pattern.CASE_INSENSITIVE), "%n: Stupid human.");
        this.actionResponses.put(Pattern.compile("fail bot[!?.]*$", Pattern.CASE_INSENSITIVE), "%n: Fail human.");
        this.actionResponses.put(Pattern.compile("good bot[!?.]*$", Pattern.CASE_INSENSITIVE), (char) 0x1 + "ACTION purrs at %n" + (char) 0x1);
        this.actionResponses.put(Pattern.compile("^ping$", Pattern.CASE_INSENSITIVE), "pong");
        this.actionResponses.put(Pattern.compile("^pong$", Pattern.CASE_INSENSITIVE), "pang");
        this.actionResponses.put(Pattern.compile("^pang$", Pattern.CASE_INSENSITIVE), "pung");
        this.actionResponses.put(Pattern.compile("^pung$", Pattern.CASE_INSENSITIVE), "derp");
    }

    @Override
    public void onGenericChannel(GenericChannelEvent<T> event) throws Exception {
        if (!(event instanceof GenericMessageEvent)) {
            return;
        }

        GenericMessageEvent messageEvent = (GenericMessageEvent) event;

        String message = messageEvent.getMessage();
        User user = messageEvent.getUser();
        Channel channel = event.getChannel();

        if (event instanceof ActionEvent) {
            handleResponse(event, message, this.actionResponses, user, channel);
        }

        if (event instanceof MessageEvent) {
            handleResponse(event, message, this.messageResponses, user, channel);
        }
    }

    private void handleResponse(GenericChannelEvent event, String message, HashMap<Pattern, String> responses, User user, Channel channel) {
        for (Map.Entry<Pattern, String> e : responses.entrySet()) {
            Pattern pattern = e.getKey();
            Matcher matcher = pattern.matcher(message);

            if (matcher.matches()) {
                String response = e.getValue();

                response = response.replace("%n", user.getNick());
                response = response.replace("%c", channel.getName());

                if (matcher.groupCount() > 1) {
                    response = response.replace("%1", matcher.group(1));
                }

                // send to channel
                event.getChannel().send().message(response);
                return;
            }
        }
    }
}
