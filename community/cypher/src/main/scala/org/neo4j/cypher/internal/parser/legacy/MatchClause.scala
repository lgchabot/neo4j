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

import org.neo4j.cypher.internal.commands._
import expressions.{Identifier, Expression}
import collection.Map
import org.neo4j.cypher.internal.helpers.CastSupport._
import org.neo4j.cypher.internal.parser._
import org.neo4j.cypher.internal.commands.NamedPath
import org.neo4j.cypher.internal.parser.ParsedEntity
import org.neo4j.cypher.internal.commands.expressions.Literal
import org.neo4j.cypher.internal.parser.ParsedNamedPath
import org.neo4j.cypher.internal.parser.ParsedRelation
import org.neo4j.cypher.internal.commands.ShortestPath
import org.neo4j.cypher.internal.commands.values.KeyToken
import org.neo4j.cypher.SyntaxException

trait MatchClause extends Base with ParserPattern {
  def matching: Parser[(Seq[Pattern], Seq[NamedPath])] = MATCH ~> usePattern(matchTranslator) ^^ {
    case matches: Seq[Any] =>
      val namedPaths = sift[NamedPath](matches)

      val patterns = if (namedPaths.nonEmpty) {
        val namedPathMatches = namedPaths.flatMap(_.pathPattern).map(matchTranslator).reduce(_ ++ _).getValuesOr(throw new SyntaxException("wut"))
        val namedPathPatterns = sift[List[Pattern]](namedPathMatches).flatten ++ sift[Pattern](namedPathMatches)
        sift[List[Pattern]](matches).flatten ++ sift[Pattern](matches) ++ namedPathPatterns

      } else sift[List[Pattern]](matches).flatten ++ sift[Pattern](matches)

      (patterns.distinct, namedPaths)
  }


  type TransformType = (ParsedEntity, ParsedEntity, Map[String, Expression], (ParsedEntity, ParsedEntity) => Pattern) => Maybe[Pattern]

  private def successIfIdentifiers[T](left: ParsedEntity,
                                      right: ParsedEntity,
                                      relProps: Map[String, Expression],
                                      f: (ParsedEntity, ParsedEntity) => T): Maybe[T] = {
    def checkProps(props: Map[String, Expression]): Maybe[T] =
      if (props.nonEmpty)
        No(Seq("Properties on pattern elements are not allowed in MATCH."))
      else
        Yes(Seq())

    def checkExpressions(x: ParsedEntity): Maybe[T] = x.expression match {
      case _: Identifier                   => Yes(Seq())
      case Literal(_: KeyToken.Unresolved) => Yes(Seq())
      case e                               => No(Seq(s"MATCH end points have to be node identifiers - found: $e"))
    }

    val props: Maybe[T] = checkProps(left.props) ++ checkProps(right.props) ++ checkProps(relProps)
    val expressions = checkExpressions(left) ++ checkExpressions(right)

    (props ++ expressions).seqMap(s => Seq(f(left, right)))
  }

  def matchTranslator(abstractPattern: AbstractPattern): Maybe[Any] =
    matchTranslator(successIfIdentifiers, abstractPattern)

  def matchTranslator(transform: TransformType, abstractPattern: AbstractPattern): Maybe[Any] = {
    val f = matchFunction(transform)
    if (f.isDefinedAt(abstractPattern))
      f(abstractPattern)
    else
      No(Seq("failed to parse MATCH pattern"))
  }

  def matchFunction(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] =
    matchNamePath(transform) orElse
    matchRelation(transform) orElse
    matchVarLengthRelation(transform) orElse
    matchShortestPath(transform) orElse
    matchEntity(transform)

  def matchNamePath(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] = {
    case ParsedNamedPath(name, patterns) =>
      parsedPath(name, patterns, transform)
  }

  def matchRelation(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] = {
    case ParsedRelation(name, props, left, right, relType, dir, optional) =>
      transform(left, right, props, (l, r) =>
        RelatedTo(left = l.asSingleNode, right = r.asSingleNode, relName = name, relTypes = relType,
          direction = dir, optional = optional))
  }

  def matchVarLengthRelation(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] = {
    case ParsedVarLengthRelation(name, props, left, right, relType, dir, optional, min, max, relIterator) =>
      transform(left, right, props, (l, r) =>
        VarLengthRelatedTo(pathName = name, left = l.asSingleNode, right = r.asSingleNode, minHops = min, maxHops = max,
          relTypes = relType, direction = dir, relIterator = relIterator, optional = optional))
  }

  def matchShortestPath(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] = {
    case ParsedShortestPath(name, props, left, right, relType, dir, optional, max, single, relIterator) =>
      transform(left, right, props, (l, r) =>
        ShortestPath(pathName = name, left = l.asSingleNode, right = r.asSingleNode, relTypes = relType, dir = dir, maxDepth = max,
          optional = optional, single = single, relIterator = relIterator))

  }

  def matchEntity(transform: TransformType): PartialFunction[AbstractPattern, Maybe[Any]] = {
    case e:ParsedEntity => Yes(Seq(e.asSingleNode))
  }

  private def parsedPath(name: String, patterns: Seq[AbstractPattern], transform: TransformType): Maybe[NamedPath] = {
    val namedPathPatterns = patterns.map(matchTranslator(transform, _))
    val result = namedPathPatterns.reduce(_ ++ _)
    result.seqMap(p => Seq(NamedPath(name, patterns: _*)))
  }
}
