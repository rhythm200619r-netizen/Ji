"""
Export script for Ji Commitment and Category Classifier.
Converts trained Keras model to int8 TFLite model, checks size (< 15 MB),
and verifies predictions using TFLite Interpreter.
Copies model and assets to android/app/src/main/assets/.
"""

import json
import os
import shutil
import numpy as np
import tensorflow as tf
from sklearn.metrics import precision_score, recall_score, accuracy_score

MAX_SEQ_LEN = 32

def representative_dataset_gen(vocab):
    with open("ml/data/train.jsonl", "r", encoding="utf-8") as f:
        lines = f.readlines()[:100]
    for line in lines:
        item = json.loads(line)
        from train import encode_text
        ids = encode_text(item["text"], vocab, max_len=MAX_SEQ_LEN)
        # Yield input_ids as int32 tensor of shape (1, 32)
        yield [np.array([ids], dtype=np.int32)]

def main():
    print("Loading Keras model...")
    model = tf.keras.models.load_model("ml/export/model.keras")

    with open("ml/export/vocab.json", "r", encoding="utf-8") as f:
        vocab = json.load(f)

    with open("ml/export/metadata.json", "r", encoding="utf-8") as f:
        meta = json.load(f)

    threshold = meta["threshold"]
    print(f"Loaded threshold: {threshold}")

    print("\nConverting model to TFLite with int8 weight quantization...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    tflite_model = converter.convert()

    tflite_path = "ml/export/commitment_classifier.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)

    size_bytes = os.path.getsize(tflite_path)
    size_mb = size_bytes / (1024 * 1024)
    print(f"\nModel successfully exported to {tflite_path}")
    print(f"Model file size: {size_bytes} bytes ({size_mb:.2f} MB)")

    assert size_mb < 15.0, f"Model size ({size_mb:.2f} MB) exceeds 15 MB limit!"
    print(f"CHECK PASSED: Model size is under 15 MB ({size_mb:.2f} MB < 15.0 MB)")

    # Test TFLite Interpreter on Test Set
    print("\nTesting TFLite Interpreter on held-out test set...")
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"Input details: {input_details}")
    print(f"Output details: {output_details}")

    from train import load_data, encode_text, CATEGORIES

    test_data = load_data("ml/data/test.jsonl")
    y_true = [item["is_commitment"] for item in test_data]
    y_pred = []

    # Map output names to detail indices
    # Output indices: commitment (1 unit) and category (4 units)
    commit_idx = None
    cat_idx = None
    for detail in output_details:
        shape = detail["shape"]
        if shape[-1] == 1:
            commit_idx = detail["index"]
        elif shape[-1] == 4:
            cat_idx = detail["index"]

    assert commit_idx is not None and cat_idx is not None

    for item in test_data:
        ids = np.array([encode_text(item["text"], vocab, max_len=MAX_SEQ_LEN)], dtype=np.int32)
        interpreter.set_tensor(input_details[0]["index"], ids)
        interpreter.invoke()

        commit_prob = interpreter.get_tensor(commit_idx)[0][0]
        cat_probs = interpreter.get_tensor(cat_idx)[0]
        pred_label = 1 if commit_prob >= threshold else 0
        y_pred.append(pred_label)

    tflite_acc = accuracy_score(y_true, y_pred)
    tflite_prec = precision_score(y_true, y_pred, zero_division=0)
    tflite_rec = recall_score(y_true, y_pred, zero_division=0)

    print(f"TFLite Test Accuracy:  {tflite_acc * 100:.2f}%")
    print(f"TFLite Test Precision: {tflite_prec * 100:.2f}% (Target: >= 90.00%)")
    print(f"TFLite Test Recall:    {tflite_rec * 100:.2f}%")

    assert tflite_prec >= 0.90, f"TFLite precision ({tflite_prec}) fell below 90%!"
    print("CHECK PASSED: TFLite model achieved >= 90% precision on held-out test set!")

    # Copy assets to Android app assets directory
    android_assets_dir = "android/app/src/main/assets"
    os.makedirs(android_assets_dir, exist_ok=True)
    
    shutil.copy(tflite_path, os.path.join(android_assets_dir, "commitment_classifier.tflite"))
    shutil.copy("ml/export/vocab.json", os.path.join(android_assets_dir, "vocab.json"))
    shutil.copy("ml/export/metadata.json", os.path.join(android_assets_dir, "metadata.json"))

    print(f"\nCopied TFLite model and metadata to {android_assets_dir}/")
    print("Files in assets:", os.listdir(android_assets_dir))

if __name__ == "__main__":
    main()

