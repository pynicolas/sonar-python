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
package org.sonar.python;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import java.io.File;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.squidbridge.SquidAstVisitor;

public class PreciseIssue {

  private final SquidAstVisitor<Grammar> check;
  private final File file;
  private final int startLine;
  private final int startColumn;
  private final int endLine;
  private final int endColumn;
  private final String message;

  public PreciseIssue(SquidAstVisitor<Grammar> check, File file, AstNode astNode, String message) {
    this.check = check;
    this.file = file;
    this.startLine = astNode.getToken().getLine();
    this.startColumn = astNode.getToken().getColumn();
    this.endLine = astNode.getLastToken().getLine();
    this.endColumn = astNode.getLastToken().getColumn() + astNode.getLastToken().getValue().length();
    this.message = message;
  }

  public void save(Checks<SquidAstVisitor<Grammar>> checks, SensorContext sensorContext) {
    RuleKey ruleKey = checks.ruleKey(check);
    FileSystem fs = sensorContext.fileSystem();
    InputFile inputFile = fs.inputFile(fs.predicates().hasPath(file.getPath()));
    NewIssue newIssue = sensorContext.newIssue().forRule(ruleKey);
    newIssue.at(
      newIssue.newLocation()
        .on(inputFile)
        .at(inputFile.newRange(startLine, startColumn, endLine, endColumn))
        .message(message));
    newIssue.save();
  }

}
