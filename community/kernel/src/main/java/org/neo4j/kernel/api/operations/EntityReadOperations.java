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
package org.neo4j.kernel.api.operations;

import java.util.Iterator;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.api.properties.SafeProperty;
import org.neo4j.kernel.impl.api.PrimitiveLongIterator;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;

public interface EntityReadOperations
{
    // Currently, of course, most relevant operations here are still in the old core API implementation.

    /**
     *
     * @param labelId the label id of the label that returned nodes are guaranteed to have
     * @return ids of all nodes that have the given label
     */
    PrimitiveLongIterator nodesGetForLabel( Statement state, long labelId );

    /**
     * Returns an iterable with the matched nodes.
     *
     * @throws IndexNotFoundKernelException if no such index found.
     */
    PrimitiveLongIterator nodesGetFromIndexLookup( Statement state, IndexDescriptor index, Object value ) throws IndexNotFoundKernelException;

    /**
     * Checks if a node is labeled with a certain label or not. Returns
     * {@code true} if the node is labeled with the label, otherwise {@code false.}
     * Label ids are retrieved from {@link KeyWriteOperations#labelGetOrCreateForName(org.neo4j.kernel.api.Statement, String)} or
     * {@link KeyReadOperations#labelGetForName(org.neo4j.kernel.api.Statement, String)}.
     */
    boolean nodeHasLabel( Statement state, long nodeId, long labelId ) throws EntityNotFoundException;

    /**
     * Returns all labels set on node with id {@code nodeId}.
     * If the node has no labels an empty {@link Iterable} will be returned.
     */
    PrimitiveLongIterator nodeGetLabels( Statement state, long nodeId ) throws EntityNotFoundException;

    Property nodeGetProperty( Statement state, long nodeId, long propertyKeyId ) throws EntityNotFoundException;

    Property relationshipGetProperty( Statement state, long relationshipId, long propertyKeyId )
            throws EntityNotFoundException;
    
    Property graphGetProperty( Statement state, long propertyKeyId );
    
    // TODO: decide if this should be replaced by nodeGetAllProperties()
    /** Return all property keys associated with a node. */
    PrimitiveLongIterator nodeGetPropertyKeys( Statement state, long nodeId ) throws EntityNotFoundException;

    Iterator<SafeProperty> nodeGetAllProperties( Statement state, long nodeId ) throws EntityNotFoundException;

    // TODO: decide if this should be replaced by relationshipGetAllProperties()
    /** Return all property keys associated with a relationship. */
    PrimitiveLongIterator relationshipGetPropertyKeys( Statement state, long relationshipId ) throws EntityNotFoundException;

    Iterator<SafeProperty> relationshipGetAllProperties( Statement state, long relationshipId ) throws EntityNotFoundException;

    // TODO: decide if this should be replaced by relationshipGetAllProperties()
    /** Return all property keys associated with a relationship. */
    PrimitiveLongIterator graphGetPropertyKeys( Statement state );

    Iterator<SafeProperty> graphGetAllProperties( Statement state );
}
