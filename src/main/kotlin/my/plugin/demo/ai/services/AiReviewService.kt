package my.plugin.demo.ai.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import my.plugin.demo.ai.interfaces.AiReviewInterface
import my.plugin.demo.ai.interfaces.ContextLoader
import my.plugin.demo.ai.interfaces.FileReviewResult

@Service(Service.Level.PROJECT)
class AiReviewService(
    private val project: Project,
    val coroutineScope: CoroutineScope
) {

    private lateinit var aiCodeReviewAssistant: AiReviewInterface
    private lateinit var embeddingStore: EmbeddingStore<TextSegment>
    private lateinit var contentRetriever: EmbeddingStoreContentRetriever

    suspend fun performReviewWithProgress(
        files: List<VirtualFile>
    ): Map<VirtualFile, FileReviewResult> {
        return withBackgroundProgress(project, "AI Code Review in Progress...") {
            initialize(files)
            review(files)
        }
    }

    private suspend fun initialize(filesToReview: List<VirtualFile>) {
        prepareRag(filesToReview)
        initAssistant()
    }

    /**
     * Launches a coroutine for each file to perform AI code review concurrently.
     *
     * @param files List of [VirtualFile]s to review.
     * @return A map pairing each original [VirtualFile] with its code-reviewed content as a [String].
     */
    private suspend fun review(files: List<VirtualFile>): Map<VirtualFile, FileReviewResult> = coroutineScope {
        val result = files.map { file ->
            async {
                val reviewResult = withContext(Dispatchers.IO) {
                    aiCodeReviewAssistant.review(VfsUtil.loadText(file), file.extension)
                }
                file to reviewResult
            }
        }.awaitAll().toMap()
        result
    }

    /**
     * Initializes the AI code review assistant.
     *
     * This function configures and builds an instance of [AiReviewInterface] using the [AiServices] builder.
     * It sets the chat model, content retriever used for RAG, and assigns the resulting assistant to [aiCodeReviewAssistant].
     *
     * This method should be called before attempting to use [aiCodeReviewAssistant] to ensure it is properly initialized.
     */
    private fun initAssistant() {
        aiCodeReviewAssistant = AiServices.builder(AiReviewInterface::class.java)
            .chatModel(getAiModel())
            .contentRetriever(contentRetriever)
            .build()
    }

    /**
     * Constructs and returns an instance of [OpenAiChatModel] configured for GPT-4o Mini.
     *
     * This function builds an [OpenAiChatModel] using the specified base URL, API key, and model name.
     * The model used is `gpt-4o-mini`, OpenAI's cost-efficient small model that supports both text and vision inputs,
     * with a context window of up to 128,000 tokens and support for up to 16,000 output tokens per request.
     *
     * **Note:** For proof-of-concept purposes, this implementation uses the demo API key provided in the LangChain4j documentation.
     * This key is intended for demonstration only and should not be used in production environments.
     *
     * @return a configured [OpenAiChatModel] instance for GPT-4o Mini, or `null` if construction fails.
     */
    private fun getAiModel(): OpenAiChatModel? {
        val openAiChatModel = OpenAiChatModel.builder()
            .baseUrl(DEMO_BASE_URL)
            .apiKey(AI_API_KEY)
            .modelName(GPT_4O_MINI_MODEL)
            .build()
        return openAiChatModel
    }


    /**
     * Prepares the basic Retrieval-Augmented Generation (RAG) environment.
     *
     * This function loads `.txt` guideline documents for file types created by the user from [GUIDELINES_PATH] in the file system,
     * then splits them into text segments of approximately 50 tokens each for more effective retrieval.
     *
     * For demonstration purposes, an in-memory embedding store is used to hold the vector representations of these segments.
     * **Important:** This approach is not suitable for production. For real-world applications, replace the in-memory store
     * with a scalable vector database such as Cassandra, Pinecone, or similar technologies.
     *
     * The ingested guidelines enable the assistant to retrieve relevant content based on semantic similarity.
     * The [contentRetriever] is initialized from the populated embedding store and is configured with a minimum similarity score
     * threshold of [MINIMUM_SIMILARITY_SCORE]. Only guideline segments with a similarity score of at least [MINIMUM_SIMILARITY_SCORE] will be retrieved, helping to filter out
     * less relevant results.
     *
     * @see EmbeddingStoreContentRetriever
     */
    private suspend fun prepareRag(filesToReview: List<VirtualFile>) {

        val contextDocuments = prepareContextDocuments(project, filesToReview)
        embeddingStore = InMemoryEmbeddingStore() // Not suitable for production, demonstration purpose only!

        val embeddingModel = createEmbeddingModel()
        createEmbeddingStoreIngestor(embeddingModel).ingest(contextDocuments)

        contentRetriever = EmbeddingStoreContentRetriever.builder()
            .minScore(MINIMUM_SIMILARITY_SCORE)
            .maxResults(MAXIMUM_RESULTS)
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .build()
    }

    private suspend fun prepareContextDocuments(project: Project, filesToReview: List<VirtualFile>): List<Document> {
        val contextLoaders = ExtensionPointName.create<ContextLoader>("my.plugin.demo.contextLoader").extensionList
        return withContext(Dispatchers.IO) {
            val additionalDocuments = contextLoaders.flatMap { contextLoader ->
                contextLoader.loadDocuments(project, filesToReview)
            }
            val guidelines = FileSystemDocumentLoader.loadDocuments(GUIDELINES_PATH)

            guidelines.plus(additionalDocuments)
        }
    }

    private fun createEmbeddingStoreIngestor(embeddingModel: OpenAiEmbeddingModel): EmbeddingStoreIngestor =
        EmbeddingStoreIngestor.builder()
            .documentSplitter(DocumentSplitters.recursive(MAX_SEGMENT_SIZE_CHAR, 0))
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .build()

    private fun createEmbeddingModel(): OpenAiEmbeddingModel = OpenAiEmbeddingModel.builder()
        .baseUrl(DEMO_BASE_URL)
        .apiKey(AI_API_KEY)
        .modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL)
        .build()


    /**
     * Proof-of-concept only!
     *
     * Should be moved to plugin settings.
     */
    companion object {
        const val MAX_SEGMENT_SIZE_CHAR = 320
        const val MINIMUM_SIMILARITY_SCORE = 0.3
        const val MAXIMUM_RESULTS = 15

        // Absolute path to guidelines folder. Proof-of-concept only. In production user should specify location of guidelines in plugin settings or add them directly in UI.
        const val GUIDELINES_PATH: String = ""
        const val GPT_4O_MINI_MODEL = "gpt-4o-mini"
        const val DEMO_BASE_URL = "http://langchain4j.dev/demo/openai/v1"
        const val AI_API_KEY = "demo"
    }
}