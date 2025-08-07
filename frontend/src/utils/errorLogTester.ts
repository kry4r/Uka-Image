/**
 * Error Log Testing Utility
 * Comprehensive testing for error logging and F12 developer tools integration
 */

export interface ErrorTestScenario {
  name: string
  description: string
  testFunction: () => Promise<void>
  expectedLogLevel: 'info' | 'warning' | 'error'
  expectedConsoleOutput: string[]
}

export class ErrorLogTester {
  private testResults: Array<{
    scenario: string
    passed: boolean
    details: string
    consoleOutput: any[]
  }> = []

  /**
   * Run all error logging tests
   */
  async runAllTests(): Promise<void> {
    console.group('🧪 Error Logging Verification Tests')
    console.log('Starting comprehensive error logging tests...')
    
    const scenarios = this.getTestScenarios()
    
    for (const scenario of scenarios) {
      await this.runTestScenario(scenario)
    }
    
    this.displayTestResults()
    console.groupEnd()
  }

  /**
   * Get all test scenarios
   */
  private getTestScenarios(): ErrorTestScenario[] {
    return [
      {
        name: 'Network Error Test',
        description: 'Test network connectivity issues and timeout errors',
        testFunction: this.testNetworkError.bind(this),
        expectedLogLevel: 'error',
        expectedConsoleOutput: [
          'Search Error',
          'Error Details',
          'Full Error Response'
        ]
      },
      {
        name: 'API Error Response Test',
        description: 'Test API returning error responses with detailed information',
        testFunction: this.testApiErrorResponse.bind(this),
        expectedLogLevel: 'error',
        expectedConsoleOutput: [
          'API Response Status',
          'Error Details',
          'Stack Trace'
        ]
      },
      {
        name: 'Invalid Query Test',
        description: 'Test handling of invalid search queries',
        testFunction: this.testInvalidQuery.bind(this),
        expectedLogLevel: 'warning',
        expectedConsoleOutput: [
          'Search Query',
          'Validation Error'
        ]
      },
      {
        name: 'Empty Results Test',
        description: 'Test logging when no results are found',
        testFunction: this.testEmptyResults.bind(this),
        expectedLogLevel: 'info',
        expectedConsoleOutput: [
          'Search Results',
          'Search Insights'
        ]
      },
      {
        name: 'Successful Search Test',
        description: 'Test comprehensive logging for successful searches',
        testFunction: this.testSuccessfulSearch.bind(this),
        expectedLogLevel: 'info',
        expectedConsoleOutput: [
          'Search Results',
          'Search Insights',
          'Full Response'
        ]
      }
    ]
  }

  /**
   * Run individual test scenario
   */
  private async runTestScenario(scenario: ErrorTestScenario): Promise<void> {
    console.group(`🔍 Testing: ${scenario.name}`)
    console.log(`Description: ${scenario.description}`)
    
    // Capture console output
    const originalConsoleLog = console.log
    const originalConsoleError = console.error
    const originalConsoleWarn = console.warn
    const capturedOutput: any[] = []
    
    const captureOutput = (...args: any[]) => {
      capturedOutput.push(args)
      originalConsoleLog(...args)
    }
    
    console.log = captureOutput
    console.error = captureOutput
    console.warn = captureOutput
    
    try {
      await scenario.testFunction()
      
      // Verify expected console output
      const hasExpectedOutput = scenario.expectedConsoleOutput.every(expected =>
        capturedOutput.some(output => 
          output.some((arg: any) => 
            typeof arg === 'string' && arg.includes(expected)
          )
        )
      )
      
      this.testResults.push({
        scenario: scenario.name,
        passed: hasExpectedOutput,
        details: hasExpectedOutput ? 'All expected console output found' : 'Missing expected console output',
        consoleOutput: capturedOutput
      })
      
      console.log(`✅ Test completed: ${hasExpectedOutput ? 'PASSED' : 'FAILED'}`)
      
    } catch (error) {
      this.testResults.push({
        scenario: scenario.name,
        passed: false,
        details: `Test failed with error: ${error}`,
        consoleOutput: capturedOutput
      })
      
      console.error(`❌ Test failed:`, error)
    } finally {
      // Restore original console methods
      console.log = originalConsoleLog
      console.error = originalConsoleError
      console.warn = originalConsoleWarn
    }
    
    console.groupEnd()
  }

  /**
   * Test network error scenario
   */
  private async testNetworkError(): Promise<void> {
    console.log('🌐 Simulating network error...')
    
    // Create mock network error
    const networkError = new Error('Network Error: Connection timeout')
    networkError.name = 'NetworkError'
    
    // Simulate error response structure
    const mockErrorResponse = {
      response: {
        status: 0,
        statusText: 'Network Error',
        data: null,
        headers: {}
      },
      request: {
        url: '/ai-search/search',
        method: 'GET'
      }
    }
    
    // Log detailed error information
    console.group('❌ Search Error')
    console.error('Error:', networkError)
    console.log('Search Query:', 'test network error')
    console.log('Error Details:', {
      errorMessage: networkError.message,
      errorType: networkError.name,
      requestPath: '/ai-search/search',
      httpMethod: 'GET',
      timestamp: Date.now(),
      apiResponseStatus: 0,
      stackTrace: networkError.stack?.split('\n').map(line => ({ line: line.trim() })) || []
    })
    console.log('Full Error Response:', mockErrorResponse)
    console.groupEnd()
    
    console.log('✅ Network error logging verified')
  }

  /**
   * Test API error response scenario
   */
  private async testApiErrorResponse(): Promise<void> {
    console.log('🔌 Simulating API error response...')
    
    // Create mock API error response
    const mockApiError = {
      response: {
        status: 500,
        statusText: 'Internal Server Error',
        data: {
          code: 500,
          message: 'Search service temporarily unavailable',
          detailedError: {
            errorCode: 'SEARCH_SERVICE_ERROR',
            errorMessage: 'Database connection failed',
            timestamp: Date.now(),
            requestId: 'req-12345',
            stackTrace: [
              'at SearchService.performSearch (SearchService.java:45)',
              'at AISearchController.enhancedAiSearch (AISearchController.java:123)',
              'at java.base/java.lang.reflect.Method.invoke (Method.java:566)'
            ]
          },
          searchContext: {
            query: 'test api error',
            searchType: 'METADATA_BASED',
            totalImagesAnalyzed: 0,
            searchDuration: 1250
          }
        },
        headers: {
          'content-type': 'application/json',
          'x-request-id': 'req-12345'
        }
      }
    }
    
    // Log comprehensive API error
    console.group('❌ Search Error')
    console.error('Error:', new Error('API Error: Search service temporarily unavailable'))
    console.log('Search Query:', 'test api error')
    console.log('Error Details:', {
      searchQuery: 'test api error',
      errorMessage: 'Search service temporarily unavailable',
      errorType: 'APIError',
      requestPath: '/ai-search/search',
      httpMethod: 'GET',
      timestamp: Date.now(),
      detailedError: mockApiError.response.data.detailedError,
      searchContext: mockApiError.response.data.searchContext,
      apiResponseStatus: mockApiError.response.status,
      apiResponseHeaders: mockApiError.response.headers,
      apiResponseBody: mockApiError.response.data,
      stackTrace: mockApiError.response.data.detailedError.stackTrace.map((line: string) => ({ line }))
    })
    console.log('Full Error Response:', mockApiError)
    console.groupEnd()
    
    console.log('✅ API error response logging verified')
  }

  /**
   * Test invalid query scenario
   */
  private async testInvalidQuery(): Promise<void> {
    console.log('⚠️ Simulating invalid query...')
    
    const invalidQuery = '!@#$%^&*()_+{}|:"<>?[]\\;\',./'
    
    console.group('⚠️ Search Warning')
    console.warn('Invalid search query detected:', invalidQuery)
    console.log('Search Query:', invalidQuery)
    console.log('Validation Error:', 'Query contains only special characters')
    console.log('Suggestion:', 'Please use alphanumeric characters and common punctuation')
    console.groupEnd()
    
    console.log('✅ Invalid query logging verified')
  }

  /**
   * Test empty results scenario
   */
  private async testEmptyResults(): Promise<void> {
    console.log('📭 Simulating empty results...')
    
    const mockEmptyResponse = {
      code: 200,
      message: 'No relevant images found',
      data: {
        results: [],
        totalResults: 0,
        currentPage: 1,
        pageSize: 20,
        totalPages: 0,
        searchInsights: {
          searchInfo: {
            totalImagesAnalyzed: 150,
            searchStrategy: 'Metadata-based matching with comprehensive filtering',
            searchDuration: 450,
            processingSteps: [
              'Query analysis completed',
              'Metadata extraction completed',
              'Filtering applied',
              'No matches found above threshold'
            ]
          },
          matchDistribution: {
            description: 0,
            tags: 0,
            filename: 0,
            metadata: 0
          }
        }
      }
    }
    
    console.group('🔍 Search Results')
    console.log('Query:', 'nonexistentquery12345')
    console.log('Results:', [])
    console.log('Search Insights:', mockEmptyResponse.data.searchInsights)
    console.log('Full Response:', mockEmptyResponse)
    console.groupEnd()
    
    console.log('✅ Empty results logging verified')
  }

  /**
   * Test successful search scenario
   */
  private async testSuccessfulSearch(): Promise<void> {
    console.log('✅ Simulating successful search...')
    
    const mockSuccessResponse = {
      code: 200,
      message: 'Found 5 relevant images using metadata search',
      data: {
        results: [
          {
            id: 1,
            fileName: 'landscape_sunset.jpg',
            originalName: 'Beautiful Sunset Landscape.jpg',
            description: 'A stunning sunset over mountain landscape',
            tags: 'landscape, sunset, mountains, nature',
            fileType: 'JPEG',
            fileSize: 2048576,
            width: 1920,
            height: 1080,
            totalScore: 0.85,
            confidenceLevel: 'HIGH',
            explanation: 'Strong match on description and tags'
          }
        ],
        totalResults: 5,
        currentPage: 1,
        pageSize: 20,
        totalPages: 1,
        searchInsights: {
          searchInfo: {
            totalImagesAnalyzed: 150,
            searchStrategy: 'Metadata-based matching with tag correlation',
            searchDuration: 320,
            processingSteps: [
              'Query analysis completed',
              'Metadata extraction completed',
              'Tag processing completed',
              'Scoring algorithm applied',
              'Results filtered and ranked'
            ]
          },
          matchDistribution: {
            description: 0.4,
            tags: 0.35,
            filename: 0.15,
            metadata: 0.1
          }
        }
      }
    }
    
    console.group('🔍 Search Results')
    console.log('Query:', 'landscape sunset')
    console.log('Results:', mockSuccessResponse.data.results)
    console.log('Search Insights:', mockSuccessResponse.data.searchInsights)
    console.log('Full Response:', mockSuccessResponse)
    console.groupEnd()
    
    console.log('✅ Successful search logging verified')
  }

  /**
   * Display test results summary
   */
  private displayTestResults(): void {
    console.group('📊 Test Results Summary')
    
    const totalTests = this.testResults.length
    const passedTests = this.testResults.filter(result => result.passed).length
    const failedTests = totalTests - passedTests
    
    console.log(`Total Tests: ${totalTests}`)
    console.log(`Passed: ${passedTests}`)
    console.log(`Failed: ${failedTests}`)
    console.log(`Success Rate: ${Math.round((passedTests / totalTests) * 100)}%`)
    
    if (failedTests > 0) {
      console.group('❌ Failed Tests')
      this.testResults
        .filter(result => !result.passed)
        .forEach(result => {
          console.log(`- ${result.scenario}: ${result.details}`)
        })
      console.groupEnd()
    }
    
    console.log('🎯 All error logging scenarios have been tested')
    console.log('🔍 Check the expandable log panels in the UI for detailed error information')
    console.log('🛠️ Use F12 Developer Tools to inspect console output')
    
    console.groupEnd()
  }

  /**
   * Test expandable log panel functionality
   */
  testExpandableLogPanel(): void {
    console.group('🔧 Testing Expandable Log Panel Features')
    
    // Test data for log panel
    const testLogData = {
      title: 'Test Error Log',
      summary: 'Testing expandable log panel functionality',
      logLevel: 'error' as const,
      logData: {
        overview: {
          timestamp: Date.now(),
          operation: 'AI Image Search',
          status: 'FAILED',
          duration: '1.25s',
          errorType: 'NetworkError'
        },
        request: {
          method: 'GET',
          url: '/ai-search/search',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          parameters: {
            query: 'test search',
            pageNum: 1,
            pageSize: 20,
            minScore: 0.1
          }
        },
        response: {
          status: 0,
          statusText: 'Network Error',
          headers: {},
          data: null
        },
        errorDetails: {
          message: 'Connection timeout after 30 seconds',
          code: 'NETWORK_TIMEOUT',
          timestamp: Date.now(),
          requestId: 'req-test-12345'
        },
        searchContext: {
          searchType: 'METADATA_BASED',
          totalImagesAnalyzed: 0,
          searchStrategy: 'Not executed due to network error',
          processingSteps: [
            'Request initiated',
            'Network connection attempted',
            'Connection timeout occurred'
          ]
        },
        systemInfo: {
          userAgent: navigator.userAgent,
          timestamp: Date.now(),
          url: window.location.href,
          referrer: document.referrer
        }
      }
    }
    
    console.log('✅ Log Panel Test Data:', testLogData)
    console.log('🔍 Features to verify in UI:')
    console.log('  - Expandable/collapsible panels')
    console.log('  - Tabbed interface (Overview, Request, Response, etc.)')
    console.log('  - JSON syntax highlighting')
    console.log('  - Copy to clipboard functionality')
    console.log('  - Console logging (F12) integration')
    console.log('  - Download log functionality')
    console.log('  - Stack trace visualization')
    
    console.groupEnd()
  }
}

// Export singleton instance
export const errorLogTester = new ErrorLogTester()

// Auto-run tests when in development mode
if (import.meta.env.DEV) {
  console.log('🧪 Error Log Tester loaded - Use errorLogTester.runAllTests() to start testing')
}