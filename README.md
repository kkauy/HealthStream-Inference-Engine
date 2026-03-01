# 🏥 HealthStream Inference Engine

![Java](https://img.shields.io/badge/Java-Spring_Boot-green?style=flat-square&logo=spring)
![Python](https://img.shields.io/badge/Python-FastAPI-blue?style=flat-square&logo=fastapi)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![Observability](https://img.shields.io/badge/Prometheus-Grafana-orange?style=flat-square&logo=grafana)

## 📌 Overview
**HealthStream** is an enterprise-grade, asynchronous machine learning orchestration system. Built to bridge robust backend infrastructure with high-performance ML inference, it utilizes **Java Spring Boot** for reliable job scheduling and queue management, orchestrating isolated **Python FastAPI** workers.

Designed with production-readiness in mind, this project implements enterprise distributed system patterns (inspired by EKS architectures and Kafka-style idempotency) and features a complete observability stack.

## 🏗️ System Architecture & Design Philosophy
This system is divided into 4 decoupled layers to ensure scalability, fault tolerance, and process isolation:

1. **The Orchestrator (Java / Spring Boot)**
    * Manages an asynchronous, idempotent Job Queue to prevent duplicate processing under high concurrent loads.
    * Utilizes Java Thread Pools and `ProcessBuilder` to spawn and manage lifecycle states of ML worker processes.
2. **Inference Workers (Python / FastAPI)**
    * Containerized, stateless endpoints serving predictive models (e.g., Breast Cancer diagnostics, Sleep Disorder analysis).
3. **Event-Driven Idempotency Layer**
    * Implements a custom `BlockingQueue` mechanism mimicking Kafka consumer idempotency to ensure reliable message streaming and exactly-once processing.
4. **Production Observability Stack**
    * **Metrics:** Micrometer integration exposing metrics to **Prometheus** (e.g., worker latency, success rates, exit codes).
    * **Dashboards:** Real-time system monitoring via **Grafana**.
    * **Structured Logging:** **FluentBit** pipeline for standardized JSON log aggregation.

## 🚀 Quick Start

The entire ecosystem is fully containerized. You can spin up the orchestrator, inference workers, and the observability stack with a single command.

### Prerequisites
* Docker & Docker Compose

### Run the System
```bash
# Clone the repository
git clone [https://github.com/yourusername/healthstream-inference-engine.git](https://github.com/yourusername/healthstream-inference-engine.git)
cd healthstream-inference-engine

# Build and start all services
docker-compose up --build -d