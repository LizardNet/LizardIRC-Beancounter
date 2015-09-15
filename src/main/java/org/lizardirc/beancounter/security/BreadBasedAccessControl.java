/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.persistence.PersistenceManager;

// This pun is entirely TLUL's fault.
public class BreadBasedAccessControl<T extends PircBotX> implements AccessControl<T> {
    private enum RemoveFromMultimapResults {
        SUCCESS,
        E_NOSUCHKEY,
        E_NOSUCHVALUE
    }

    private final BreadBasedAccessControlListener<T> listener = new BreadBasedAccessControlListener<>();
    private Map<String, Set<String>> hostmasksToRoles;
    private Map<String, Set<String>> rolesToPermissions;
    private PersistenceManager pm;

    private final Set<String> seenPermissions = new HashSet<>();

    public BreadBasedAccessControl(String ownerMask, PersistenceManager pm) {
        this.pm = pm;

        hostmasksToRoles = cloneMultimap(this.pm.getMultimap("hostmasksToRoles"));
        rolesToPermissions = cloneMultimap(this.pm.getMultimap("rolesToPermissions"));

        try {
            grantRoleToHostmask(ownerMask, "*");
        } catch(IllegalArgumentException e) {
            // Ignore!
        }

        addToMultimap(rolesToPermissions, "*", "*");
    }

    @Override
    public synchronized boolean hasPermission(GenericUserEvent<?> event, String checkPermission) {
        seenPermissions.add(checkPermission);

        return getPermissionStream(event)
                .anyMatch(perm -> "*".equals(perm) || checkPermission.equals(perm));
    }

    @Override
    public synchronized Set<String> getPermissions(GenericUserEvent<?> event) {
        return getPermissionStream(event).collect(Collectors.toSet());
    }

