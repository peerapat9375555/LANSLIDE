import pandas as pd
import numpy as np
import joblib
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, accuracy_score, precision_score, recall_score, f1_score
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
import os

print("=" * 70)
print("  LANDSLIDE MODEL COMPARISON — วัดที่ Recall เป็นหลัก")
print("=" * 70)

print("\n[1/5] Loading dataset...")
df = pd.read_csv('Landslide_Final_Cleaned_V2.csv')
TARGET_COL = 'Geohaz_E'

features_to_use = [
    'CHIRPS_Day_1', 'CHIRPS_Day_2', 'CHIRPS_Day_3', 'CHIRPS_Day_4', 'CHIRPS_Day_5',
    'CHIRPS_Day_6', 'CHIRPS_Day_7', 'CHIRPS_Day_8', 'CHIRPS_Day_9', 'CHIRPS_Day_10',
    'Elevation_Extracted', 'Slope_Extracted', 'Aspect_Extracted',
    'MODIS_LC', 'NDVI', 'NDWI', 'TWI', 'Soil_Type',
    'Road_Zone',
    'Rain_3D_Prior', 'Rain_5D_Prior', 'Rain_7D_Prior', 'Rain_10D_Prior',
    'Rain3D_x_Slope', 'Rain5D_x_Slope', 'Rain7D_x_Slope', 'Rain10D_x_Slope'
]

print(f"   Dataset shape: {df.shape}")
print(f"   Target distribution:\n{df[TARGET_COL].value_counts().to_string()}")

print("\n[2/5] Feature Engineering & Balancing classes...")

# Compute derived features if not already in CSV
if 'Rain_3D_Prior' not in df.columns:
    df['Rain_3D_Prior'] = df['CHIRPS_Day_1'] + df['CHIRPS_Day_2'] + df['CHIRPS_Day_3']
if 'Rain_5D_Prior' not in df.columns:
    df['Rain_5D_Prior'] = df['Rain_3D_Prior'] + df['CHIRPS_Day_4'] + df['CHIRPS_Day_5']
if 'Rain_7D_Prior' not in df.columns:
    df['Rain_7D_Prior'] = df['Rain_5D_Prior'] + df['CHIRPS_Day_6'] + df['CHIRPS_Day_7']
if 'Rain_10D_Prior' not in df.columns:
    df['Rain_10D_Prior'] = df['Rain_7D_Prior'] + df['CHIRPS_Day_8'] + df['CHIRPS_Day_9'] + df['CHIRPS_Day_10']

if 'Rain3D_x_Slope' not in df.columns:
    df['Rain3D_x_Slope'] = df['Rain_3D_Prior'] * df['Slope_Extracted']
if 'Rain5D_x_Slope' not in df.columns:
    df['Rain5D_x_Slope'] = df['Rain_5D_Prior'] * df['Slope_Extracted']
if 'Rain7D_x_Slope' not in df.columns:
    df['Rain7D_x_Slope'] = df['Rain_7D_Prior'] * df['Slope_Extracted']
if 'Rain10D_x_Slope' not in df.columns:
    df['Rain10D_x_Slope'] = df['Rain_10D_Prior'] * df['Slope_Extracted']

numeric_cols = df.select_dtypes(include=[np.number]).columns
df[numeric_cols] = df[numeric_cols].fillna(0)

X = df[features_to_use].fillna(df[features_to_use].median(numeric_only=True))
y = df[TARGET_COL]

# *** FIX: Fit scaler on ORIGINAL (unbalanced) data ***
# เพราะ mean/std ต้องมาจากข้อมูลจริง ไม่ใช่ข้อมูลที่ upsample แล้ว
# ถ้า fit บน balanced data → scaler จะ shift ค่าผิดเมื่อเอาไปใช้กับข้อมูลจริงในแอป
print("\n[3/5] Scaling features (fit on ORIGINAL data)...")
scaler = StandardScaler()
scaler.fit(X)  # fit บนข้อมูลจริง ก่อน balance
print(f"   Scaler fitted on {len(X)} original samples (real-world distribution)")

