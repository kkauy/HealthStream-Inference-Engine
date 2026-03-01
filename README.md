🏥 HealthStream Inference Engine
📌 Overview
HealthStream is an enterprise-grade, asynchronous machine learning orchestration system. Built to bridge robust backend infrastructure with high-performance ML inference, it utilizes Java Spring Boot for reliable job scheduling and queue management to orchestrate isolated Python FastAPI workers.

Designed with production-readiness in mind, this project implements distributed system patterns (inspired by EKS architectures and Kafka-style idempotency) and features a complete observability stack.

🏗️ System Architecture & Design Philosophy
The system is divided into 4 decoupled layers to ensure scalability, fault tolerance, and process isolation:

The Orchestrator (Java / Spring Boot)

Manages an asynchronous, idempotent Job Queue to prevent duplicate processing under high concurrent loads.

Utilizes Java Thread Pools and ProcessBuilder to spawn and manage lifecycle states of ML worker processes.

Inference Workers (Python / FastAPI)

Containerized, stateless endpoints serving predictive models (e.g., Breast Cancer diagnostics, Sleep Disorder analysis).

Event-Driven Idempotency Layer

Implements a custom BlockingQueue mechanism mimicking Kafka consumer idempotency to ensure reliable message streaming and exactly-once processing.

Production Observability Stack

Metrics: Micrometer integration exposing metrics to Prometheus (e.g., worker latency, success rates, exit codes).

Dashboards: Real-time system monitoring via Grafana.

Structured Logging: FluentBit pipeline for standardized JSON log aggregation.

🚀 Quick Start
The entire ecosystem is fully containerized. You can spin up the orchestrator, inference workers, and the observability stack with a single command.

Prerequisites
Docker & Docker Compose

Run the System
Bash
# Clone the repository
git clone https://github.com/kkauy/HealthStream-Inference-Engine.git
cd healthstream-inference-engine

# Build and start all services
docker-compose up --build -d
🧪 Quality Assurance & Integration Testing
To maintain system reliability across the Java/Python microservice boundary, this project implements a keyword-driven automated testing suite.

Framework: Robot Framework

Logic: Validates API contracts (HTTP status codes, JSON schema) and ensures the end-to-end inference flow remains consistent across deployments.

Why Robot Framework? It provides a high-level abstraction that makes integration tests human-readable and provides clear HTML logs for fast debugging of distributed failures.

Execute Tests:
Bash
# Install test dependencies
pip install robotframework robotframework-requests

# Run the integration suite
robot tests/api_integration.robot
🧪 Inspiration & Background
This project is a synthesis of industrial engineering patterns and rigorous independent scientific research:

Domain Logic & Research: The core inference models are based on my independent research in Clinical Data Analytics, specifically focusing on Breast Cancer Diagnostics and Behavioral Analysis using high-dimensional datasets.

Orchestration Pattern (Enterprise-Grade): Inspired by worker management and EKS deployment patterns observed in high-scale ML platforms (e.g., Apple-style Prism), ensuring robust process isolation.

Reliability Standards: Leverages idempotency and null-safety logic typical of enterprise message streaming (e.g., Hilton-style Kafka consumers) to ensure data integrity and system stability.