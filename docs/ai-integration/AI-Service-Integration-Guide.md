# AI服务集成指南

## 概述

本指南详细介绍了如何在Uka图片托管系统中集成AI服务，实现智能图片分析、自动标签生成和语义搜索功能。

## 系统架构

### AI服务组件
- **AIAnalysisService**: 核心AI分析服务
- **ImageSearchMetadata**: 图片搜索元数据实体
- **SearchController**: AI搜索控制器
- **DescriptionController**: 智能描述生成控制器

### 数据流程
1. 图片上传 → AI分析 → 元数据提取 → 数据库存储
2. 搜索请求 → 语义匹配 → 结果排序 → 返回结果

## 配置说明

### 1. 数据库配置

确保已创建图片搜索元数据表：

```sql
CREATE TABLE image_search_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_id BIGINT NOT NULL,
    ai_description TEXT,
    ai_tags JSON,
    color_palette JSON,
    objects_detected JSON,
    scene_analysis JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
);
```

### 2. AI服务配置

在 `application.yml` 中添加AI服务配置：

```yaml
ai:
  service:
    enabled: true
    provider: "openai"  # 支持: openai, baidu, tencent
    api-key: "${AI_API_KEY:your-api-key}"
    api-url: "${AI_API_URL:https://api.openai.com/v1}"
    model: "gpt-4-vision-preview"
    timeout: 30000
    max-retries: 3
  
  analysis:
    auto-generate-tags: true
    auto-generate-description: true
    extract-colors: true
    detect-objects: true
    analyze-scene: true
    
  search:
    similarity-threshold: 0.7
    max-results: 50
    enable-semantic-search: true
```

### 3. 环境变量配置

设置必要的环境变量：

```bash
# AI服务配置
export AI_API_KEY="your-openai-api-key"
export AI_API_URL="https://api.openai.com/v1"

# 百度AI服务（可选）
export BAIDU_AI_API_KEY="your-baidu-api-key"
export BAIDU_AI_SECRET_KEY="your-baidu-secret-key"

# 腾讯AI服务（可选）
export TENCENT_AI_SECRET_ID="your-tencent-secret-id"
export TENCENT_AI_SECRET_KEY="your-tencent-secret-key"
```

## AI服务提供商集成

### 1. OpenAI集成

#### 依赖配置
在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```

#### 服务实现
```java
@Service
public class OpenAIAnalysisService implements AIAnalysisService {
    
    @Value("${ai.service.api-key}")
    private String apiKey;
    
    @Value("${ai.service.model}")
    private String model;
    
    private OpenAiService openAiService;
    
    @PostConstruct
    public void init() {
        this.openAiService = new OpenAiService(apiKey);
    }
    
    @Override
    public AIAnalysisResult analyzeImage(String imageUrl) {
        // 实现图片分析逻辑
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(Arrays.asList(
                new ChatMessage(ChatMessageRole.USER.value(), 
                    "请分析这张图片，提供详细描述、标签和场景分析")
            ))
            .build();
            
        ChatCompletionResult result = openAiService.createChatCompletion(request);
        return parseAnalysisResult(result);
    }
}
```

### 2. 百度AI集成

#### 依赖配置
```xml
<dependency>
    <groupId>com.baidu.aip</groupId>
    <artifactId>java-sdk</artifactId>
    <version>4.16.8</version>
</dependency>
```

#### 服务实现
```java
@Service
public class BaiduAIAnalysisService implements AIAnalysisService {
    
    @Value("${baidu.ai.api-key}")
    private String apiKey;
    
    @Value("${baidu.ai.secret-key}")
    private String secretKey;
    
    private AipImageClassify client;
    
    @PostConstruct
    public void init() {
        this.client = new AipImageClassify(apiKey, secretKey);
    }
    
    @Override
    public AIAnalysisResult analyzeImage(String imageUrl) {
        // 实现百度AI图片分析
        JSONObject result = client.advancedGeneral(imageUrl, new HashMap<>());
        return parseBaiduResult(result);
    }
}
```

### 3. 腾讯AI集成

#### 依赖配置
```xml
<dependency>
    <groupId>com.tencentcloudapi</groupId>
    <artifactId>tencentcloud-sdk-java</artifactId>
    <version>3.1.562</version>
</dependency>
```

#### 服务实现
```java
@Service
public class TencentAIAnalysisService implements AIAnalysisService {
    
    @Value("${tencent.ai.secret-id}")
    private String secretId;
    
    @Value("${tencent.ai.secret-key}")
    private String secretKey;
    
    private TiiaClient client;
    
    @PostConstruct
    public void init() {
        Credential cred = new Credential(secretId, secretKey);
        this.client = new TiiaClient(cred, "ap-beijing");
    }
    