df_combined = pd.concat([X, y], axis=1)
majority = df_combined[df_combined[TARGET_COL] == 0.0]
minority = df_combined[df_combined[TARGET_COL] == 1.0]
minority_upsampled = minority.sample(n=len(majority), replace=True, random_state=42)
df_balanced = pd.concat([majority, minority_upsampled])

X_bal = df_balanced[features_to_use]
y_bal = df_balanced[TARGET_COL]

X_train, X_test, y_train, y_test = train_test_split(X_bal, y_bal, test_size=0.3, random_state=42, stratify=y_bal)

print(f"   Train: {len(X_train)} | Test: {len(X_test)}")

# Transform ใช้ scaler ที่ fit จากข้อมูลจริง
X_train_scaled = scaler.transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Try importing optional libraries
try:
    from xgboost import XGBClassifier
    HAS_XGB = True
except ImportError:
    HAS_XGB = False
    print("   ⚠ xgboost not installed, skipping XGBoost")

try:
    from lightgbm import LGBMClassifier
    HAS_LGB = True
except ImportError:
    HAS_LGB = False
    print("   ⚠ lightgbm not installed, skipping LightGBM")

try:
    from catboost import CatBoostClassifier
    HAS_CAT = True
except ImportError:
    HAS_CAT = False
    print("   ⚠ catboost not installed, skipping CatBoost")

print("\n[4/5] Training & Evaluating Models (Primary metric: RECALL)...")

models = {
    "Decision Tree": DecisionTreeClassifier(random_state=42),
    "Random Forest": RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1),
    "Gradient Boosting": GradientBoostingClassifier(n_estimators=200, random_state=42),
    "Logistic Regression": LogisticRegression(max_iter=1000, random_state=42),
}

if HAS_XGB:
    models["XGBoost"] = XGBClassifier(use_label_encoder=False, eval_metric='logloss', n_estimators=200, random_state=42)
if HAS_LGB:
    models["LightGBM"] = LGBMClassifier(n_estimators=200, random_state=42, verbose=-1)
if HAS_CAT:
    models["CatBoost"] = CatBoostClassifier(iterations=200, verbose=0, random_state=42)

results = []
trained_models = {}
best_model = None
best_recall = -1
best_model_name = ""

for name, model in models.items():
    print(f"   -> Training {name}...", end=" ", flush=True)
    model.fit(X_train_scaled, y_train)
    y_pred = model.predict(X_test_scaled)
    
    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, zero_division=0)
    rec = recall_score(y_test, y_pred, zero_division=0)
    f1 = f1_score(y_test, y_pred, zero_division=0)
    
    trained_models[name] = model
    
    results.append({
        "Model": name,
        "Recall": f"{rec*100:.2f}%",
        "Precision": f"{prec*100:.2f}%",
        "F1-Score": f"{f1*100:.2f}%",
        "Accuracy": f"{acc*100:.2f}%",
        "_recall_val": rec
    })
    
    print(f"Recall={rec*100:.2f}%  Precision={prec*100:.2f}%  F1={f1*100:.2f}%")
    
    if rec > best_recall:
        best_recall = rec
        best_model = model
        best_model_name = name

# Print Comparison Table (Sorted by Recall)
results_df = pd.DataFrame(results).sort_values(by="_recall_val", ascending=False).drop(columns=["_recall_val"])
print("\n" + "=" * 70)
print("  MODEL COMPARISON (Sorted by Recall)")
print("=" * 70)
print(results_df.to_string(index=False))

print(f"\n🏆 Best Model: {best_model_name} (Recall: {best_recall*100:.2f}%)")

# Classification Report for best model
print(f"\n--- Classification Report ({best_model_name}) ---")
y_pred_best = best_model.predict(X_test_scaled)
print(classification_report(y_test, y_pred_best, target_names=["No Landslide (0)", "Landslide (1)"]))

