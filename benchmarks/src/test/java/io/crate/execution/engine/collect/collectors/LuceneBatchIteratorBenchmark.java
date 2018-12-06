/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.execution.engine.collect.collectors;

import io.crate.breaker.RamAccountingContext;
import io.crate.expression.reference.doc.lucene.CollectorContext;
import io.crate.expression.reference.doc.lucene.IntegerColumnReference;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.MMapDirectory;
import org.elasticsearch.common.breaker.NoopCircuitBreaker;
import org.elasticsearch.core.internal.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LuceneBatchIteratorBenchmark {

    public static final RamAccountingContext RAM_ACCOUNTING_CONTEXT = new RamAccountingContext("dummy", new NoopCircuitBreaker("dummy"));
    private CollectorContext collectorContext;
    private IndexSearcher indexSearcher;
    private List<IntegerColumnReference> columnRefs;
    private IndexWriter iw;
    private Path tempDirectory;

    @Setup
    public void createLuceneBatchIterator() throws Exception {
        tempDirectory = Files.createTempDirectory("lucene-batch-iterator-benchmark");
        iw = new IndexWriter(new MMapDirectory(tempDirectory), new IndexWriterConfig(new StandardAnalyzer()));
        String columnName = "x";
        for (int i = 0; i < 10_000_000; i++) {
            Document doc = new Document();
            doc.add(new NumericDocValuesField(columnName, i));
            iw.addDocument(doc);
        }
        iw.commit();
        iw.forceMerge(1, true);
        indexSearcher = new IndexSearcher(DirectoryReader.open(iw));
        IntegerColumnReference columnReference = new IntegerColumnReference(columnName);
        columnRefs = Collections.singletonList(columnReference);

        collectorContext = new CollectorContext(
            mappedFieldType -> null,
            new CollectorFieldsVisitor(0)
        );
    }

    @TearDown
    public void closeIndexWriter() throws Exception {
        iw.close();
        iw.getDirectory().close();
        IOUtils.rm(tempDirectory);
    }

    @Benchmark
    public void measureConsumeLuceneBatchIterator(Blackhole blackhole) throws Exception {
        LuceneBatchIterator it = new LuceneBatchIterator(
            indexSearcher,
            new MatchAllDocsQuery(),
            null,
            false,
            collectorContext,
            RAM_ACCOUNTING_CONTEXT,
            columnRefs,
            columnRefs
        );

        while (it.moveNext()) {
            blackhole.consume(it.currentElement().get(0));
        }
    }
}
