# 🎮 Multiplayer Tic-Tac-Toe

A real-time multiplayer Tic-Tac-Toe game built with Spring Boot and WebSocket technology.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)

🔗 Quick Links

🎮 Live Demo: https://tic-tac-toe-nyo1.onrender.com
💻 GitHub Repository: https://github.com/raaghu2002/tic-tac-toe.git
📖 API Documentation: Swagger UI
🏥 Health Check: API Health


## 📋 Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Game Rules](#game-rules)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## ✨ Features

### Core Functionality
- **Real-time Multiplayer Gaming**: Play against real opponents using WebSocket technology
- **Automatic Matchmaking**: Smart pairing system connects you with available players
- **Player Statistics**: Track wins, losses, and draws with persistent storage
- **Leaderboard System**: Compete for the top spot with a scoring system
- **Session Management**: Automatic cleanup of disconnected players
- **Responsive Design**: Play on desktop, tablet, or mobile devices
- **Heartbeat Mechanism**: Maintains connection stability

### Game Features
- Real-time board updates
- Turn indicators
- Win/draw detection
- Forfeit option
- Play again functionality
- Player statistics display
- Visual animations and effects

## 🛠 Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 17
- **WebSocket**: Spring WebSocket + STOMP Protocol
- **Database**: 
  - Development: H2 (in-memory)
  - Production: PostgreSQL
- **ORM**: Spring Data JPA
- **Build Tool**: Maven
- **Documentation**: Swagger/OpenAPI 3.0

### Frontend
- **Core**: HTML5, CSS3, Vanilla JavaScript
- **WebSocket Client**: SockJS 1.6.1 + STOMP.js 2.3.3
- **Styling**: Custom CSS with animations

### Key Dependencies
```xml
- spring-boot-starter-web
- spring-boot-starter-websocket
- spring-boot-starter-data-jpa
- h2database (dev)
- postgresql (prod)
- lombok
- jackson-databind
```

## 🏗 Architecture

### System Architecture
```
┌─────────────────┐
│   Web Browser   │
│   (HTML/JS/CSS) │
└────────┬────────┘
         │ HTTP/WebSocket
         ▼
┌─────────────────┐
│  Spring Boot    │
│  Application    │
│  ┌───────────┐  │
│  │Controller │  │
│  │  Layer    │  │
│  └─────┬─────┘  │
│        │        │
│  ┌─────▼─────┐  │
│  │ Service   │  │
│  │  Layer    │  │
│  └─────┬─────┘  │
│        │        │
│  ┌─────▼─────┐  │
│  │Repository │  │
│  │  Layer    │  │
│  └─────┬─────┘  │
└────────┼────────┘
         │ JPA
         ▼
┌─────────────────┐
│    Database     │
│   (H2/Postgres) │
└─────────────────┘
```

