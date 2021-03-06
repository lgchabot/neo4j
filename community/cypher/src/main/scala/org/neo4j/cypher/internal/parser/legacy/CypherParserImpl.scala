/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.parser.legacy

import org.neo4j.cypher._
import org.neo4j.cypher.internal.commands._
import org.neo4j.cypher.internal.{CypherParser, ReattachAliasedExpressions}
import org.neo4j.cypher.internal.parser.MarkOptionalNodes

class CypherParserImpl extends Base
with Index
with Constraint
with Unions
with QueryParser
with Expressions
with MatchClause
with CypherParser {
  @throws(classOf[SyntaxException])
  def parse(text: String): AbstractQuery = {
    parseAll(cypherQuery, text) match {
      case Success(r, q) =>
        val resultQuery: AbstractQuery = ReattachAliasedExpressions(r.setQueryText(text))

        // This should be removed once the parser understand explicit optional nodes
        MarkOptionalNodes(resultQuery)

      case NoSuccess(message, input) => {
        if (message.startsWith("INNER"))
          throw new SyntaxException(message.substring(5), text, input.offset)
        else
          throw new SyntaxException(message + """

Think we should have better error message here? Help us by sending this query to cypher@neo4j.org.

Thank you, the Neo4j Team.
""", text, input.offset)
      }
    }
  }

  def cypherQuery: Parser[AbstractQuery] = (indexOps | constraintOps | union | query) <~ opt(";")
}
