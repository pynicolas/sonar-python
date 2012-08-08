/*
 * Sonar Python Plugin
 * Copyright (C) 2011 SonarSource and Waleri Enns
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.python.pylint;

import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.command.Command;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.api.utils.command.StreamConsumer;
import org.sonar.plugins.python.PythonPlugin;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PylintViolationsAnalyzer {

  private static final String FALLBACK_PYLINT = "pylint";
  private static final String[] ARGS = {"-i", "y", "-f", "parseable", "-r", "n"};
  private static final Pattern PATTERN = Pattern.compile("([^:]+):([0-9]+): \\[(.*)\\] (.*)");

  private String pylint = null;
  private String pylintConfigParam = null;

  PylintViolationsAnalyzer(String pylintPath, String pylintConfigPath) {
    pylint = FALLBACK_PYLINT;
    if (pylintPath != null) {
      if (!new File(pylintPath).exists()) {
        throw new SonarException("Cannot find the pylint executable: " + pylintPath);
      }
      pylint = pylintPath;
    }

    if (pylintConfigPath != null) {
      if (!new File(pylintConfigPath).exists()) {
        throw new SonarException("Cannot find the pylint configuration file: " + pylintConfigPath);
      }
      pylintConfigParam = " --rcfile=" + pylintConfigPath;
    }
  }

  public List<Issue> analyze(String path) {
    Command command = Command.create(pylint).addArguments(ARGS).addArgument(path);

    if (pylintConfigParam != null) {
      command.addArgument(pylintConfigParam);
    }

    PythonPlugin.LOG.debug("Calling command: '{}'", command.toString());

    long timeoutMS = 300000; // =5min
    MyStreamConsumer stdOut = new MyStreamConsumer();
    MyStreamConsumer stdErr = new MyStreamConsumer();
    CommandExecutor.create().execute(command, stdOut, stdErr, timeoutMS);

    return parseOutput(stdOut.getData());
  }

  protected List<Issue> parseOutput(List<String> lines) {
    // Parse the output of pylint. Example of the format:
    //
    // complexity/code_chunks.py:62: [W0104, list_compr] Statement seems to have no effect
    // complexity/code_chunks.py:64: [C0111, list_compr_filter] Missing docstring
    // ...

    List<Issue> issues = new LinkedList<Issue>();

    int linenr;
    String filename = null;
    String ruleid = null;
    String objname = null;
    String descr = null;

    if (!lines.isEmpty()) {
      for (String line : lines) {
        Matcher m = PATTERN.matcher(line);
        if (m.matches() && m.groupCount() == 4) {
          filename = m.group(1);
          linenr = Integer.valueOf(m.group(2));
          String[] parts = m.group(3).split(",");
          ruleid = parts[0].trim();
          if (parts.length == 2) {
            objname = parts[1].trim();
          }

          descr = m.group(4);
          issues.add(new Issue(filename, linenr, ruleid, objname, descr));
        }
      }
    }

    return issues;
  }

  private static class MyStreamConsumer implements StreamConsumer {
    private List<String> data = new LinkedList<String>();

    public void consumeLine(String line) {
      data.add(line);
    }

    public List<String> getData() {
      return data;
    }
  }

}
