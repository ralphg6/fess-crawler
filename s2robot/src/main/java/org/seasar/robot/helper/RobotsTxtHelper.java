/*
 * Copyright 2004-2013 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.robot.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.seasar.framework.util.StringUtil;
import org.seasar.robot.Constants;
import org.seasar.robot.RobotSystemException;
import org.seasar.robot.entity.RobotsTxt;

public class RobotsTxtHelper {

    protected static final Pattern USER_AGENT = Pattern
        .compile("^User-agent:\\s*([^\\s]+)\\s*$");

    protected static final Pattern DISALLOW = Pattern
        .compile("^Disallow:\\s*([^\\s]*)\\s*$");

    protected static final Pattern ALLOW = Pattern
        .compile("^Allow:\\s*([^\\s]*)\\s*$");

    protected static final Pattern CRAWL_DELAY = Pattern
        .compile("^Crawl-delay:\\s*([^\\s]+)\\s*$");

    // TODO sitemaps

    public RobotsTxt parse(final String text) {
        final String[] lines = text.split("(\\r\\n)|\\r|\\n");
        return parse(java.util.Arrays.asList(lines));
    }

    public RobotsTxt parse(final InputStream stream) {
        return parse(stream, Constants.UTF_8);
    }

    public RobotsTxt parse(final InputStream stream, final String charsetName) {
        try {
            return parse(new InputStreamReader(stream, charsetName));
        } catch (final UnsupportedEncodingException e) {
            throw new RobotSystemException(e);
        }
    }

    public RobotsTxt parse(final Reader reader) {
        BufferedReader br;
        if (reader instanceof BufferedReader) {
            br = (BufferedReader) reader;
        } else {
            br = new BufferedReader(reader);
        }
        final List<String> lines = new ArrayList<String>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return parse(lines);
        } catch (final IOException e) {
            throw new RobotSystemException("Could not read from Reader.", e);
        }
    }

    protected RobotsTxt parse(final List<String> lines) {
        final RobotsTxt robotsTxt = new RobotsTxt();
        RobotsTxt.Directives currentDirectives = null;
        for (String line : lines) {
            line = stripComment(line);
            line = line.trim();
            String value;
            if (StringUtil.isEmpty(line)) {
                continue;
            } else if ((value = getValue(USER_AGENT, line)) != null) {
                final String userAgent = value.toLowerCase(Locale.ENGLISH);
                currentDirectives = robotsTxt.getDirectives(userAgent, null);
                if (currentDirectives == null) {
                    currentDirectives = new RobotsTxt.Directives();
                    robotsTxt.addDirectives(userAgent, currentDirectives);
                }
            } else if ((value = getValue(DISALLOW, line)) != null) {
                if (currentDirectives == null) {
                    continue; // the format of robots.txt is invalid
                }
                if (value.length() > 0) {
                    currentDirectives.addDisallow(value);
                }
            } else if ((value = getValue(ALLOW, line)) != null) {
                if (currentDirectives == null) {
                    continue; // the format of robots.txt is invalid
                }
                currentDirectives.addAllow(value);
            } else if ((value = getValue(CRAWL_DELAY, line)) != null) {
                if (currentDirectives == null) {
                    continue; // the format of robots.txt is invalid
                }
                try {
                    currentDirectives.setCrawlDelay(Math.max(
                        0,
                        Integer.parseInt(value)));
                } catch (final NumberFormatException e) {
                    continue; // the format of robots.txt is invalid
                }
            }
        }
        return robotsTxt;
    }

    protected String getValue(final Pattern pattern, final String line) {
        final Matcher m = pattern.matcher(line);
        if (m.matches() && m.groupCount() > 0) {
            return m.group(1);
        }
        return null;
    }

    protected String stripComment(final String line) {
        final int commentIndex = line.indexOf('#');
        if (commentIndex != -1) {
            return line.substring(0, commentIndex);
        }
        return line;
    }

}
