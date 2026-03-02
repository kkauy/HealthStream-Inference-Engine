🏥 HealthStream Inference Engine
📌 Overview
A Java-based orchestrator that automates Machine Learning inference by managing isolated Python processes.
I built this project to solve a real-world problem: how to reliably run heavy Python ML models (like Breast Cancer diagnostics) from a stable Java Spring Boot backend without crashing the main server.

Instead of just calling a script, I implemented a process-management approach inspired by how big systems handle isolated tasks. It's built to be portable, easy to test, and scalable.

🏗️ How it Works (Architecture)
The project is split into two main parts to keep things clean and modular:

1. The Java Orchestrator (Spring Boot)
   Process Management: Uses ProcessBuilder to spin up Python workers on demand. I chose this over JNI for Process Isolation—if the ML model leaks memory, it doesn't take down the Java API.

Thread Safety: Implemented a fixed thread pool to control how many ML jobs run at once, preventing CPU exhaustion.

Dynamic Pathing: No hardcoded paths! The system automatically locates the Python environment relative to the project root.

2. The ML Workers (Python)
   Scikit-learn Models: Handles the actual math. Currently supports Breast Cancer and Behavioral Analysis tasks.

CLI-Driven: Communicates via a clean CLI contract (--task, --input), returning structured JSON that Java can easily parse.

🚀 Getting Started
I've Dockerized the whole setup so anyone can run it without worrying about "it works on my machine."

Quick Start:

Bash
git clone https://github.com/kkauy/HealthStream-Inference-Engine.git
cd healthstream-inference-engine
docker-compose up --build
🧪 Testing & Reliability
I didn't want to just "hope" it works, so I added an integration layer:

Robot Framework: I used this for automated API testing because it’s human-readable. It checks if the Java-to-Python bridge is actually returning the right diagnostic JSON.

Error Handling: Captured STDERR from Python and mapped it to Java exceptions to make debugging 10x easier during development.

Bash
# To run the tests:
pip install robotframework robotframework-requests
robot tests/api_integration.robot

🧠 Why I Built This (My Journey)
Finding the Bridge Between Data Science and Engineering
This project started from my personal interest in Clinical Data Analytics. I had these raw Python models for Breast Cancer diagnostics, but I realized that in the real world, a model is only useful if it’s part of a stable, accessible service.

The Real-World Challenge: "The Java-Python Gap"
The biggest hurdle wasn't the ML itself—it was the Data Contract.

The Struggle: Initially, I had issues with Python scripts crashing or Java failing to read the output because of pathing and stream buffering.

The Learning: I had to dive deep into ProcessBuilder, learning how to handle STDOUT and STDERR properly so that Java doesn't "hang" while waiting for Python. It taught me a lot about Resource Management and why process isolation is so important in a Spring environment.

The Goal: "Simple but Robust"
I wanted to build something that feels like a "Mini-Orchestrator". While big companies use complex EKS setups, I wanted to see if I could achieve that same level of Reliability and Isolation on a smaller scale. My goal was to create a system that a small team could easily maintain, but is "production-ready" enough to handle real diagnostic data safely.

🚧 Challenges & Lessons Learned
The Zombie Process Issue: In early versions, if the Python script crashed, the Java process would stay "alive" in the background. I had to learn how to implement proper Process Destruction and timeout handling to keep the system clean.

Stream Buffering: I realized that if Python prints too much data, the ProcessBuilder buffer fills up and the whole app freezes. This led me to implement a dedicated Thread to consume the output stream, which was a huge "Aha!" moment for me regarding asynchronous I/O.

Path Portability: Moving between my local Mac and a Docker container broke the absolute paths. I refactored the logic to use Relative Discovery, making the entire engine "Plug and Play."
