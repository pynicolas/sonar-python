/*
 * SonarQube Python Plugin
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
package org.sonar.python.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.python.api.PythonGrammar;
import org.sonar.python.api.PythonTokenType;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.ast.AstSelect;

import java.util.List;

@Rule(
    key = NeedlessPassCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    name = "\"pass\" should not be used needlessly",
    tags = Tags.UNUSED
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class NeedlessPassCheck extends SquidCheck<Grammar> {

  public static final String CHECK_KEY = "S2772";

  private static final String MESSAGE = "Remove this unneeded \"pass\".";

  @Override
  public void init() {
    subscribeTo(PythonGrammar.PASS_STMT);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode suite = node.getFirstAncestor(PythonGrammar.SUITE);
    List<AstNode> statements = suite.getChildren(PythonGrammar.STATEMENT);
    if (statements.size() > 1) {
      if (!docstringException(statements)) {
        raiseIssue(node);
      }
    } else {
      visitOneOrZeroStatement(node, suite, statements.size());
    }
  }

  private boolean docstringException(List<AstNode> statements) {
    return statements.size() == 2 && statements.get(0).getToken().getType().equals(PythonTokenType.STRING);
  }

  private void visitOneOrZeroStatement(AstNode node, AstNode suite, int statementNumber) {
    AstSelect simpleStatements;
    if (statementNumber == 1) {
      simpleStatements = suite.select()
          .children(PythonGrammar.STATEMENT)
          .children(PythonGrammar.STMT_LIST)
          .children(PythonGrammar.SIMPLE_STMT);
    } else {
      simpleStatements = suite.select()
          .children(PythonGrammar.STMT_LIST)
          .children(PythonGrammar.SIMPLE_STMT);
    }
    if (simpleStatements.size() > 1) {
      raiseIssue(node);
    }
  }

  private void raiseIssue(AstNode node) {
    getContext().createLineViolation(this, MESSAGE, node);
  }

}
