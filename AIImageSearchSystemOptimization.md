# AI Image Search System Optimization

## Core Features

- Remove manual search weights

- Enhanced AI format recognition

- Upload time integration

- Optimized message pushing

## Tech Stack

{
  "Backend": "Java Spring Boot with MyBatis Plus, Spark AI service integration",
  "Frontend": "Vue.js with TypeScript",
  "AI": "Enhanced Spark AI prompts for metadata extraction",
  "Database": "Existing image metadata storage"
}

## Design

Backend service optimization with enhanced AI prompt engineering for improved image search accuracy and streamlined message pushing

## Plan

Note: 

- [ ] is holding
- [/] is doing
- [X] is done

---

[X] Analyze current searchWeights implementation and usage patterns

[X] Remove searchWeights parameter from Search Criteria model and related DTOs

[X] Update search service methods to eliminate weight-based filtering logic

[ ] Modify message pushing service to exclude searchWeights from notification payloads

[X] Design enhanced AI prompts for format recognition with high accuracy parsing

[X] Implement resolution and image size detection prompts in Spark AI service

[X] Create filename and description analysis prompts with context awareness

[X] Integrate upload time context into AI prompt templates

[X] Update image metadata extraction service to use new AI prompts

[X] Modify search controller to work with AI-only relevance scoring

[/] Update frontend TypeScript interfaces to remove searchWeights properties

[ ] Test AI-driven search functionality with various image formats and metadata
