package dev.langchain4j.store.embedding.pinecone;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreWithFilteringIT;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThan;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static dev.langchain4j.internal.Utils.randomUUID;

@EnabledIfEnvironmentVariable(named = "PINECONE_API_KEY", matches = ".+")
class PineconeEmbeddingStoreIT extends EmbeddingStoreWithFilteringIT {

    EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
            .apiKey(System.getenv("PINECONE_API_KEY"))
            .index("test")
            .nameSpace(randomUUID())
            .createIndex(PineconeServerlessIndexConfig.builder()
                    .cloud("AWS")
                    .region("us-east-1")
                    .dimension(embeddingModel.dimension())
                    .build())
            .build();

    @AfterEach
    protected void clear() {
        embeddingStore.removeAll();
    }

    @Override
    protected EmbeddingStore<TextSegment> embeddingStore() {
        return embeddingStore;
    }

    @Override
    protected EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    @Override
    @SneakyThrows
    protected void awaitUntilPersisted() {
        Thread.sleep(6000);
    }

    @ParameterizedTest
    @MethodSource("should_filter_by_metadata")
    protected void should_filter_by_metadata(Filter metadataFilter,
                                             List<Metadata> matchingMetadatas,
                                             List<Metadata> notMatchingMetadatas) {
        super.should_filter_by_metadata(metadataFilter, matchingMetadatas, notMatchingMetadatas);
    }

    // in pinecone, compare filter only works with numbers
    protected static Stream<Arguments> should_filter_by_metadata() {
        return EmbeddingStoreWithFilteringIT.should_filter_by_metadata().filter(
                arguments -> {
                    Object o = arguments.get()[0];
                    if (o instanceof IsLessThan) {
                        return ((IsLessThan) o).comparisonValue() instanceof Number;
                    } else if (o instanceof IsLessThanOrEqualTo) {
                        return ((IsLessThanOrEqualTo) o).comparisonValue() instanceof Number;
                    } else if (o instanceof IsGreaterThan) {
                        return ((IsGreaterThan) o).comparisonValue() instanceof Number;
                    } else if (o instanceof IsGreaterThanOrEqualTo) {
                        return ((IsGreaterThanOrEqualTo) o).comparisonValue() instanceof Number;
                    } else {
                        return true;
                    }
                }
        );
    }
}