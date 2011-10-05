/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Lead:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* -------------------------
 * ClosestFirstIterator.java
 * -------------------------
 * (C) Copyright 2003, by John V. Sichi and Contributors.
 *
 * Original Author:  John V. Sichi
 * Contributor(s):   Barak Naveh
 *
 * $Id: ClosestWithinRangeIterator.java,v 1.1 2004/08/13 06:40:03 mrdon Exp $
 *
 * Changes
 * -------
 * 02-Sep-2003 : Initial revision (JVS);
 * 31-Jan-2004 : Reparented and changed interface to parent class (BN);
 *
 */
package org.twdata.TW1606U.tw.graph;

import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.Graph;
import org._3pq.jgrapht.traverse.*;
import org._3pq.jgrapht.util.FibonacciHeap;

/**
 * A closest-first iterator for a directed or undirected graph. For this
 * iterator to work correctly the graph must not be modified during iteration.
 * Currently there are no means to ensure that, nor to fail-fast. The results
 * of such modifications are undefined.
 *
 * <p>
 * The metric for <i>closest</i> here is the path length from a start vertex.
 * Edge.getWeight() is summed to calculate path length. Negative edge weights
 * will result in an IllegalArgumentException.
 * </p>
 *
 * @author John V. Sichi
 *
 * @since Sep 2, 2003
 */
public class ClosestWithinRangeIterator extends CrossComponentIterator {
    /** Priority queue of fringe vertices. */
    private FibonacciHeap m_heap = new FibonacciHeap(  );
    private QueueEntry curEntry;
    private double maxDistance = -1;

    /**
     * Creates a new closest-first iterator for the specified graph.
     *
     * @param g the graph to be iterated.
     */
    /*public ClosestFirstIterator( Graph g ) {
        this( g, null );
    }
    */


    /**
     * Creates a new closest-first iterator for the specified graph. Iteration
     * will start at the specified start vertex and will be limited to the
     * connected component that includes that vertex. If the specified start
     * vertex is <code>null</code>, iteration will start at an arbitrary
     * vertex and will not be limited, that is, will be able to traverse all
     * the graph.
     *
     * @param g the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     */
    public ClosestWithinRangeIterator( Graph g, Object startVertex, double maxRange ) {
        super( g, startVertex );
        this.maxDistance = maxRange;
    }

    /**
     * Get the spanning tree edge reaching a vertex which has been seen already
     * in this traversal.  This edge is the last link in the shortest known
     * path between the start vertex and the requested vertex.  If the vertex
     * has already been visited, then it is truly the minimum spanning tree
     * edge; otherwise, it is the best candidate seen so far.
     *
     * @param vertex the spanned vertex.
     *
     * @return the spanning tree edge, or null if the vertex either has not
     *         been seen yet or is the start vertex.
     */
    public Edge getSpanningTreeEdge( Object vertex ) {
        QueueEntry entry = (QueueEntry) getSeenData( vertex );

        if( entry == null ) {
            return null;
        }

        return entry.m_spanningTreeEdge;
    }


    /**
     * @see org._3pq.jgrapht.traverse.CrossComponentIterator#isConnectedComponentExhausted()
     */
    protected boolean isConnectedComponentExhausted(  ) {
        return m_heap.size(  ) == 0;
    }


    /**
     * @see org._3pq.jgrapht.traverse.CrossComponentIterator#encounterVertex(java.lang.Object,
     *      org._3pq.jgrapht.Edge)
     */
    protected void encounterVertex( Object vertex, Edge edge ) {
        QueueEntry entry = createSeenData( vertex, edge );
        if (entry.getShortestPathLength() <= maxDistance) {
            putSeenData( vertex, entry );
            m_heap.insert( entry, entry.getShortestPathLength(  ) );
        }
    }


    /**
     * Override superclass.  When we see a vertex again, we need to see if the
     * new edge provides a shorter path than the old edge.
     *
     * @param vertex the vertex re-encountered
     * @param edge the edge via which the vertex was re-encountered
     */
    protected void encounterVertexAgain( Object vertex, Edge edge ) {
        QueueEntry entry = (QueueEntry) getSeenData( vertex );

        if( entry.m_frozen ) {
            // no improvement for this vertex possible
            return;
        }

        double candidatePathLength = calculatePathLength( vertex, edge );

        if( candidatePathLength < entry.getShortestPathLength(  ) ) {
            entry.m_spanningTreeEdge = edge;
            m_heap.decreaseKey( entry, candidatePathLength );
        }
    }


    /**
     * @see org._3pq.jgrapht.traverse.CrossComponentIterator#provideNextVertex()
     */
    protected Object provideNextVertex(  ) {
        QueueEntry entry = (QueueEntry) m_heap.removeMin(  );
        entry.m_frozen = true;
        curEntry = entry;

        return entry.m_vertex;
    }

    public double getCurrentDistance() {
        if (curEntry != null) {
            return curEntry.getShortestPathLength();
        }
        return -1;
    }


    private void assertNonNegativeEdge( Edge edge ) {
        if( edge.getWeight(  ) < 0 ) {
            throw new IllegalArgumentException(
                "negative edge weights not allowed" );
        }
    }


    /**
     * Determine path length to a vertex via an edge, using the path length for
     * the opposite vertex.
     *
     * @param vertex the vertex for which to calculate the path length.
     * @param edge the edge via which the path is being extended.
     *
     * @return calculated path length.
     */
    private double calculatePathLength( Object vertex, Edge edge ) {
        assertNonNegativeEdge( edge );

        Object     otherVertex = edge.oppositeVertex( vertex );
        QueueEntry otherEntry = (QueueEntry) getSeenData( otherVertex );

        return otherEntry.getShortestPathLength(  ) + edge.getWeight(  );
    }


    /**
     * The first time we see a vertex, make up a new queue entry for it.
     *
     * @param vertex a vertex which has just been encountered.
     * @param edge the edge via which the vertex was encountered.
     *
     * @return the new queue entry.
     */
    private QueueEntry createSeenData( Object vertex, Edge edge ) {
        double shortestPathLength;

        if( edge == null ) {
            shortestPathLength = 0;
        }
        else {
            shortestPathLength = calculatePathLength( vertex, edge );
        }

        QueueEntry entry = new QueueEntry( shortestPathLength );
        entry.m_vertex               = vertex;
        entry.m_spanningTreeEdge     = edge;

        return entry;
    }

    /**
     * Private data to associate with each entry in the priority queue.
     */
    private static class QueueEntry extends FibonacciHeap.Node {
        /** Best spanning tree edge to vertex seen so far. */
        Edge m_spanningTreeEdge;

        /** The vertex reached. */
        Object m_vertex;

        /** True once m_spanningTreeEdge is guaranteed to be the true minimum. */
        boolean m_frozen;

        QueueEntry( double key ) {
            super( key );
        }

        double getShortestPathLength(  ) {
            return getKey(  );
        }
    }
}