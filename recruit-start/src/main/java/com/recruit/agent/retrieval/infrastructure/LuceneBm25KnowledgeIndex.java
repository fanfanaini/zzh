package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.KnowledgeSparseIndex;
import com.recruit.agent.retrieval.domain.RetrievalFilters;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class LuceneBm25KnowledgeIndex implements KnowledgeSparseIndex, AutoCloseable {
    private static final String CHUNK_KEY = "chunk_key";
    private static final String DOCUMENT_ID = "document_id";
    private static final String DOCUMENT_ID_FILTER = "document_id_filter";
    private static final String CHUNK_ID = "chunk_id";
    private static final String SOURCE_NAME = "source_name";
    private static final String SOURCE_FILTER = "source_filter";
    private static final String CONTENT = "content";

    private final Analyzer analyzer;
    private final Directory directory;
    private final IndexWriter writer;
    private final BM25Similarity similarity;

    public LuceneBm25KnowledgeIndex(Bm25Properties properties) {
        try {
            this.analyzer = new SmartChineseAnalyzer();
            this.directory = new ByteBuffersDirectory();
            this.writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            this.similarity = new BM25Similarity(properties.getK1(), properties.getB());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize Lucene BM25 index", exception);
        }
    }

    @Override
    public synchronized void rebuild(List<KnowledgeChunk> chunks) {
        try {
            writer.deleteAll();
            if (chunks != null) {
                for (KnowledgeChunk chunk : chunks) {
                    writer.addDocument(toDocument(chunk));
                }
            }
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to rebuild Lucene BM25 index", exception);
        }
    }

    @Override
    public synchronized void upsertDocument(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document chunks must not be empty");
        }
        Long documentId = chunks.get(0).documentId();
        validateDocument(chunks, documentId);
        try {
            writer.deleteDocuments(LongPoint.newExactQuery(DOCUMENT_ID_FILTER, documentId));
            for (KnowledgeChunk chunk : chunks) {
                writer.addDocument(toDocument(chunk));
            }
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to update Lucene BM25 index", exception);
        }
    }

    @Override
    public synchronized void deleteDocument(Long documentId) {
        if (documentId == null) {
            return;
        }
        try {
            writer.deleteDocuments(LongPoint.newExactQuery(DOCUMENT_ID_FILTER, documentId));
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete from Lucene BM25 index", exception);
        }
    }

    @Override
    public synchronized List<RetrievalHit> search(String query, RetrievalRequest request) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        try {
            writer.flush();
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                searcher.setSimilarity(similarity);
                TopDocs topDocs = searcher.search(buildQuery(query, request.filters()), request.candidateLimit());
                List<RetrievalHit> results = new ArrayList<>(topDocs.scoreDocs.length);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.storedFields().document(scoreDoc.doc);
                    results.add(new RetrievalHit(
                            document.getField(DOCUMENT_ID).numericValue().longValue(),
                            document.getField(CHUNK_ID).numericValue().longValue(),
                            document.get(SOURCE_NAME),
                            document.get(CONTENT),
                            scoreDoc.score
                    ));
                }
                return List.copyOf(results);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Lucene BM25 search failed", exception);
        }
    }

    private Query buildQuery(String query, RetrievalFilters filters) throws IOException {
        BooleanQuery.Builder root = new BooleanQuery.Builder();
        root.add(textQuery(query), BooleanClause.Occur.MUST);
        if (filters.hasDocumentIds()) {
            BooleanQuery.Builder ids = new BooleanQuery.Builder();
            filters.documentIds().forEach(id ->
                    ids.add(LongPoint.newExactQuery(DOCUMENT_ID_FILTER, id), BooleanClause.Occur.SHOULD));
            root.add(ids.build(), BooleanClause.Occur.FILTER);
        }
        if (filters.hasSourceNames()) {
            BooleanQuery.Builder sources = new BooleanQuery.Builder();
            filters.sourceNames().forEach(source ->
                    sources.add(new TermQuery(new Term(SOURCE_FILTER, source)), BooleanClause.Occur.SHOULD));
            root.add(sources.build(), BooleanClause.Occur.FILTER);
        }
        return root.build();
    }

    private Query textQuery(String query) throws IOException {
        BooleanQuery.Builder terms = new BooleanQuery.Builder();
        int count = 0;
        try (TokenStream stream = analyzer.tokenStream(CONTENT, query)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                terms.add(new TermQuery(new Term(CONTENT, term.toString())), BooleanClause.Occur.SHOULD);
                count++;
            }
            stream.end();
        }
        return count == 0 ? new MatchNoDocsQuery() : terms.build();
    }

    private Document toDocument(KnowledgeChunk chunk) {
        Document document = new Document();
        document.add(new StringField(CHUNK_KEY, chunk.chunkId().toString(), Field.Store.NO));
        document.add(new LongPoint(DOCUMENT_ID_FILTER, chunk.documentId()));
        document.add(new StoredField(DOCUMENT_ID, chunk.documentId()));
        document.add(new StoredField(CHUNK_ID, chunk.chunkId()));
        document.add(new StringField(SOURCE_FILTER, chunk.sourceName(), Field.Store.NO));
        document.add(new StoredField(SOURCE_NAME, chunk.sourceName()));
        document.add(new TextField(CONTENT, chunk.content(), Field.Store.YES));
        return document;
    }

    private void validateDocument(List<KnowledgeChunk> chunks, Long documentId) {
        if (chunks.stream().anyMatch(chunk -> !documentId.equals(chunk.documentId()))) {
            throw new IllegalArgumentException("All chunks must belong to the same document");
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.close();
            directory.close();
            analyzer.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to close Lucene BM25 index", exception);
        }
    }
}
