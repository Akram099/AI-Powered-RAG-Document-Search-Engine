package com.tech.spring_ai_rag_with_ollama.ingestion;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIngestionService implements CommandLineRunner {

    @Value("classpath:/pdf/spring-boot-reference.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Read PDF
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        // 2. Configure splitter (🔥 IMPORTANT FIX)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(150)              // smaller chunks
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        // 3. Split into chunks
        List<Document> documents = splitter.apply(reader.read());

        // 4. Filter problematic chunks (🔥 avoids 400 error)
        documents = documents.stream()
                .filter(doc -> doc.getText() != null)
                .filter(doc -> doc.getText().length() < 2000) // safety limit
                .toList();

        // 5. Debug (optional but useful)
        System.out.println("Total chunks: " + documents.size());
        documents.forEach(doc ->
                System.out.println("Chunk size: " + doc.getText().length())
        );

        // 6. Store in vector DB
        vectorStore.accept(documents);

        System.out.println("✅ Documents successfully ingested into vector store");
    }
}