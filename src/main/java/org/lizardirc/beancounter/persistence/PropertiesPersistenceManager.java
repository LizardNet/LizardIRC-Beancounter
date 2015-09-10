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
 * This is an open source project.  The source Git repositories, which you are
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

package org.lizardirc.beancounter.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import org.lizardirc.beancounter.utils.Bases;

public class PropertiesPersistenceManager implements PersistenceManager {
    private final PropertiesWrapper wrapper;
    private final List<String> namespace;

    public PropertiesPersistenceManager(Path path) {
        this.wrapper = new PropertiesWrapper(path);
        this.namespace = ImmutableList.of();
    }

    private PropertiesPersistenceManager(PropertiesWrapper wrapper, List<String> namespace) {
        this.wrapper = wrapper;
        this.namespace = namespace;
    }

    @Override
    public PersistenceManager getNamespace(String name) {
        List<String> newNamespace = ImmutableList.copyOf(qualify(name).iterator());
        return new PropertiesPersistenceManager(wrapper, newNamespace);
    }

    @Override
    public String get(String name) {
        return wrapper.get(qualify(name));
    }

    @Override
    public void set(String name, String value) {
        wrapper.set(qualify(name), value);
    }

    @Override
    public void sync() {
        wrapper.loadClean();
        wrapper.save();
    }

    private Stream<String> qualify(String name) {
        return Stream.concat(namespace.stream(), Stream.of(name));
    }

    private static class PropertiesWrapper {
        private final Path path;
        private final Properties properties = new Properties();
        private final Set<String> dirty = new HashSet<>();

        public PropertiesWrapper(Path path) {
            this.path = path;
            loadClean();
        }

        public String get(Stream<String> names) {
            return properties.getProperty(encode(names));
        }

        public void set(Stream<String> names, String value) {
            String encoded = encode(names);
            dirty.add(encoded);
            properties.setProperty(encoded, value);
        }

        public void loadClean() {
            Properties loaded = new Properties();
            try (InputStream is = Files.newInputStream(path)) {
                loaded.load(is);

                loaded.stringPropertyNames().forEach(prop -> {
                    if (!dirty.contains(prop)) {
                        properties.setProperty(prop, loaded.getProperty(prop));
                    }
                });
            } catch (NoSuchFileException e) {
                System.err.println("WARNING: Could not find state file " + path + " (NoSuchFileException). This is normal if this is the first time running the bot.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void save() {
            if (!dirty.isEmpty()) {
                try (OutputStream os = Files.newOutputStream(path)) {
                    properties.store(os, null);
                    dirty.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String encode(Stream<String> names) {
            return names
                .map(Bases::base64encode)
                .collect(Collectors.joining("."));
        }
    }
}
