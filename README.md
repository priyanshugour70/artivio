# Artivio - Where creativity meets intelligence 🚀

A full-fledged real-time chat application with AI integration, supporting human-to-human and human-to-AI conversations with word-by-word streaming responses.

## Features

- 🔐 **Authentication**: JWT-based user authentication with registration and login
- 💬 **Real-time Chat**: WebSocket-based real-time messaging between users
- 🤖 **AI Integration**: Chat with Google Gemini AI with word-by-word streaming responses
- 🎯 **Context Awareness**: AI remembers conversation history using vector embeddings
- 👥 **Multi-user Support**: Multiple users can chat with each other simultaneously
- 📱 **Online Status**: Real-time online/offline user status
- 🎨 **Modern UI**: Beautiful and responsive chat interface

## Tech Stack

- **Backend**: Spring Boot 3.5.4, Java 21
- **Database**: PostgreSQL
- **Vector Store**: ChromaDB for AI context storage
- **AI**: Google Gemini 1.5 Flash (free tier) with optional ChromaDB for context storage
- **Authentication**: JWT with Spring Security
- **Real-time**: WebSocket with STOMP
- **Caching**: Redis
- **Build Tool**: Maven

## Setup Instructions

### Prerequisites

1. **Java 21** or higher
2. **PostgreSQL** database
3. **ChromaDB** (optional - for AI context storage)
4. **Redis** (for caching)
5. **Gemini API Key** (free from Google AI Studio)

### 1. Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE artivio_db;
CREATE USER postgres WITH ENCRYPTED PASSWORD 'Harda@20p';
GRANT ALL PRIVILEGES ON DATABASE artivio_db TO postgres;
```

### 2. ChromaDB Setup (Optional - for AI context memory)

If you want AI to remember conversation history, install and run ChromaDB:
```bash
pip install chromadb
chroma run --host localhost --port 8000
```

**Note**: The application will work without ChromaDB, but AI won't remember previous messages in the conversation.

### 3. Redis Setup

Install and run Redis:
```bash
# On macOS
brew install redis
brew services start redis

# On Ubuntu/Debian
sudo apt install redis-server
sudo systemctl start redis-server
```

### 4. Environment Variables

Get your Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey) (free).

Create a `.env` file or set environment variables:
```bash
DB_USERNAME=postgres
DB_PASSWORD=Harda@20p
GEMINI_API_KEY=your-gemini-api-key-here
JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

**Note**: OpenAI API key is NOT required! We use Google Gemini for all AI functionality.

### 5. Run the Application

```bash
# Clone the repository (if from git)
git clone <repository-url>
cd klakar-artivio

# Build the application
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### 6. Test the Application

1. Open `http://localhost:8080` in your browser
2. Register a new account or login
3. Start chatting with AI or other users!

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### Chat
- `GET /api/chat/conversations` - Get user conversations
- `POST /api/chat/conversations` - Create/get conversation
- `GET /api/chat/conversations/{id}/messages` - Get conversation messages
- `POST /api/chat/messages` - Send message
- `GET /api/chat/users/online` - Get online users

### WebSocket Endpoints
- `/ws` - WebSocket connection endpoint
- `/app/chat.sendMessage` - Send message
- `/app/chat.typing` - Typing notification
- `/topic/user.status` - User status updates
- `/user/queue/messages` - User-specific messages
- `/user/queue/message-updates` - Message updates (for streaming)

## Configuration

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/artivio_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:Harda@20p}
  
  ai:
    vectorstore:
      chroma:
        client:
          host: localhost
          port: 8000
        collection-name: artivio-conversations

# Gemini AI Configuration (No OpenAI needed!)
app:
  gemini:
    api-key: ${GEMINI_API_KEY:demo}
    model: gemini-1.5-flash

jwt:
  secret-key: ${JWT_SECRET_KEY:your-secret-key}
  expiration-time: 86400000
```

## Features Explained

### 1. Real-time Messaging
- Uses WebSocket with STOMP protocol
- Instant message delivery
- Typing indicators
- Online/offline status

### 2. AI Integration
- Word-by-word streaming responses for realistic chat experience
- Context-aware conversations using vector embeddings
- Conversation history stored in ChromaDB
- Supports both Gemini and OpenAI models

### 3. User Management
- JWT-based authentication
- User registration and login
- Online status tracking
- Multi-user conversations

### 4. Data Storage
- All conversations stored in PostgreSQL
- Vector embeddings in ChromaDB for AI context
- Redis for caching and session management

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check if PostgreSQL is running
   - Verify database credentials
   - Ensure database exists

2. **ChromaDB Connection Error**
   - Check if ChromaDB is running on port 8000
   - Verify the host and port in configuration

3. **WebSocket Connection Issues**
   - Check CORS configuration
   - Verify JWT token is being sent

4. **AI Responses Not Working**
   - Check API key configuration
   - Verify AI service is accessible

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/klakar/artivio/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST and WebSocket controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── repository/     # Data repositories
│   │   └── service/        # Business logic
│   └── resources/
│       ├── application.yml  # Application configuration
│       ├── static/         # Static web assets
│       └── templates/      # Templates (if any)
```

### Adding New Features

1. Create new entities in `entity/` package
2. Add repositories in `repository/` package
3. Implement business logic in `service/` package
4. Create controllers in `controller/` package
5. Add DTOs in `dto/` package for data transfer

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Write tests
5. Submit pull request

## License

This project is licensed under the MIT License.