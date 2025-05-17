package my.plugin.demo.ai.interfaces

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V

interface AiReviewInterface {
    @SystemMessage("""
        You are an expert AI code reviewer.
        Your job is to analyze code submissions (in any programming language) for correctness, readability, maintainability, and adherence to best practices.
        Follow these rules:
        1. ALWAYS apply both COMMON and LANGUAGE-SPECIFIC INTERNAL guidelines
        2. When guidelines conflict, prioritize LANGUAGE-SPECIFIC ones
        3. Combine relevant parts from multiple guidelines
        
        Always tailor your review to the language, framework, and context provided. 
        Your goal is to help the author produce clean, robust, and professional code.
        
        Formatting rules:
        1. In JSON, do not escape the dollar sign (${'$'}).
        2. Only use valid JSON escapes: \\, \", \n, \r, \t, \b, \f, or \uXXXX.
        3. Do not wrap JSON in markdown (no json ...


    """)
    @UserMessage("""
        Perform code review of {{codeText}}
        Apply best fitting INTERNAL guidelines based on {{fileType}}.
        Return altered code with review changes and provide comments for performed changes.
        Each comment added by you to the provided code should start with prefix "AI Review:"
        Example of comment: "// AI Review: removed unnecessary code and replaced Hello World."
    """)
    fun review(@V("codeText") codeText: String, @V("fileType") fileType: String?) : FileReviewResult
}


data class FileReviewResult(
    val generalComment: String,
    val codeTextWithReviewChanges: String,
    val comments: List<Comment>,
    val usedInternalGuidelines: List<String>
)

data class Comment(
    val commentRelatedLine: Int,
    val commentText: String,
)