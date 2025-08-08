# Payamresan  
*A Distributed Java Socket-Based Messaging System*

---

## 📖 Introduction

**Payamresan** (Persian for *"Messenger"*) is a robust, multi-component messaging application built entirely from scratch using **Java Sockets**.  
It features a **distributed architecture** designed for scalability, separating coordination, execution, and user interaction into distinct, independent applications.

This project demonstrates core concepts of **network programming**, **multithreading**, and **system design** in Java, providing a practical example of how a distributed messaging service can be architected.

---

## ✨ Features

- **Distributed Architecture**  
  Composed of three independent applications:
  1. **Central Server**
  2. **Host Server(s)**
  3. **Clients**

- **User Authentication**  
  Secure registration and login system.

- **Dynamic Chat Rooms (Workspaces)**  
  Users can create isolated chat environments.

- **Real-Time One-on-One Messaging**  
  Instant text communication inside workspaces.

- **Chat History & Management**  
  - `get-chats`: View all conversations and unread counts.  
  - `get-messages`: Retrieve full conversation history.

- **Data Persistence**  
  JSON-based state saving for Central & Host Servers on shutdown, auto-loaded on startup.

- **Command-Line Interface (CLI)**  
  User-friendly terminal-based interface.

---

## 🏗️ System Architecture

The architecture is **modular** and **scalable**. Core components:

### 1️⃣ Central Server (The Brain)
- Runs on **port 8000**.
- Manages **user registration**, **authentication**, and **workspace allocation**.
- Registers Host Servers and assigns workspaces.
- Provides workspace connection details to clients.
- **Does not** handle chat messages directly.

### 2️⃣ Host Server (The Workhorse)
- Multiple instances can run on different machines.
- Registers itself with the Central Server (port range provided).
- Manages multiple **isolated workspaces**.
- Handles:
  - Message routing
  - Storage
  - History retrieval

### 3️⃣ Client (The User Interface)
- CLI app for user interaction.
- Communicates with Central Server for authentication & workspace info.
- Connects directly to Host Server for chat.
- Sends/receives real-time messages.
- Manages session state.

---

## 🛠️ Tech Stack

- **Language:** Java (JDK 21)  
- **Build Tool:** Apache Maven  
- **Networking:** Java Sockets API  
- **Concurrency:** Java Threads, ExecutorService  
- **Data Storage:** JSON (Google Gson)  
- **Logging:** SLF4J + Logback  
- **Boilerplate Reduction:** Lombok  

---

## 🚀 How to Run

### Prerequisites
- Java JDK 21+
- Apache Maven
- Git

---

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/payamresan-project.git
cd payamresan-project
