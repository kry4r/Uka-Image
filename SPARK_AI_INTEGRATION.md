# Spark AI Integration Guide

This document describes the integration of iFlytek's Spark AI model into the Uka Image Hosting System for enhanced semantic search capabilities.

## Overview

The integration replaces the existing AI analysis system with Spark AI's language understanding capabilities to provide more accurate and intelligent image search functionality.

## Features

- **Semantic Search**: AI-powered understanding of search queries
- **Language Processing**: Natural language understanding for better search results
- **Tag and Description Analysis**: AI analyzes all image tags and descriptions
- **Intelligent Filtering**: Spark AI filters relevant images based on semantic similarity

## Architecture

### Components

1. **SparkAIProperties**: Configuration class for API credentials and settings
2. **SparkAIService**: HTTP client service for Spark AI API communication
3. **AIAnalysisService**: Enhanced service with semantic search capabilities
4. **SearchController**: Updated REST API endpoints for AI-powered search

### API Integration

- **Base URL**: `https://spark-api-open.xf-yun.com/v1`
- **Model**: Spark Lite (free tier)
- **Authentication**: Bearer token authentication
- **Request Format**: JSON with chat completion format

## Configuration

### Application Properties

The Spark AI configuration is stored in `application-spark.yml`:

```yaml
spark:
  ai:
    base-url: https://spark-api-open.xf-yun.com/v1
    api-key: wEsVFmKSPqbZyWurLYaq:XRoMXcnEWpNnXxKsFhUd
    model: lite
    timeout: 30000
    max-tokens: 4096
    temperature: 0.7
```

## API Endpoints

### Search Images
- **GET** `/api/search/images?query={query}`
- **POST** `/api/search/images/semantic`

### Test Connectivity
- **GET** `/api/search/test`

### Get Search Statistics
- **GET** `/api/search/stats`

## Usage Example

### Frontend Integration

```javascript
// Search for images using semantic search
const searchImages = async (query) => {
  try {
    const response = await fetch(`/api/search/images?query=${encodeURIComponent(query)}`);
    const result = await response.json();
    
    if (result.success) {
      console.log(`Found ${result.data.totalCount} images in ${result.data.processingTimeMs}ms`);
      return result.data.results;
    }
  } catch (error) {
    console.error('Search failed:', error);
  }
};

// Example usage
searchImages('寻找自然风景的图片').then(images => {
  // Display images
});
```

### Backend Integration

```java
@Autowired
private AIAnalysisService aiAnalysisService;

// Perform semantic search
List<Image> results = aiAnalysisService.semanticSearch("用户搜索查询");
```

## How It Works

1. **User Input**: User enters search query in the frontend
2. **Data Retrieval**: System retrieves all image tags and descriptions from database
3. **AI Processing**: Spark AI analyzes the query and available metadata
4. **Semantic Matching**: AI identifies semantically relevant images
5. **Results Return**: Filtered image list is returned to the user

## Testing

Run the integration tests:

```bash
cd backend
mvn test -Dtest=SparkAIServiceTest
mvn test -Dtest=SparkAIIntegrationTest
```

## Troubleshooting

### Common Issues

1. **API Key Issues**: Ensure the API key is correctly configured
2. **Network Connectivity**: Check if the Spark AI API is accessible
3. **Rate Limiting**: Spark Lite has usage limits, monitor API calls
4. **Response Parsing**: Verify AI responses are properly formatted

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.uka.image.service.SparkAIService: DEBUG
    com.uka.image.service.AIAnalysisService: DEBUG
```

## Performance Considerations

- **Caching**: Consider caching frequent search results
- **Batch Processing**: Process multiple queries efficiently
- **Timeout Handling**: Configure appropriate timeouts for API calls
- **Error Handling**: Implement fallback mechanisms for API failures

## Security

- **API Key Protection**: Store API keys securely using environment variables
- **Input Validation**: Validate all user inputs before sending to AI
- **Rate Limiting**: Implement client-side rate limiting to prevent abuse

## Future Enhancements

- **Image Analysis**: Use Spark AI for automatic image description generation
- **Multi-language Support**: Support searches in multiple languages
- **Advanced Filtering**: Implement more sophisticated filtering options
- **Real-time Updates**: Add real-time search suggestions