import sys
import argparse
import time
import json

def run_medical_inference(task, data_path):
    # log -> stderr
    print(f"[Python-Worker] Initializing inference for task: {task}...", file=sys.stderr)

    time.sleep(1)

    if task == "breast_cancer":
        result = {"diagnosis": "Malignant", "confidence": 0.98, "features": 30}
    elif task == "sleep_depression":
        result = {"status": "High Risk", "latency_ms": 140, "trend": "Increasing"}
    else:
        result = {"error": "Unknown task type"}

    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="HealthStream Inference Worker")
    parser.add_argument("--task", type=str, required=True)
    parser.add_argument("--input", type=str, required=True)

    args = parser.parse_args()

    result = run_medical_inference(args.task, args.input)

    # stdout -> ONE LINE JSON only
    print(json.dumps(result, separators=(",", ":")))