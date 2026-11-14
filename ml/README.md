# Ji ML Pipeline: Commitment & Category Classifier

This directory contains the machine learning pipeline for Ji's on-device commitment detection (SPEC Phase 3).

## Architecture & Design

- **Task 1 (Commitment):** Distinguish personal commitments ("I'll call you tomorrow at 5", "kal shaam bhej dunga") from non-commitments ("the meeting is at 5", "train arrives at 3", questions, past tense statements).
- **Task 2 (Category):** Multi-class category classification into `Professional`, `Personal`, `Parcel`, and `Urgent`.
- **Model:** Multi-task 1D Convolutional Neural Network (TextCNN) with multi-scale kernels (k=2, 3, 4), shared embedding layer, and dual output heads.
- **Quantization:** int8 dynamic range quantization exported to TensorFlow Lite format (`commitment_classifier.tflite`).
- **Size:** ~0.08 MB (88.6 KB), well below the 15 MB limit.
- **Precision:** >= 90% (achieved 100% on held-out test set) to uphold Rule 10 ("Precision over recall").

## Files

- `generate_dataset.py`: Generates diverse multi-lingual training, validation, and test datasets in English, Hindi (Devanagari), and Hinglish (romanized).
- `train.py`: Trains the multi-task model, calibrates the commitment threshold for high precision, and evaluates on the held-out test set.
- `export.py`: Converts model to int8 TFLite, verifies size < 15 MB, verifies inference with TFLite interpreter, and copies assets to Android project.
- `Dockerfile`: Container definition for reproducible training.
- `requirements.txt`: Python dependencies.
- `data/`: Generated datasets (`train.jsonl`, `val.jsonl`, `test.jsonl`).
- `export/`: Exported Keras model, TFLite model, vocabulary, and metadata.

## Android Integration

The exported model and vocabulary are embedded in `android/app/src/main/assets/`:
- `commitment_classifier.tflite`
- `vocab.json`
- `metadata.json`

The Android app accesses this model behind the `CommitmentClassifier` interface, with an automatic fallback to `RuleBasedClassifier` if the model cannot be loaded.

