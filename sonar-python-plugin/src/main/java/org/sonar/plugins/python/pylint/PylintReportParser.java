/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.python.pylint;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class PylintReportParser {
  private static final Pattern PATTERN = Pattern.compile("(.+):([0-9]+): \\[(.*)\\] (.*)");
  private static final Logger LOG = Loggers.get(PylintReportParser.class);

  // Pylint 0.24 brings a nasty reidentifying of some rules...
  // To avoid burdening of users with rule clones we map the ids.
  // This workaround can die as soon as pylints <= 0.23.X become obsolete.
  private static final Map<String, String> ID_MAP = ImmutableMap.<String, String> builder()
      .put("E9900", "E1300")
      .put("E9901", "E1301")
      .put("E9902", "E1302")
      .put("E9903", "E1303")
      .put("E9904", "E1304")
      .put("E9905", "E1305")
      .put("E9906", "E1306")
      .put("W6501", "W1201")
      .put("W9900", "W1300")
      .put("W9901", "W1301")
      .build();

  public Issue parseLine(String line) {
    // Parse the output of pylint. Example of the format:
    //
    // complexity/code_chunks.py:62: [W0104, list_compr] Statement seems to have no effect
    // complexity/code_chunks.py:64: [C0111, list_compr_filter] Missing docstring
    // ...

    Issue issue = null;

    int linenr;
    String ruleid = null;
    String objname = null;
    String descr = null;
    String filename = null;

    if (line.length() > 0) {
      if (!isDetail(line)) {
        Matcher m = PATTERN.matcher(line);
        if (m.matches() && m.groupCount() == 4) {
          filename = m.group(1);
          linenr = Integer.valueOf(m.group(2));
          String[] parts = m.group(3).split(",");

          ruleid = ruleId(parts[0].trim());

          if (parts.length == 2) {
            objname = parts[1].trim();
          } else {
            objname = "";
          }

          descr = m.group(4);
          issue = new Issue(filename, linenr, ruleid, objname, descr);
        } else {
          LOG.debug("Cannot parse the line: {}", line);
        }
      } else {
        LOG.trace("Classifying as detail and ignoring line '{}'", line);
      }
    }

    return issue;
  }

  private String ruleId(String ruleAndMessageIds) {
    String ruleid = ruleAndMessageIds;
    int parenthesisIndex = ruleid.indexOf('(');
    if (parenthesisIndex > -1) {
      ruleid = ruleid.substring(0, parenthesisIndex);
    }
    if (ID_MAP.containsKey(ruleid)) {
      ruleid = ID_MAP.get(ruleid);
    }
    return ruleid;
  }

  private boolean isDetail(String line) {
    char first = line.charAt(0);
    return first == ' ' || first == '\t' || first == '\n';
  }
}
