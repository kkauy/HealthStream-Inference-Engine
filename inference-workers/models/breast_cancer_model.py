import pandas as pd
import numpy as np
import joblib
import os

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'pipeline.joblib')

def predict(data_path)

    try:
        # load pipeline
        pipe = joblib.load(MODEL_PATH )

        # load data
        df = pd.read_csv(data_path)

        # data preprocessing
        if "Unnamed: 32" in df.columns:"
            df = df.drop(columns=["Unnamed: 32"])

        if "id" in df.columns:
            df = df.drop(columns=["id"])
        else:
            X =df

    prediction = pipe.predict(x)[0]
    probability = pipe.predict_proba(X)[0][1]

    return {
        "status": "success",
        "diagnosis": "Malignant" if prediction == 1 else "Benign",
        "confidence": round(float(probability, 4),
        "model_info": "Breast Cancer analysis using Logistic Regression"
    }

    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }