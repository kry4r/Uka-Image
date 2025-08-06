# Uka Image Hosting System - Frontend

A modern Vue.js frontend for the Uka Image Hosting System with AI-powered search capabilities.

## Features

- **Modern Vue 3 + TypeScript**: Built with the latest Vue.js framework and TypeScript for type safety
- **Responsive Design**: Mobile-first design using Tailwind CSS
- **Image Management**: Upload, view, edit, and delete images with metadata
- **AI-Powered Search**: Semantic, color, and scene-based image search
- **Drag & Drop Upload**: Intuitive file upload with progress tracking
- **Album Organization**: Create and manage image albums
- **Real-time Updates**: Live search suggestions and instant feedback

## Tech Stack

- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: Pinia
- **Routing**: Vue Router
- **HTTP Client**: Axios
- **Build Tool**: Vite
- **Icons**: Heroicons

## Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── api/               # API service layer
│   │   ├── index.ts       # Axios configuration
│   │   ├── image.ts       # Image API calls
│   │   └── search.ts      # Search API calls
│   ├── components/        # Reusable components
│   │   ├── Layout/        # Layout components
│   │   └── common/        # Common UI components
│   ├── stores/            # Pinia stores
│   │   ├── image.ts       # Image state management
│   │   └── search.ts      # Search state management
│   ├── views/             # Page components
│   │   ├── Home.vue       # Homepage
│   │   ├── Gallery.vue    # Image gallery
│   │   ├── Upload.vue     # File upload
│   │   ├── Search.vue     # Search interface
│   │   ├── ImageDetail.vue # Image details
│   │   └── Albums.vue     # Album management
│   ├── router/            # Vue Router configuration
│   ├── App.vue            # Root component
│   ├── main.ts            # Application entry point
│   └── style.css          # Global styles
├── index.html             # HTML template
├── package.json           # Dependencies and scripts
├── vite.config.ts         # Vite configuration
├── tailwind.config.js     # Tailwind CSS configuration
└── tsconfig.json          # TypeScript configuration
```

## Getting Started

### Prerequisites

- Node.js 16+ and npm/yarn
- Backend API server running on port 8080

### Installation

1. Clone the repository and navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create environment file:
```bash
cp .env.example .env
```

4. Update the `.env` file with your configuration:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### Development

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Building for Production

Build the application:
```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```

## API Integration

The frontend communicates with the Java Spring Boot backend through RESTful APIs:

- **Image Management**: Upload, retrieve, update, and delete images
- **Search**: AI-powered search with multiple search types
- **Albums**: Create and manage image collections
- **Metadata**: Automatic extraction and manual editing

## Key Components

### Image Upload
- Drag-and-drop interface
- Multiple file selection
- Progress tracking
- Metadata input (description, tags)
- File validation and error handling

### Search Interface
- **Semantic Search**: Natural language queries
- **Color Search**: Find images by dominant colors
- **Scene Search**: Search by scene types (landscape, portrait, etc.)
- **Visual Search**: Find similar images
- Real-time search suggestions

### Image Gallery
- Responsive grid layout
- Pagination support
- Thumbnail optimization
- Quick actions (view, edit, delete)

### Image Detail View
- Full-size image display
- Metadata editing
- AI description generation
- Similar image suggestions
- Download and sharing options

## State Management

The application uses Pinia for state management with two main stores:

- **Image Store**: Manages image data, upload state, and CRUD operations
- **Search Store**: Handles search queries, results, and suggestions

## Styling

The application uses Tailwind CSS with custom components:

- Responsive design patterns
- Custom color palette
- Reusable component classes
- Dark mode support (planned)

## Performance Optimizations

- Lazy loading of images
- Route-based code splitting
- Optimized bundle size
- CDN integration for image delivery
- Caching strategies

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Contributing

1. Follow the existing code style and patterns
2. Use TypeScript for type safety
3. Write meaningful commit messages
4. Test your changes thoroughly

## License

This project is part of the Uka Image Hosting System.