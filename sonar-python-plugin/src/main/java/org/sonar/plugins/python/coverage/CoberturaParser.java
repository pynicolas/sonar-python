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
package org.sonar.plugins.python.coverage;

import java.io.File;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.python.EmptyReportException;

public class CoberturaParser {

  private static final Logger LOG = Loggers.get(CoberturaParser.class);

  public void parseReport(File xmlFile, final Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {
    LOG.info("Parsing report '{}'", xmlFile);

    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        try {
          rootCursor.advance();
        } catch (com.ctc.wstx.exc.WstxEOFException eofExc) {
          LOG.debug("Unexpected end of file is encountered", eofExc);
          throw new EmptyReportException();
        }
        collectPackageMeasures(rootCursor.descendantElementCursor("package"), coverageData);
      }
    });
    parser.parse(xmlFile);
  }

  private void collectPackageMeasures(SMInputCursor pack, Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {
    while (pack.getNext() != null) {
      collectFileMeasures(pack.descendantElementCursor("class"), coverageData);
    }
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> coverageData) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");
      CoverageMeasuresBuilder builder = coverageData.get(fileName);
      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        coverageData.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      builder.setHits(lineId, Integer.parseInt(line.getAttrValue("hits")));

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId,  Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