### Component Diagram
```
GameController (WebSocket)
    ├─→ GameService (Game Logic)
    │   ├─→ Matchmaking Queue
    │   ├─→ Active Games Map
    │   └─→ Session Management
    │
    └─→ PlayerService (Persistence)
        └─→ PlayerRepository (JPA)
            └─→ Database

RestApiController (REST)
    └─→ PlayerService
        └─→ PlayerRepository
```

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Git** ([Download](https://git-scm.com/downloads))

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/raaghu/tic-tac-toe.git
cd tictactoe
```

#### 2. Build the Project
```bash
mvn clean install
```

#### 3. Run the Application
```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/tictactoe-0.0.1-SNAPSHOT.jar
```

#### 4. Access the Application
- **Game Interface**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console
- **API Health**: http://localhost:8081/api/health
- **Swagger UI**: http://localhost:8081/swagger-ui.html

### Quick Start Guide

1. **Open the game** in your browser at `http://localhost:8081`
2. **Enter a nickname** (3-20 characters)
3. **Click "Start Playing"** to enter matchmaking
4. **Wait for an opponent** (or open another browser window to test)
5. **Play the game** by clicking cells when it's your turn
6. **View leaderboard** to see top players

## 📚 API Documentation

### REST Endpoints

#### Get Leaderboard
```http
GET /api/leaderboard?limit={number}
```

**Parameters:**
- `limit` (optional, default: 10): Number of top players to return

**Response:**
```json
[
  {
    "nickname": "Player1",
    "wins": 5,
    "losses": 2,
    "draws": 1,
    "record": "5/2/1",
    "totalScore": 1050
  }
]
```

#### Get Player Statistics
```http
GET /api/player/{nickname}
```

**Response:**
```json
{
  "id": 1,
  "nickname": "Player1",
  "wins": 5,
  "losses": 2,
  "draws": 1,
  "totalScore": 1050,
  "createdAt": "2025-01-15T10:30:00",
  "lastPlayed": "2025-01-15T14:20:00"
}
```

#### Get System Statistics
```http
GET /api/stats
```

**Response:**
```json
{
  "activeGames": 3,
  "waitingPlayers": 1
}
```

#### Health Check
```http
GET /api/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "TicTacToe Multiplayer"
}
```

### WebSocket Endpoints

#### Connection
- **Endpoint**: `/ws`
- **Protocol**: STOMP over SockJS

#### Subscribe Destinations
- `/queue/matchmaking-{nickname}` - Matchmaking updates
- `/topic/game/{gameId}` - Game state updates
- `/queue/error-{nickname}` - Error messages

#### Send Destinations
- `/app/join` - Join matchmaking
- `/app/move` - Make a move
- `/app/cancel` - Cancel matchmaking
- `/app/forfeit` - Forfeit game
- `/app/heartbeat` - Keep connection alive

### WebSocket Message Examples

#### Join Matchmaking
```javascript
stompClient.send('/app/join', {}, JSON.stringify({
  nickname: "Player1"
}));
```

#### Make a Move
```javascript
stompClient.send('/app/move', {}, JSON.stringify({
  gameId: "game-uuid",
  nickname: "Player1",
  row: 0,
  col: 1
}));
```

#### Forfeit Game
```javascript
stompClient.send('/app/forfeit', {}, JSON.stringify({
  gameId: "game-uuid",
  nickname: "Player1"
}));
```

## 🎯 Game Rules

### Scoring System
- **Win**: +200 points
- **Draw**: +50 points (each player)
- **Loss**: 0 points

### Gameplay
1. Player 1 is assigned **X** (Red)
2. Player 2 is assigned **O** (Blue)
3. Players take turns placing their symbol
4. First to get 3 in a row (horizontal, vertical, or diagonal) wins
5. If all cells are filled with no winner, it's a draw

### Win Conditions
- 3 in a row horizontally
- 3 in a column vertically
- 3 in a diagonal (either direction)

### Timeouts
- **Matchmaking timeout**: 30 seconds
- **Inactive player**: 3 minutes without activity
- **Game timeout**: 10 minutes without moves

## ⚙️ Configuration

### Database Configuration

#### H2 (Development)
```properties
spring.datasource.url=jdbc:h2:mem:tictactoe
spring.datasource.username=sa
spring.datasource.password=
```

Access H2 Console:
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:tictactoe`
- Username: `sa`
- Password: (leave empty)

#### PostgreSQL (Production)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tictactoe
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Environment Variables
```bash
# Database Configuration
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=tictactoe
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# CORS Configuration
export ALLOWED_ORIGINS=https://yourdomain.com

# Run with production profile
java -jar tictactoe.jar --spring.profiles.active=prod
```

### Application Profiles

#### Development
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Production
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 🐳 Deployment

### Docker Deployment

#### Create Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/tictactoe-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build Docker Image
```bash
docker build -t tictactoe:latest .
```

#### Run Container
```bash
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  tictactoe:latest
```

## 🧪 Testing

### Manual Testing

#### Test Scenario 1: Matchmaking
1. Open two browser windows
2. Enter different nicknames in each
3. Click "Start Playing" in both
4. Verify game starts automatically

#### Test Scenario 2: Gameplay
1. Start a game
2. Take turns making moves
3. Verify turn indicator updates correctly
4. Complete the game and check statistics

#### Test Scenario 3: Disconnect Handling
1. Start a game
2. Close one browser window abruptly
3. Verify other player sees game ended message

### API Testing with cURL

```bash
# Health check
curl http://localhost:8081/api/health

# Get leaderboard
curl http://localhost:8081/api/leaderboard?limit=5

# Get player stats
curl http://localhost:8081/api/player/TestPlayer

# Get system stats
curl http://localhost:8081/api/stats
```

### Unit Testing

Run all tests:
```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=GameServiceTest
```

## 🔧 Troubleshooting

### Common Issues

#### Issue 1: Port Already in Use
**Error**: `Port 8081 is already in use`

**Solution**:
```bash
# Find process using port 8081
lsof -i :8081

# Kill the process
kill -9 <PID>

# Or change port in application.properties
server.port=8082
```

#### Issue 2: Database Connection Failed
**Error**: `Unable to obtain JDBC Connection`

**Solution**:
- Verify database is running
- Check connection parameters
- Ensure database exists
- Verify credentials

#### Issue 3: WebSocket Connection Failed
**Error**: `Connection failed`

**Solution**:
- Check if server is running
- Verify firewall settings
- Check browser console for errors
- Ensure port 8081 is accessible

#### Issue 4: Player Stuck in Matchmaking
**Error**: "Finding opponent..." never ends

**Solution**:
- Click "Cancel" and try again
- Check server logs for errors
- Verify WebSocket connection is active
- Clear browser cache and reload

### Debug Mode

Enable detailed logging:
```properties
logging.level.com.tictactoe=TRACE
logging.level.org.springframework.messaging=TRACE
logging.level.org.springframework.web.socket=TRACE
```

View logs in real-time:
```bash
tail -f logs/tictactoe.log
```

## 📊 Performance Considerations

### Current Capacity
- **Concurrent Games**: 100+
- **Players**: 200+
- **Response Time**: < 100ms

### Optimization Tips
1. **Enable caching** for leaderboard queries
2. **Use connection pooling** for database
3. **Implement Redis** for distributed game state
4. **Add CDN** for static assets
5. **Enable Gzip compression**

### Monitoring

Add Spring Boot Actuator endpoints:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Access metrics:
- http://localhost:8081/actuator/health
- http://localhost:8081/actuator/metrics
- http://localhost:8081/actuator/info

## 🔒 Security Considerations

### Current Security
- ✅ CORS configuration
- ✅ Input validation (basic)
- ✅ Exception handling
- ✅ SQL injection prevention (JPA)
- ❌ No authentication
- ❌ No authorization
- ❌ No rate limiting

### Production Recommendations
1. Implement JWT authentication
2. Add OAuth2 support
3. Enable HTTPS
4. Add rate limiting
5. Implement CSRF protection
6. Add security headers
7. Enable audit logging

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Use Lombok for boilerplate code
- Add JavaDoc for public methods
- Write unit tests for new features
- Keep methods small and focused


## 👥 Authors

- **Your Name** - *Initial work* - [GitHub](https://github.com/raaghu2002)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- SockJS and STOMP.js for WebSocket support
- All contributors who helped improve this project

## 📧 Contact

- **Email**: raaghu2002@gmail.com
- **LinkedIn**: [Profile](https://linkedin.com/in/raghavendra2002)
- **GitHub**: [Profile](https://github.com/raaghu2002)

## 📈 Project Status

**Current Version**: 1.0.0  
**Status**: ✅ Production Ready  
**Last Updated**: October 2025

---

Made with ❤️ using Spring Boot