    private synchronized Stream<String> getPermissionStream(GenericUserEvent<?> event) {
        String userHostmask = event.getUser().getNick() + "!" + event.getUser().getLogin() + "@" + event.getUser().getHostmask();

        return hostmasksToRoles.entrySet().stream()
                .filter(e -> userHostmask.matches(e.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Set::stream)
                .flatMap(role -> rolesToPermissions.getOrDefault(role, Collections.emptySet()).stream())
                .distinct();
    }

    private synchronized void grantRoleToHostmask(String hostmask, String role) throws IllegalArgumentException {
        if (!rolesToPermissions.containsKey(role) && !role.equals("*")) {
            throw new IllegalArgumentException("Role \"" + role + "\" does not exist (create it first by granting it permissions)");
        }

        if (!addToMultimap(hostmasksToRoles, hostmask, role)) {
            throw new IllegalArgumentException("Hostmask \"" + hostmask + "\" is already granted role \"" + role + "\"");
        }
    }

    private synchronized void grantPermissionToRole(String role, String permission) throws IllegalArgumentException {
        if (role.equals("*")) {
            throw new IllegalArgumentException("The pseudo-role \"*\" cannot be modified");
        }

        if (!addToMultimap(rolesToPermissions, role, permission)) {
            throw new IllegalArgumentException("Role \"" + role + "\" is already granted permission \"" + permission + "\"");
        }
    }


    private synchronized void revokeRoleFromHostmask(String hostmask, String role) throws IllegalArgumentException {
        switch (removeFromMultimap(hostmasksToRoles, hostmask, role)) {
            case E_NOSUCHVALUE:
                throw new IllegalArgumentException("Hostmask \"" + hostmask + "\" is not granted role \"" + role + "\"");
            case E_NOSUCHKEY:
                throw new IllegalArgumentException("Hostmask \"" + hostmask + "\" is not granted any roles");
        }
    }

    private synchronized void revokePermissionFromRole(String role, String permission) throws IllegalArgumentException {
        if (role.equals("*")) {
            throw new IllegalArgumentException("The pseudo-role \"*\" cannot be modified.");
        }

        switch (removeFromMultimap(rolesToPermissions, role, permission)) {
            case E_NOSUCHVALUE:
                throw new IllegalArgumentException("Role \"" + role + "\" is not granted permission \"" + permission + "\"");
            case E_NOSUCHKEY:
                throw new IllegalArgumentException("Role \"" + role + "\" does not exist (grant it permissions to create it)");
        }
    }

    private synchronized void revokeRoleFromAllHostmasks(String role) {
        for (String hostmask : (new HashSet<>(hostmasksToRoles.keySet()))) {
            try {
                revokeRoleFromHostmask(hostmask, role);
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }

    private synchronized void revokePermissionFromAllRoles(String permission) {
        for (String role : (new HashSet<>(rolesToPermissions.keySet()))) {
            try {
                revokePermissionFromRole(role, permission);
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }

    private synchronized boolean deleteHostmaskFromMap(String hostmask) {
        return hostmasksToRoles.remove(hostmask) != null;
    }

    private synchronized boolean deleteRoleFromMap(String role) throws IllegalArgumentException {
        if(role.equals("*")) {
            throw new IllegalArgumentException("The \"*\" pseudo-role cannot be deleted");
        }

        revokeRoleFromAllHostmasks(role);
        return rolesToPermissions.remove(role) != null;
    }

    private void sync() {
        pm.setMultimap("hostmasksToRoles", hostmasksToRoles);
        pm.setMultimap("rolesToPermissions", rolesToPermissions);
        pm.sync();
    }

    private synchronized Map<String, Set<String>> cloneMultimap(Map<String, Set<String>> multimap) {
        return new HashMap<>(multimap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new HashSet<>(e.getValue()))));
    }

    private synchronized boolean addToMultimap(Map<String, Set<String>> multimap, String key, String value) {
        Set<String> values;

        if (multimap.containsKey(key)) {
            values = multimap.get(key);
            if (values.contains(value)) {
                return false; // Already contains this key->value
            }
            values.add(value);
        } else {
            values = new HashSet<>();
            values.add(value);
            multimap.put(key, values);
        }

        return true;
    }

    private synchronized RemoveFromMultimapResults removeFromMultimap(Map<String, Set<String>> multimap, String key, String value) {
        if (multimap.containsKey(key)) {
            Set<String> values = multimap.get(key);
            if (values.contains(value)) {
                values.remove(value);
                if (values.isEmpty()) {
                    multimap.remove(key);
                }
            } else {
                return RemoveFromMultimapResults.E_NOSUCHVALUE;
            }
        } else {
            return RemoveFromMultimapResults.E_NOSUCHKEY;
        }

        return RemoveFromMultimapResults.SUCCESS;
    }

    private synchronized Set<String> getAllRoles() {
        return Stream.concat(hostmasksToRoles.values().stream()
                .flatMap(Set::stream),
                rolesToPermissions.keySet().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public CommandListener<T> getListener() {
        return listener;
    }

    /* Generics to right of them,
    Generics to left of them,
    Generics in front of them
    Volley'd and thunder'd;
    Storm'd at with shot and shell,
    Boldly they rode and well,
    Into the jaws of Death,
    Into the mouth of Hell
    Rode the six hundred. */

    private class BreadBasedAccessControlListener<PlsStopT extends PircBotX> extends CommandListener<PlsStopT> {
        private static final String COMMAND_ACL = "acl";
        private static final String COMMAND_MYPERMS = "myperms";
        private final Set<String> COMMANDS = ImmutableSet.of(COMMAND_ACL, COMMAND_MYPERMS);

        private static final String OPERATION_GRANT = "grant";
        private static final String OPERATION_REVOKE = "ReVoke";
        private static final String OPERATION_LIST = "list";
        private static final String OPERATION_REVOKE_ALL = "RevokeAll";
        private static final String OPERATION_DELETE = "delete";
        private final Set<String> OPERATIONS = ImmutableSet.of(OPERATION_GRANT, OPERATION_REVOKE, OPERATION_LIST, OPERATION_REVOKE_ALL, OPERATION_DELETE);

        private static final String OPERAND_ROLES = "roles";
        private static final String OPERAND_PERMISSIONS = "permissions";
        private final Set<String> OPERANDS = ImmutableSet.of(OPERAND_ROLES, OPERAND_PERMISSIONS);

        private static final String OPERAND_HOSTMASK = "hostmask";
        private final Set<String> OPERANDS_DELETE = ImmutableSet.of(OPERAND_ROLES, OPERAND_HOSTMASK);

        private static final String LIST_OPTION_AVAILABLE = "available";
        private final Set<String> LIST_OPTIONS = ImmutableSet.of(LIST_OPTION_AVAILABLE, "");

        @Override
        public Set<String> getSubCommands(GenericMessageEvent<PlsStopT> event, List<String> commands) {
            switch (commands.size()) {
                case 0:
                    return COMMANDS;
                case 1:
                    return OPERATIONS;
                case 2:
                    switch (commands.get(1)) {
                        case OPERATION_DELETE:
                            return OPERANDS_DELETE;
                        default:
                            return OPERANDS;
                    }
                case 3:
                    switch (commands.get(1)) {
                        case OPERATION_LIST:
                            return LIST_OPTIONS;
                        default:
                            return Collections.emptySet();
                    }
                default:
                    return Collections.emptySet();
            }
        }

        @Override
        public synchronized void handleCommand(GenericMessageEvent<PlsStopT> event, List<String> commands, String remainder) {
            if (commands.size() == 0) {
                return;
            }

            if (commands.get(0).equals(COMMAND_MYPERMS)) {
                event.respond("Your permissions are: " + getSetAsString(getPermissions(event)));
                return;
            }

            String target = event.getUser().getNick();
            OutputIRC outIRC = event.getBot().sendIRC();
            Consumer<String> message = s -> outIRC.message(target, s);

            if (!(event instanceof PrivateMessageEvent)) {
                event.respond("See private message.");
            }

            if (commands.size() < 3) {
                message.accept("Error: Too few or invalid arguments for the ?acl command.");
                showUsage(message);
                return;
            }

            if (!commands.get(1).equals(OPERATION_LIST) && !hasPermission(event, "acl")) {
                message.accept("No u! (You only have permission to run the \"?acl list\" commands.)");
                showUsage(message);
            }

            String[] args = remainder.split(" ");
            switch (commands.get(1)) {
                case OPERATION_LIST:
                    if (commands.get(3).equals("available")) {
                        switch (commands.get(2)) {
                            case OPERAND_ROLES:
                                message.accept("The following roles are available: " + getSetAsString(getAllRoles()));
                                message.accept("Remember that the \"*\" pseudo-role that grants ALL permissions unconditionally is always available.");
                                break;
                            case OPERAND_PERMISSIONS:
                                message.accept("I've seen the following permissions used, but note that this list may not include all available permissions: " + getSetAsString(seenPermissions));
                                message.accept("Remember that the \"*\" pseudo-permission that grants ALL permissions unconditionally is always available.");
                                break;
                        }
                    } else {
                        switch (commands.get(2)) {
                            case OPERAND_ROLES:
                                message.accept("The following is a mapping of hostmask regexes to the roles they are granted:");
                                message.accept("Format: regex => list of granted roles");
                                message.accept("----- BEGIN -----");
                                outputMultimap(hostmasksToRoles, message);
                                message.accept("------ END ------");
                                break;
                            case OPERAND_PERMISSIONS:
                                message.accept("The following is a mapping of roles to the permissions they are granted:");
                                message.accept("Format: role => list of granted permissions");
                                message.accept("----- BEGIN -----");
                                outputMultimap(rolesToPermissions, message);
                                message.accept("------ END ------");
                                break;
                        }
                    }
                    break;
                case OPERATION_GRANT:
                    if (args.length != 2) {
                        message.accept("Error: Too many or too few arguments for \"?acl grant\"");
                        showUsage(message);
                        return;
                    }
                    String grantee = args[0];
                    String granted = args[1];

                    switch (commands.get(2)) {
                        case OPERAND_ROLES:
                            try {
                                grantRoleToHostmask(grantee, granted);
                                message.accept("Grant complete!");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                        case OPERAND_PERMISSIONS:
                            try {
                                grantPermissionToRole(grantee, granted);
                                message.accept("Grant complete!");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                    }

                    sync();
                    break;
                case OPERATION_REVOKE:
                    if (args.length != 2) {
                        message.accept("Error: Too many or too few arguments for \"?acl revoke\"");
                        showUsage(message);
                        return;
                    }
                    String revokee = args[0];
                    String revoked = args[1];

                    switch (commands.get(2)) {
                        case OPERAND_ROLES:
                            try {
                                revokeRoleFromHostmask(revokee, revoked);
                                message.accept("Revocation complete.");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                        case OPERAND_PERMISSIONS:
                            try {
                                revokePermissionFromRole(revokee, revoked);
                                message.accept("Revocation complete.");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                    }

                    sync();
                    break;
                case OPERATION_REVOKE_ALL:
                    if (args.length != 1) {
                        message.accept("Error: Too many arguments for \"?acl revokeAll\"");
                        showUsage(message);
                        return;
                    }
                    String revokeAll = args[0];

                    switch (commands.get(2)) {
                        case OPERAND_ROLES:
                            try {
                                revokeRoleFromAllHostmasks(revokeAll);
                                message.accept("Revocation of all role instances complete.");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                        case OPERAND_PERMISSIONS:
                            try {
                                revokePermissionFromAllRoles(revokeAll);
                                message.accept("Revocation of all permission instances complete.");
                            } catch (IllegalArgumentException e) {
                                message.accept("Error occurred: " + e);
                            }
                            break;
                    }

                    sync();
                    break;
                case OPERATION_DELETE:
                    if (args.length != 1) {
                        message.accept("Error: Too many arguments for \"?acl delete\"");
                        showUsage(message);
                        return;
                    }
                    String toDelete = args[0];

                    switch (commands.get(2)) {
                        case OPERAND_HOSTMASK:
                            if (deleteHostmaskFromMap(toDelete)) {
                                message.accept("Deletion of hostmask from ACL complete.");
                            } else {
                                message.accept("Deletion failed (no such hostmask in map) - did you mistype the hostmask?");
                            }
                            break;
                        case OPERAND_ROLES:
                            try {
                                if (deleteRoleFromMap(toDelete)) {
                                    message.accept("Deletion of role from ACL complete.");
                                } else {
                                    message.accept("Deletion failed (no such role in map) - did you mistype the role?");
                                }
                            } catch (IllegalArgumentException e) {
                                message.accept("Deletion failed: " + e);
                            }
                            break;
                    }

                    sync();
                    break;
            }
        }

        private void showUsage(Consumer<String> message) {
            message.accept("Usage: ?acl <grant|revoke> roles [hostmask] [role]");
            message.accept("Usage: ?acl <grant|revoke> permissions [role] [permission]");
            message.accept("Usage: ?acl revokeAll roles [role]");
            message.accept("Usage: ?acl revokeAll permissions [permission]");
            message.accept("Usage: ?acl delete hostmask [hostmask]");
            message.accept("Usage: ?acl delete role [role]");
            message.accept("Usage: ?acl list <roles|permissions> {available}");
            message.accept(" Note: For \"acl list\", specifying the \"available\" flag will show all available roles or permissions; otherwise, it will show the mapping of hostmasks to roles or roles to permissions.");
        }

        private synchronized String getSetAsString(Set<String> set) {
            if (set.isEmpty()) {
                return "(none)";
            } else {
                return set.stream().collect(Collectors.joining(", "));
            }
        }

        private synchronized void outputMultimap(Map<String, Set<String>> multimap, Consumer<String> message) {
            for (Entry<String, Set<String>> entry : multimap.entrySet()) {
                String key = entry.getKey();
                String values = getSetAsString(entry.getValue());

                message.accept(key + " => " + values);
            }
        }
    }
}
