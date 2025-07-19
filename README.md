Payamresan: A Distributed Java Socket-Based Messaging System
📖 Introduction
Payamresan (Persian for "Messenger") is a robust, multi-component messaging application built entirely from scratch using Java Sockets. It features a distributed architecture designed for scalability, separating coordination, execution, and user interaction into distinct, independent applications.

This project demonstrates core concepts of network programming, multithreading, and system design in Java, providing a practical example of how a distributed messaging service can be architected.

✨ Features
Distributed Architecture: The system is composed of three independent applications: a Central Server, one or more Host Servers, and multiple Clients.

User Authentication: Secure user registration and login system.

Dynamic Chat Rooms (Workspaces): Users can create isolated chat environments called "Workspaces".

Real-time One-on-One Messaging: Send and receive text messages in real-time within a workspace.

Chat History & Management:

get-chats: View a list of all conversations and the number of unread messages in each.

get-messages: Retrieve the full message history for a specific conversation.

Data Persistence: Both the Central Server and Host Servers save their state (users, workspaces, chat history) to JSON files upon graceful shutdown and reload the data on startup.

Command-Line Interface (CLI): A user-friendly CLI for interacting with the messaging service.

🏗️ System Architecture
The system's architecture is designed to be scalable. The core logic is divided into three main components:

1. Central Server
The "brain" of the entire system. It runs on a fixed port (8000) and acts as the central coordinator.

Responsibilities:

Manages user registration and authentication.

Registers and authenticates available Host servers.

Allocates new Workspaces to available Hosts.

Provides clients with the connection details (IP and Port) for specific Workspaces.

It does NOT handle any chat messages directly.

2. Host Server
The "workhorse" of the system. Multiple Host applications can run simultaneously on different machines.

Responsibilities:

Registers itself with the Central Server upon startup, providing a range of available ports.

Listens for commands from the Central Server.

Creates and manages multiple, isolated Workspace instances, each running on a dedicated port.

Handles all chat logic within its workspaces: message routing, storage, and history retrieval.

3. Client
A command-line application that serves as the user's entry point to the system.

Responsibilities:

Communicates with the Central Server for tasks like registration, login, and getting workspace addresses.

Establishes a persistent connection directly with a Host Server to join a Workspace.

Sends and receives chat messages in real-time.

Manages the user's session state (logged-in status).

🛠️ Tech Stack
Language: Java (JDK 21)

Build & Dependency Management: Apache Maven

Core Networking: Java Sockets API

Concurrency: Java Threads & ExecutorService

JSON Serialization: Google Gson

Logging: SLF4J + Logback

Boilerplate Reduction: Lombok

🚀 How to Run
Prerequisites
Java Development Kit (JDK) 21 or higher

Apache Maven

Git

1. Clone the Repository
git clone [https://github.com/your-username/payamresan-project.git](https://github.com/your-username/payamresan-project.git)
cd payamresan-project

2. Build the Projects
Each component is a separate Maven project. Build them all using the following commands from the root directory:

# Build the Central Server
mvn -f CentralServer/pom.xml clean install

# Build the Host Server
mvn -f Host/pom.xml clean install

# Build the Client
mvn -f Client/pom.xml clean install

3. Run the Applications
The components must be started in the following order. Open a new terminal for each component.

Step 1: Start the Central Server

java -jar CentralServer/target/central-server-1.0.0-SNAPSHOT.jar

You should see a log message indicating it's listening on port 8000.

Step 2: Start the Host Server
The Host requires 5 command-line arguments: <central_server_ip> <central_server_port> <host_ip> <host_start_port> <host_end_port>

java -jar Host/target/host-app-1.0.0-SNAPSHOT.jar 127.0.0.1 8000 127.0.0.1 9000 9999

You should see logs indicating a successful connection and registration with the Central Server.

Step 3: Start the Client(s)
You can run multiple instances of the client application to simulate a conversation.t

java -jar Client/target/client-app-

(
