import sys
import time
import json


def run_medical_inference(task, features):
    # log -> stderr
    print(f"[Python-Worker] Initializing inference for task: {task}...", file=sys.stderr)

    time.sleep(1)

    if task == "breast_cancer":
        return {
            "diagnosis": "Malignant",
            "confidence": 0.98,
            "features_used": len(features)
        }

    return {
        "error": "Unknown task type"
    }


def main():
    try:
        raw = sys.stdin.read().strip()

        if not raw:
            print(json.dumps({
                "ok": False,
                "error": "empty stdin request"
            }, separators=(",", ":")))
            sys.exit(1)

        req = json.loads(raw)

        request_id = req.get("id", "unknown")
        task = req.get("task")
        features = req.get("features")

        # Basic validation
        if not isinstance(task, str):
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "task must be a string"
            }, separators=(",", ":")))
            sys.exit(1)

        task = task.strip()
        if not task:
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "task cannot be empty"
            }, separators=(",", ":")))
            sys.exit(1)

        if task != "breast_cancer":
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "unsupported task"
            }, separators=(",", ":")))
            sys.exit(1)

        if not isinstance(features, list):
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "features must be an array"
            }, separators=(",", ":")))
            sys.exit(1)

        if len(features) != 30:
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "invalid feature vector length"
            }, separators=(",", ":")))
            sys.exit(1)

        if not all(isinstance(x, (int, float)) for x in features):
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": "all features must be numeric"
            }, separators=(",", ":")))
            sys.exit(1)

        result = run_medical_inference(task, features)

        if "error" in result:
            print(json.dumps({
                "ok": False,
                "id": request_id,
                "error": result["error"]
            }, separators=(",", ":")))
            sys.exit(1)

        print(json.dumps({
            "ok": True,
            "id": request_id,
            "task": task,
            "result": result
        }, separators=(",", ":")))
        sys.exit(0)

    except json.JSONDecodeError:
        print(json.dumps({
            "ok": False,
            "error": "json parse error in python worker"
        }, separators=(",", ":")))
        sys.exit(1)

    except Exception as e:
        print(json.dumps({
            "ok": False,
            "error": f"python worker exception: {str(e)}"
        }, separators=(",", ":")))
        sys.exit(1)


if __name__ == "__main__":
    main()