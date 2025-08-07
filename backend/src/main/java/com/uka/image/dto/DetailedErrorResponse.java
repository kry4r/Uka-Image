package com.uka.image.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed Error Response for comprehensive error logging and debugging
 * Provides complete error information including stack traces and API response data
 * 
 * @author Uka Team
 */
@Data
public class DetailedErrorResponse {
    
    // Basic error information
    private String errorCode;
    private String errorMessage;
    private String errorType;
    private LocalDateTime timestamp;
    private String requestPath;
    private String httpMethod;
    private String userAgent;
    private String clientIp;
    
    // Detailed error context
    private Map<String, Object> requestParameters;
    private Map<String, String> requestHeaders;
    private Object requestBody;
    
    // Exception details
    private String exceptionClass;
    private String exceptionMessage;
    private List<StackTraceInfo> stackTrace;
    private String rootCauseMessage;
    
    // API response details (for external API failures)
    private Integer apiResponseStatus;
    private Map<String, String> apiResponseHeaders;
    private String apiResponseBody;
    private Long apiResponseTime;
    
    // Search context (for search-related errors)
    private SearchErrorContext searchContext;
    
    // System context
    private SystemContext systemContext;
    
    // Debug information
    private Map<String, Object> debugInfo;
    private String correlationId;
    
    /**
     * Stack trace information for detailed debugging
     */
    @Data
    public static class StackTraceInfo {
        private String className;
        private String methodName;
        private String fileName;
        private Integer lineNumber;
        private boolean isNativeMethod;
        
        public static StackTraceInfo fromStackTraceElement(StackTraceElement element) {
            StackTraceInfo info = new StackTraceInfo();
            info.setClassName(element.getClassName());
            info.setMethodName(element.getMethodName());
            info.setFileName(element.getFileName());
            info.setLineNumber(element.getLineNumber());
            info.setNativeMethod(element.isNativeMethod());
            return info;
        }
    }
    
    /**
     * Search-specific error context
     */
    @Data
    public static class SearchErrorContext {
        private String searchQuery;
        private String searchType;
        private Integer totalImagesAnalyzed;
        private Long searchDuration;
        private String searchStrategy;
        private Map<String, Object> searchParameters;
        private List<String> processingSteps;
        private String failurePoint;
    }
    
    /**
     * System context information
     */
    @Data
    public static class SystemContext {
        private String javaVersion;
        private String springBootVersion;
        private Long availableMemory;
        private Long totalMemory;
        private Long usedMemory;
        private Integer activeThreads;
        private String operatingSystem;
        private String serverName;
        private Integer serverPort;
    }
    
    /**
     * Create a detailed error response from an exception
     * 
     * @param exception The exception that occurred
     * @param requestPath The request path where error occurred
     * @param httpMethod The HTTP method used
     * @return DetailedErrorResponse with comprehensive error information
     */
    public static DetailedErrorResponse fromException(Exception exception, String requestPath, String httpMethod) {
        DetailedErrorResponse response = new DetailedErrorResponse();
        
        // Basic error information
        response.setErrorCode("INTERNAL_ERROR");
        response.setErrorMessage(exception.getMessage());
        response.setErrorType(exception.getClass().getSimpleName());
        response.setTimestamp(LocalDateTime.now());
        response.setRequestPath(requestPath);
        response.setHttpMethod(httpMethod);
        
        // Exception details
        response.setExceptionClass(exception.getClass().getName());
        response.setExceptionMessage(exception.getMessage());
        
        // Stack trace
        List<StackTraceInfo> stackTrace = new java.util.ArrayList<>();
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.add(StackTraceInfo.fromStackTraceElement(element));
        }
        response.setStackTrace(stackTrace);
        
        // Root cause
        Throwable rootCause = getRootCause(exception);
        if (rootCause != null && rootCause != exception) {
            response.setRootCauseMessage(rootCause.getMessage());
        }
        
        // System context
        response.setSystemContext(createSystemContext());
        
        return response;
    }
    
    /**
     * Create a detailed error response for search failures
     * 
     * @param exception The search exception
     * @param searchQuery The search query that failed
     * @param searchContext Additional search context
     * @return DetailedErrorResponse with search-specific information
     */
    public static DetailedErrorResponse fromSearchException(Exception exception, String searchQuery, 
                                                           Map<String, Object> searchContext) {
        DetailedErrorResponse response = fromException(exception, "/ai-search/search", "GET");
        
        // Search-specific context
        SearchErrorContext searchErrorContext = new SearchErrorContext();
        searchErrorContext.setSearchQuery(searchQuery);
        searchErrorContext.setSearchType("METADATA_BASED");
        
        if (searchContext != null) {
            searchErrorContext.setTotalImagesAnalyzed((Integer) searchContext.get("totalImages"));
            searchErrorContext.setSearchDuration((Long) searchContext.get("searchDuration"));
            searchErrorContext.setSearchStrategy((String) searchContext.get("searchStrategy"));
            searchErrorContext.setSearchParameters((Map<String, Object>) searchContext.get("parameters"));
            searchErrorContext.setProcessingSteps((List<String>) searchContext.get("processingSteps"));
            searchErrorContext.setFailurePoint((String) searchContext.get("failurePoint"));
        }
        
        response.setSearchContext(searchErrorContext);
        response.setErrorCode("SEARCH_ERROR");
        
        return response;
    }
    
    /**
     * Create a detailed error response for API failures
     * 
     * @param exception The API exception
     * @param apiResponseStatus HTTP status from API
     * @param apiResponseBody Response body from API
     * @param apiResponseHeaders Response headers from API
     * @param responseTime API response time
     * @return DetailedErrorResponse with API-specific information
     */
    public static DetailedErrorResponse fromApiException(Exception exception, Integer apiResponseStatus,
                                                        String apiResponseBody, Map<String, String> apiResponseHeaders,
                                                        Long responseTime) {
        DetailedErrorResponse response = fromException(exception, "/ai-search/search", "GET");
        
        response.setErrorCode("API_ERROR");
        response.setApiResponseStatus(apiResponseStatus);
        response.setApiResponseBody(apiResponseBody);
        response.setApiResponseHeaders(apiResponseHeaders);
        response.setApiResponseTime(responseTime);
        
        return response;
    }
    
    /**
     * Get the root cause of an exception
     * 
     * @param exception The exception to analyze
     * @return The root cause exception
     */
    private static Throwable getRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
    
    /**
     * Create system context information
     * 
     * @return SystemContext with current system information
     */
    private static SystemContext createSystemContext() {
        SystemContext context = new SystemContext();
        
        Runtime runtime = Runtime.getRuntime();
        context.setJavaVersion(System.getProperty("java.version"));
        context.setAvailableMemory(runtime.freeMemory());
        context.setTotalMemory(runtime.totalMemory());
        context.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
        context.setActiveThreads(Thread.activeCount());
        context.setOperatingSystem(System.getProperty("os.name"));
        
        // Try to get Spring Boot version
        try {
            Package springBootPackage = org.springframework.boot.SpringBootVersion.class.getPackage();
            if (springBootPackage != null) {
                context.setSpringBootVersion(springBootPackage.getImplementationVersion());
            }
        } catch (Exception e) {
            context.setSpringBootVersion("Unknown");
        }
        
        return context;
    }
}