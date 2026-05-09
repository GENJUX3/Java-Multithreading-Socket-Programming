# 🚀 Advanced Java Multithreaded Chat System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-GUI-blue?style=for-the-badge)
![Socket](https://img.shields.io/badge/Socket-Networking-green?style=for-the-badge)

A powerful, high-performance multithreaded chat application built with Java Socket Programming. This project offers two distinct versions: a lightweight **Terminal-based version** and a feature-rich, **Modern GUI version** with a custom-designed administration dashboard.

---

## ✨ Features

### 🖥️ GUI Version (Modern Administration)
*   **Modern UI/UX**: Custom-built Swing components with rounded edges, sleek tables, and a premium dark/light theme.
*   **User Management**: Full registration and login system with persistent data storage.
*   **Group Chat**: Dynamic group creation and management.
*   **Persistence**: Automatically saves user data (`users.db`), group info (`groups.db`), and chat logs (`chat_history.log`).
*   **Admin Dashboard**: Monitor active users, view login history, and manage group memberships in real-time.
*   **Networking**: Efficient TCP/IP communication using `ServerSocket` and `Socket`.

### 📟 Terminal Version (Basic)
*   **Lightweight**: Pure console-based interaction.
*   **Fast**: Ideal for low-resource environments.
*   **Multithreaded**: Supports multiple concurrent client connections via `ClientHandler`.

---

## 📁 Project Structure

The repository is organized into two main folders:

```text
├── 📂 GUI_Version/           # Feature-rich GUI version
│   ├── ChatServerApp.java    # Main Server with Admin Dashboard
│   ├── ChatClientApp.java    # Modern Client Application
│   ├── start_server.bat      # Quick-launch Server
│   ├── new_client.bat        # Quick-launch Client
│   └── *.db / *.log          # Data storage files
│
└── 📂 Terminal_Version/      # Lightweight console version
    ├── Server.java           # Basic Server
    ├── Client.java           # Basic Client
    ├── ClientHandler.java    # Multithreading logic
    ├── start_server.bat      # Quick-launch Server
    └── new_client.bat        # Quick-launch Client
```

---

## 🚀 Getting Started

### Prerequisites
*   **Java JDK 8 or higher** installed on your system.
*   Properly configured `JAVA_HOME` environment variable.

### How to Run

Running the application is extremely simple using the provided batch files:

#### 1️⃣ Start the Server:
Navigate to either the `GUI_Version` or `Terminal_Version` folder and run:
```bash
start_server.bat
```

#### 2️⃣ Launch Clients:
In the same folder, run as many clients as you want:
```bash
new_client.bat
```

---

## 🛠️ Built With
*   **Java SE**: Core logic and networking.
*   **Swing**: Custom modern GUI implementation.
*   **Socket Programming**: TCP/IP communication.
*   **Multithreading**: Concurrent client handling.

---

## 👨‍💻 Author
Developed with ❤️ for high-performance network programming.

---

> [!TIP]
> Make sure the server is running before launching any clients!