    @Override
    public AIAnalysisResult analyzeImage(String imageUrl) {
        // 实现腾讯AI图片分析
        DetectLabelRequest req = new DetectLabelRequest();
        req.setImageUrl(imageUrl);
        
        DetectLabelResponse resp = client.DetectLabel(req);
        return parseTencentResult(resp);
    }
}
```

## API接口说明

### 1. AI分析接口

#### 生成AI描述
```http
POST /api/descriptions/ai-generate/{imageId}
Content-Type: application/json

Response:
{
    "success": true,
    "data": {
        "description": "一张美丽的风景照片，展示了蓝天白云下的青山绿水",
        "confidence": 0.95
    }
}
```

#### 批量生成AI描述
```http
POST /api/descriptions/batch-ai-generate
Content-Type: application/json

{
    "imageIds": [1, 2, 3, 4, 5]
}

Response:
{
    "success": true,
    "data": {
        "totalProcessed": 5,
        "successCount": 4,
        "failureCount": 1,
        "results": [
            {
                "imageId": 1,
                "success": true,
                "description": "AI生成的描述"
            }
        ]
    }
}
```

### 2. AI搜索接口

#### 语义搜索
```http
GET /api/search/ai?query=蓝天白云的风景&limit=20
Content-Type: application/json

Response:
{
    "success": true,
    "data": {
        "images": [
            {
                "id": 1,
                "filename": "landscape.jpg",
                "url": "/api/files/uploads/2025/01/01/landscape.jpg",
                "aiDescription": "蓝天白云下的美丽风景",
                "tags": ["风景", "蓝天", "白云"],
                "similarity": 0.92
            }
        ],
        "total": 15,
        "searchTime": 245
    }
}
```

#### 高级搜索选项
```http
GET /api/search/ai?query=风景&options={"includeColors":true,"includeObjects":true,"minSimilarity":0.8}

Response:
{
    "success": true,
    "data": {
        "images": [...],
        "searchOptions": {
            "includeColors": true,
            "includeObjects": true,
            "minSimilarity": 0.8
        },
        "total": 8
    }
}
```

## 前端集成

### 1. AI搜索组件

```vue
<template>
  <div class="ai-search-container">
    <div class="search-input-group">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="使用AI智能搜索图片..."
        class="ai-search-input"
        @keyup.enter="performAISearch"
      />
      <button @click="performAISearch" class="ai-search-button">
        <i class="fas fa-brain"></i> AI搜索
      </button>
    </div>
    
    <div v-if="isSearching" class="search-loading">
      <i class="fas fa-spinner fa-spin"></i> AI正在分析搜索内容...
    </div>
    
    <div v-if="searchResults.length > 0" class="search-results">
      <div class="results-header">
        <h3>找到 {{ searchResults.length }} 张相关图片</h3>
        <span class="search-time">搜索耗时: {{ searchTime }}ms</span>
      </div>
      
      <div class="image-grid">
        <div
          v-for="image in searchResults"
          :key="image.id"
          class="image-card"
          @click="openImageDetail(image)"
        >
          <img :src="image.url" :alt="image.filename" />
          <div class="image-info">
            <p class="ai-description">{{ image.aiDescription }}</p>
            <div class="similarity-score">
              相似度: {{ (image.similarity * 100).toFixed(1) }}%
            </div>
            <div class="image-tags">
              <span
                v-for="tag in image.tags"
                :key="tag"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchAPI } from '@/api/search'

const searchQuery = ref('')
const searchResults = ref([])
const isSearching = ref(false)
const searchTime = ref(0)