# Probability Distribution Analysis — ดูว่า threshold 0.35/0.70 เหมาะสมไหม
print("\n" + "=" * 70)
print("  PROBABILITY DISTRIBUTION ANALYSIS (for threshold calibration)")
print("=" * 70)
try:
    test_probs = best_model.predict_proba(X_test_scaled)[:, 1]
    y_test_arr = y_test.values
    
    no_ls_probs = test_probs[y_test_arr == 0.0]
    ls_probs = test_probs[y_test_arr == 1.0]
    
    print(f"\n  Non-Landslide samples (class 0): {len(no_ls_probs)}")
    print(f"    Mean prob:   {no_ls_probs.mean():.4f}")
    print(f"    Median prob: {np.median(no_ls_probs):.4f}")
    print(f"    Max prob:    {no_ls_probs.max():.4f}")
    print(f"    % < 0.35:    {(no_ls_probs < 0.35).sum() / len(no_ls_probs) * 100:.1f}%")
    print(f"    % < 0.50:    {(no_ls_probs < 0.50).sum() / len(no_ls_probs) * 100:.1f}%")
    
    print(f"\n  Landslide samples (class 1): {len(ls_probs)}")
    print(f"    Mean prob:   {ls_probs.mean():.4f}")
    print(f"    Median prob: {np.median(ls_probs):.4f}")
    print(f"    Min prob:    {ls_probs.min():.4f}")
    print(f"    % >= 0.35:   {(ls_probs >= 0.35).sum() / len(ls_probs) * 100:.1f}%")
    print(f"    % >= 0.70:   {(ls_probs >= 0.70).sum() / len(ls_probs) * 100:.1f}%")
    
    # Threshold simulation
    print(f"\n  --- Threshold Simulation ---")
    for t_med, t_high in [(0.35, 0.70), (0.40, 0.75), (0.45, 0.80), (0.50, 0.85)]:
        low_correct = (no_ls_probs < t_med).sum()
        med_detected = ((ls_probs >= t_med) & (ls_probs < t_high)).sum()
        high_detected = (ls_probs >= t_high).sum()
        false_med = ((no_ls_probs >= t_med) & (no_ls_probs < t_high)).sum()
        false_high = (no_ls_probs >= t_high).sum()
        print(f"  Threshold Med={t_med}/High={t_high}: "
              f"Non-LS correctly Low={low_correct}/{len(no_ls_probs)} | "
              f"False Med={false_med} False High={false_high} | "
              f"LS detected Med={med_detected} High={high_detected}")
except Exception as e:
    print(f"  Could not compute probability analysis: {e}")

# Feature Importances for ALL models that support it
print("\n" + "=" * 70)
print("  FEATURE IMPORTANCE (Top 10 per Model)")
print("=" * 70)

for name, model in trained_models.items():
    if hasattr(model, 'feature_importances_'):
        importances = model.feature_importances_
        fi_df = pd.DataFrame({
            'Feature': features_to_use,
            'Importance': importances
        }).sort_values(by='Importance', ascending=False).reset_index(drop=True)
        
        marker = " 🏆" if name == best_model_name else ""
        print(f"\n--- {name}{marker} ---")
        for idx, row in fi_df.head(10).iterrows():
            bar = "█" * int(row['Importance'] * 50)
            print(f"  {idx+1:2d}. {row['Feature']:<22s} {row['Importance']:.4f} {bar}")
    elif name == "Logistic Regression":
        coef = np.abs(model.coef_[0])
        fi_df = pd.DataFrame({
            'Feature': features_to_use,
            'Importance': coef
        }).sort_values(by='Importance', ascending=False).reset_index(drop=True)
        
        print(f"\n--- {name} (|coefficients|) ---")
        for idx, row in fi_df.head(10).iterrows():
            bar = "█" * int(row['Importance'] / fi_df['Importance'].max() * 50)
            print(f"  {idx+1:2d}. {row['Feature']:<22s} {row['Importance']:.4f} {bar}")

# [5/5] Save best model
print("\n" + "=" * 70)
print("[5/5] Saving best model...")
joblib.dump(scaler, 'landslide_scaler.pkl')
joblib.dump(best_model, 'best_ml_model.pkl')
print(f"   ✅ Saved: best_ml_model.pkl ({best_model_name})")
print(f"   ✅ Saved: landslide_scaler.pkl")
print(f"\n   Model: {os.path.abspath('best_ml_model.pkl')}")
print(f"   Scaler: {os.path.abspath('landslide_scaler.pkl')}")
print("=" * 70)
