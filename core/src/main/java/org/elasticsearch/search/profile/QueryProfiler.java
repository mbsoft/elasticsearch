/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.profile;

import org.apache.lucene.search.Query;

import java.util.List;
import java.util.Objects;

/**
 * This class acts as a thread-local storage for profiling a query.  It also
 * builds a representation of the query tree which is built constructed
 * "online" as the weights are wrapped by ContextIndexSearcher.  This allows us
 * to know the relationship between nodes in tree without explicitly
 * walking the tree or pre-wrapping everything
 *
 * A Profiler is associated with every Search, not per Search-Request. E.g. a
 * request may execute two searches (query + global agg).  A Profiler just
 * represents one of those
 */
public final class QueryProfiler {

    private final InternalQueryProfileTree queryTree = new InternalQueryProfileTree();

    /**
     * The root Collector used in the search
     */
    private InternalProfileCollector collector;

    public QueryProfiler() {}

    /** Set the collector that is associated with this profiler. */
    public void setCollector(InternalProfileCollector collector) {
        if (this.collector != null) {
            throw new IllegalStateException("The collector can only be set once.");
        }
        this.collector = Objects.requireNonNull(collector);
    }

    /**
     * Get the {@link QueryProfileBreakdown} for the given query, potentially creating it if it did not exist.
     * This should only be used for queries that will be undergoing scoring. Do not use it to profile the
     * rewriting phase
     */
    public QueryProfileBreakdown getQueryBreakdown(Query query) {
        return queryTree.getQueryBreakdown(query);
    }

    /**
     * Begin timing the rewrite phase of a request.  All rewrites are accumulated together into a
     * single metric
     */
    public void startRewriteTime() {
        queryTree.startRewriteTime();
    }

    /**
     * Stop recording the current rewrite and add it's time to the total tally, returning the
     * cumulative time so far.
     *
     * @return cumulative rewrite time
     */
    public long stopAndAddRewriteTime() {
        return queryTree.stopAndAddRewriteTime();
    }

    /**
     * Removes the last (e.g. most recent) query on the stack.  This should only be called for scoring
     * queries, not rewritten queries
     */
    public void pollLastQuery() {
        queryTree.pollLast();
    }

    /**
     * @return a hierarchical representation of the profiled query tree
     */
    public List<ProfileResult> getQueryTree() {
        return queryTree.getQueryTree();
    }

    /**
     * @return total time taken to rewrite all queries in this profile
     */
    public long getRewriteTime() {
        return queryTree.getRewriteTime();
    }

    /**
     * Return the current root Collector for this search
     */
    public CollectorResult getCollector() {
        return collector.getCollectorTree();
    }


}