const performAISearch = async () => {
  if (!searchQuery.value.trim()) return
  
  isSearching.value = true
  try {
    const startTime = Date.now()
    const response = await searchAPI.aiSearch(searchQuery.value)
    searchTime.value = Date.now() - startTime
    
    if (response.success) {
      searchResults.value = response.data.images
    }
  } catch (error) {
    console.error('AI搜索失败:', error)
  } finally {
    isSearching.value = false
  }
}
</script>
```

### 2. AI描述生成组件

```vue
<template>
  <div class="ai-description-generator">
    <button
      @click="generateAIDescription"
      :disabled="isGenerating"
      class="generate-button"
    >
      <i class="fas fa-magic"></i>
      {{ isGenerating ? '生成中...' : '生成AI描述' }}
    </button>
    
    <div v-if="aiDescription" class="ai-description-result">
      <h4>AI生成的描述:</h4>
      <p>{{ aiDescription }}</p>
      <div class="confidence-score">
        置信度: {{ (confidence * 100).toFixed(1) }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { descriptionAPI } from '@/api/description'

const props = defineProps<{
  imageId: number
}>()

const isGenerating = ref(false)
const aiDescription = ref('')
const confidence = ref(0)

const generateAIDescription = async () => {
  isGenerating.value = true
  try {
    const response = await descriptionAPI.generateAI(props.imageId)
    if (response.success) {
      aiDescription.value = response.data.description
      confidence.value = response.data.confidence
    }
  } catch (error) {
    console.error('生成AI描述失败:', error)
  } finally {
    isGenerating.value = false
  }
}
</script>
```

## 性能优化

### 1. 缓存策略

```java
@Service
@CacheConfig(cacheNames = "ai-analysis")
public class AIAnalysisService {
    
    @Cacheable(key = "#imageId")
    public AIAnalysisResult getAnalysisResult(Long imageId) {
        // 从缓存获取分析结果
    }
    
    @CacheEvict(key = "#imageId")
    public void invalidateAnalysisCache(Long imageId) {
        // 清除缓存
    }
}
```

### 2. 异步处理

```java
@Service
public class AsyncAIAnalysisService {
    
    @Async("aiAnalysisExecutor")
    public CompletableFuture<AIAnalysisResult> analyzeImageAsync(String imageUrl) {
        AIAnalysisResult result = performAnalysis(imageUrl);
        return CompletableFuture.completedFuture(result);
    }
}
```

### 3. 批量处理

```java
@Service
public class BatchAIAnalysisService {
    
    public BatchAnalysisResult processBatch(List<Long> imageIds) {
        return imageIds.parallelStream()
            .map(this::analyzeImage)
            .collect(Collectors.toList());
    }
}
```

## 监控和日志

### 1. 性能监控

```java
@Component
public class AIServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer analysisTimer;
    private final Counter successCounter;
    private final Counter errorCounter;
    
    public AIServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.analysisTimer = Timer.builder("ai.analysis.duration")
            .description("AI分析耗时")
            .register(meterRegistry);
        this.successCounter = Counter.builder("ai.analysis.success")
            .description("AI分析成功次数")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("ai.analysis.error")
            .description("AI分析失败次数")
            .register(meterRegistry);
    }
}
```

### 2. 日志配置

```yaml
logging:
  level:
    com.uka.image.service.AIAnalysisService: DEBUG
    com.uka.image.controller.SearchController: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%logger{36}] - %msg%n"
  file:
    name: logs/ai-service.log
    max-size: 100MB
    max-history: 30
```

## 故障排除

### 1. 常见问题

#### API密钥错误
```
错误: Unauthorized - Invalid API key
解决: 检查环境变量AI_API_KEY是否正确设置
```

#### 网络连接超时
```
错误: Connection timeout
解决: 
1. 检查网络连接
2. 增加超时时间配置
3. 配置代理服务器（如需要）
```

#### 内存不足
```
错误: OutOfMemoryError
解决:
1. 增加JVM堆内存: -Xmx2g
2. 启用批量处理限制
3. 实现图片压缩预处理
```

### 2. 调试工具

#### AI服务健康检查
```java
@RestController
@RequestMapping("/api/ai/health")
public class AIHealthController {
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("services", checkAllServices());
        return ResponseEntity.ok(health);
    }
}
```

#### 测试接口
```java
@RestController
@RequestMapping("/api/ai/test")
public class AITestController {
    
    @PostMapping("/analyze")
    public ResponseEntity<AIAnalysisResult> testAnalysis(@RequestParam String imageUrl) {
        // 测试AI分析功能
        return ResponseEntity.ok(aiAnalysisService.analyzeImage(imageUrl));
    }
}
```

## 安全考虑

### 1. API密钥管理
- 使用环境变量存储敏感信息
- 定期轮换API密钥
- 实施访问控制和审计日志

### 2. 数据隐私
- 图片数据加密传输
- 敏感信息脱敏处理
- 遵循数据保护法规

### 3. 访问限制
```java
@Component
public class AIServiceRateLimiter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isAllowed(String userId) {
        String key = "ai:rate_limit:" + userId;
        String count = redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(1));
            return true;
        }
        
        int currentCount = Integer.parseInt(count);
        if (currentCount >= 100) { // 每小时100次限制
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
```

## 扩展功能

### 1. 多模态搜索
- 支持文本+图片组合搜索
- 语音搜索转文本
- 手绘草图搜索

### 2. 智能推荐
- 基于用户行为的个性化推荐
- 相似图片推荐
- 热门内容推荐

### 3. 自动分类
- 智能相册分类
- 场景自动识别
- 人物自动标记

## 总结

本指南提供了完整的AI服务集成方案，包括多个AI服务提供商的接入、前后端实现、性能优化和故障排除。通过遵循本指南，您可以成功地在Uka图片托管系统中实现强大的AI功能，提升用户体验和系统智能化水平。

如需更多技术支持，请参考相关API文档或联系技术团队。