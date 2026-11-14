"""
Script to generate training, validation, and test datasets for Ji Commitment Classifier.
Generates multi-lingual sentences (English, Hindi Devanagari, Hinglish) across:
- Positive Commitments (with Category: Professional, Personal, Parcel, Urgent)
- Negative Non-Commitments (Event statements, third-person statements, questions, past tense, casual text)
"""

import json
import random
import os

SEED = 42
random.seed(SEED)

def get_templates():
    # --- POSITIVE COMMITMENTS ---
    # Professional
    prof_en = [
        "I will email the client deck by {time}",
        "I'll submit the PR before {time}",
        "I will send the quarterly report at {time}",
        "Will review the document at {time}",
        "I'll join the team sync at {time}",
        "I will finish the Jira tickets by {time}",
        "I promise to send the invoice by {time}",
        "I'll ping you after the standup at {time}",
        "Will send the updated resume by {time}",
        "I will update the spreadsheet by {time}",
        "I'll coordinate with the product manager at {time}",
        "I will schedule the design review at {time}",
        "Will share the presentation slides by {time}",
        "I will complete the bug fixes before {time}",
        "I'll push the production deployment at {time}",
        "Will follow up with the vendor by {time}",
        "I will email the meeting minutes at {time}",
        "I'll wrap up the sprint backlog by {time}",
        "Will share the API documentation before {time}",
        "I will send the contract draft by {time}",
        "I'll send the audit logs at {time}",
        "I will deliver the sprint demo at {time}",
        "Will send the architecture diagram by {time}",
        "I'll complete the code review by {time}",
        "I will write the test cases before {time}",
    ]
    prof_hi_lat = [
        "kal subah meeting mein presentation de dunga {time}",
        "main kal subah {time} report share karunga",
        "kal boss ko file bhej dunga {time}",
        "kal tak draft bhejta hoon {time}",
        "main {time} ko code push kar dunga",
        "kal shaam ko call karke status bataunga {time}",
        "main {time} document review karke feedback dunga",
        "kal client meeting prepare karunga {time}",
        "kal {time} document sign karke de dunga",
        "main parso project review complete karunga {time}",
        "kal subah HR se baat karke update deta hoon {time}",
        "main {time} baje tak PR approve kar dunga",
        "shaam {time} baje tak report mail kar dunga",
        "main {time} tak client ko call kar lunga",
        "kal subah {time} baje sync meeting attend karunga",
        "main {time} baje testing complete kar dunga",
        "parso {time} baje boss ko presentation dikhaunga",
        "main {time} baje tak release notes submit kar dunga",
        "kal {time} baje manager se 1-on-1 schedule karunga",
        "main {time} baje tak saari files upload kar dunga",
    ]
    prof_hi_dev = [
        "मैं कल सुबह {time} रिपोर्ट भेज दूंगा",
        "कल शाम {time} बजे मीटिंग की प्रेजेंटेशन तैयार कर दूंगा",
        "मैं {time} बजे तक कोड रिव्यू पूरा कर लूंगा",
        "कल {time} बजे क्लाइंट को ईमेल कर दूंगा",
        "मैं कल शाम {time} बजे बॉस को प्रोजेक्ट स्टेटस दे दूंगा",
        "कल दोपहर {time} बजे तक सारे डाक्यूमेंट्स भेज दूंगा",
        "मैं {time} बजे टीम के साथ सिंक कॉल ज्वाइन कर लूंगा",
    ]

    # Personal
    pers_en = [
        "I'll call you at {time}",
        "I will call mom at {time}",
        "I'll pick up kids from school at {time}",
        "Will meet you at the cafe at {time}",
        "I'll pay the electricity bill by {time}",
        "I will go to the gym at {time}",
        "Will buy groceries at {time}",
        "I'll take my medicine at {time}",
        "I will book the movie tickets at {time}",
        "Will wish him happy birthday at {time}",
        "I'll transfer the rent money by {time}",
        "I will water the plants at {time}",
        "I'll cook dinner for everyone at {time}",
        "Will take the dog for a walk at {time}",
        "I'll reach your place by {time}",
        "I will clean the room at {time}",
        "Will meet the dentist at {time}",
        "I'll return the library book by {time}",
        "I will call the plumber at {time}",
        "Will book train tickets at {time}",
    ]
    pers_hi_lat = [
        "main kal shaam {time} milta hoon",
        "main {time} baje call karta hoon",
        "kal gym jaunga {time}",
        "shaam ko dinner pe chalte hain {time}",
        "kal doctor ke paas jaana hai {time}",
        "main raat ko {time} phone karta hoon",
        "kal movie dekhne chalte hain {time}",
        "parso subah mummy ko call karunga {time}",
        "main {time} baje tak rent transfer kar dunga",
        "kal shaam ko tennis khelenge {time}",
        "main {time} baje dawai le lunga",
        "kal dost se milkar books wapas karunga {time}",
        "main {time} baje grocery lene jaunga",
        "kal sham ko chai pe milte hain {time}",
        "main {time} baje bike service karwane jaunga",
        "kal subah {time} baje running ke liye jaunga",
        "main {time} baje room saaf karunga",
        "kal {time} baje ticket book karunga",
    ]
    pers_hi_dev = [
        "मैं तुम्हें कल शाम {time} बजे फोन करूंगा",
        "कल दोपहर {time} बजे डॉक्टर से मिलने जाऊंगा",
        "मैं {time} बजे मम्मी को कॉल करूंगा",
        "कल शाम {time} बजे जिम जाऊंगा",
        "मैं {time} बजे तक बिजली का बिल भर दूंगा",
        "कल सुबह {time} बजे मंदिर जाऊंगा",
        "मैं {time} बजे दवाई ले लूंगा",
    ]

    # Parcel
    parcel_en = [
        "I will collect the Amazon parcel at {time}",
        "I'll pick up the Flipkart courier at {time}",
        "I will receive the delivery at {time}",
        "Will collect the Swiggy order at {time}",
        "I'll drop off the return parcel by {time}",
        "Will pick up the package from reception at {time}",
        "I will give the delivery OTP at {time}",
        "I'll collect the medicine package at {time}",
        "Will meet the courier agent at {time}",
        "I will check the gate for the parcel at {time}",
        "I'll pick up the food delivery at {time}",
        "Will receive the shipment at {time}",
    ]
    parcel_hi_lat = [
        "delivery wale ko {time} call karke OTP dunga",
        "courier boy aa raha hai {time} parcel le lunga",
        "kal shaam ko delivery collect karunga {time}",
        "Swiggy order {time} collect karna hai",
        "I'll pick up medicine parcel {time}",
        "courier pickup ke liye {time} ghar par rahunga",
        "kal {time} baje Amazon package receive karunga",
        "Flipkart delivery {time} aayegi main le lunga",
        "Zomato delivery boy ko {time} baje gate pe milunga",
        "main {time} baje parcel collect kar lunga",
    ]
    parcel_hi_dev = [
        "मैं {time} बजे अमेज़न पार्सल कलेक्ट कर लूंगा",
        "कल दोपहर {time} बजे कूरियर रिसीव कर लूंगा",
        "मैं {time} बजे डिलीवरी वाले को ओटीपी दे दूंगा",
        "कल शाम {time} बजे पार्सल पिक कर लूंगा",
    ]

    # Urgent
    urgent_en = [
        "Urgent: I must submit this report before {time}",
        "Emergency: need to reach hospital by {time}",
        "Urgent: I will take my insulin shot at {time}",
        "ASAP: fix server downtime before {time}",
        "Very urgent: pay credit card bill before {time}",
        "Urgent reminder: board flight at {time}",
        "I must urgently transfer the funds by {time}",
        "Critical task: submit assignment before {time}",
        "Urgent client escalation call at {time}, I must join",
        "Immediately call the doctor at {time}",
        "Urgent: submit visa documents by {time}",
        "Critical: renew car insurance before {time}",
    ]
    urgent_hi_lat = [
        "jaldi karo {time} deadline hai submit karna hai",
        "urgent: {time} baje hospital pahunchna hai",
        "very urgent: {time} baje se pehle bill pay karna hai",
        "urgent call hai client ke saath {time} baje attend karna hai",
        "bahut zaroori hai {time} baje tak fee jama karni hai",
        "emergency: {time} baje doctor ke paas jana hai",
        "urgent: {time} baje tak flight pakadni hai",
        "urgent meeting hai {time} baje miss nahi kar sakta",
    ]
    urgent_hi_dev = [
        "अति आवश्यक: {time} बजे तक रिपोर्ट जमा करनी है",
        "इमरजेंसी: मुझे {time} बजे तक अस्पताल पहुंचना है",
        "अर्जेंट: {time} बजे तक बिल का भुगतान करना है",
        "बहुत जरूरी है: {time} बजे डॉक्टर से मिलना है",
    ]

    # --- NEGATIVE NON-COMMITMENTS ---
    # 1. Event / Third-party statements with time
    event_statements = [
        "The train leaves at {time}",
        "The flight lands at {time}",
        "Match starts at {time}",
        "Office timing is 9 am to 6 pm",
        "The movie starts at {time}",
        "Store closes at {time}",
        "Sunset is at {time} today",
        "Doctor is available only after {time}",
        "Metro starts at {time} tomorrow",
        "Sale ends on Sunday at {time}",
        "The bank opens at {time}",
        "Lunch break is at {time}",
        "Class begins at {time}",
        "The concert begins at {time}",
        "Library closes at {time}",
        "Breakfast is served until {time}",
        "Museum entry closes at {time}",
        "The bus arrives at {time}",
        "The meeting is scheduled at {time}",
        "Conference starts at {time}",
        "The webinar is at {time}",
        "Results will be announced at {time}",
        "It is {time} right now",
        "Current time is {time}",
        "Traffic is usually heavy around {time}",
        # Hinglish
        "train {time} baje aayegi",
        "office {time} baje khulta hai",
        "kal chhutti hai",
        "match {time} baje shuru hoga",
        "flight {time} baje land karegi",
        "dukaan {time} baje band ho jaati hai",
        "movie ka show {time} baje hai",
        "metro {time} baje chalegi",
        "doctor {time} baje ke baad hi milega",
        "kal Sunday hai",
        "aaj {time} baje baarish ho sakti hai",
        "yeh dukan shaam {time} baje tak khuli rehti hai",
        # Hindi Devanagari
        "ट्रेन {time} बजे आएगी",
        "दुकान {time} बजे बंद हो जाती है",
        "मूवी का शो {time} बजे शुरू होगा",
        "कल छुट्टी है",
        "ऑफिस का समय सुबह 9 बजे से शाम 6 बजे तक है",
        "फ्लाइट {time} बजे लैंड करेगी",
    ]

    # 2. Third person actions
    third_person = [
        "Rohan is going to Delhi at {time}",
        "She said the presentation is at {time}",
        "He will call you at {time}",
        "They are launching the product at {time}",
        "The delivery boy will arrive around {time}",
        "Amit is visiting the doctor at {time}",
        "Priya mentioned she will come at {time}",
        "The manager will take the interview at {time}",
        "The guest is arriving at {time}",
        "Rahul is booking the tickets at {time}",
        # Hinglish
        "Rohan kal {time} baje aayega",
        "mummy {time} baje mandir jayengi",
        "bhaiya {time} baje call karenge",
        "woh log {time} baje nikal rahe hain",
        "sir {time} baje class lenge",
        "delivery boy {time} baje aayega bola",
        "priya {time} baje meeting attend karegi",
        # Hindi Devanagari
        "रोहन कल {time} बजे आएगा",
        "वह {time} बजे फोन करेगा",
        "मम्मी {time} बजे मंदिर जाएंगी",
        "सर {time} बजे क्लास लेंगे",
    ]

    # 3. Questions
    questions = [
        "What time is the meeting?",
        "Are you coming at {time}?",
        "When does the train arrive?",
        "Can you send it by {time}?",
        "Is the store open at {time}?",
        "Did you get the email at {time}?",
        "Will you be free at {time}?",
        "Are we meeting at {time}?",
        "Who is presenting at {time}?",
        "Where should we meet at {time}?",
        # Hinglish
        "kya tum {time} baje aa rahe ho?",
        "kitne baje milna hai?",
        "kya meeting {time} baje hai?",
        "kya tum {time} baje free ho?",
        "aap kab aaoge?",
        "train kitne baje aayegi?",
        "kya kal office jaana hai?",
        # Hindi Devanagari
        "क्या आप {time} बजे आ रहे हैं?",
        "मीटिंग कितने बजे शुरू होगी?",
        "क्या कल छुट्टी है?",
        "ट्रेन कितने बजे आएगी?",
    ]

    # 4. Past tense
    past_tense = [
        "I called you yesterday at {time}",
        "We completed the task at {time}",
        "I had lunch at {time}",
        "He arrived 2 hours ago",
        "The parcel was delivered at {time}",
        "I sent the email at {time}",
        "We met yesterday at {time}",
        "I submitted the report at {time}",
        "The payment was completed at {time}",
        "The doctor called at {time}",
        # Hinglish
        "kal maine {time} baje call kiya tha",
        "maine {time} baje report bhej di thi",
        "hum kal {time} baje mile the",
        "parcel {time} baje deliver ho gaya tha",
        "maine kal {time} baje mail kar diya tha",
        "woh 2 ghante pehle aaya tha",
        "maine {time} baje baat ki thi",
        # Hindi Devanagari
        "मैंने कल शाम {time} बजे फोन किया था",
        "हम कल {time} बजे मिले थे",
        "रिपोर्ट {time} बजे भेज दी गई थी",
        "पार्सल दोपहर {time} बजे आ गया था",
    ]

    # 5. Casual chat without time or commitment
    casual = [
        "Hello how are you doing",
        "Haan bhai theek hai",
        "Thanks a lot for your help",
        "Good morning have a nice day",
        "Let me know what you think",
        "Haha that was so funny",
        "Okay see you soon",
        "The weather is very nice today",
        "Sure no problem at all",
        "Bahut badhiya laga dekh kar",
        "Yes got it",
        "Okay noted",
        "See you tomorrow",
        "Talk to you later",
        "Sounds good",
        "All the best for your exam",
        "Great job team",
        "Congratulations on the promotion",
        "Koi baat nahi",
        "Haan bilkul",
        "Main ghar par hoon",
        "Chalo theek hai",
        "Good night sweet dreams",
        "Have safe travels",
        "Please take care",
        "बहुत अच्छा लगा",
        "नमस्ते कैसे हैं आप",
        "धन्यवाद",
        "शुभ रात्रि",
    ]

    return {
        "pos": {
            "Professional": (prof_en, prof_hi_lat, prof_hi_dev),
            "Personal": (pers_en, pers_hi_lat, pers_hi_dev),
            "Parcel": (parcel_en, parcel_hi_lat, parcel_hi_dev),
            "Urgent": (urgent_en, urgent_hi_lat, urgent_hi_dev),
        },
        "neg": {
            "events": event_statements,
            "third_person": third_person,
            "questions": questions,
            "past": past_tense,
            "casual": casual,
        }
    }

TIMES_EN = ["5 pm", "tomorrow at 10 am", "in 2 hours", "next Monday at 3 pm", "6:30 pm", "tomorrow morning", "tonight at 9", "noon", "midnight", "8 am", "4 pm", "11:30 am", "tomorrow 2 pm"]
TIMES_HI_LAT = ["5 baje", "kal shaam 6 baje", "2 ghante mein", "parso subah 10 baje", "raat 9 baje", "dopahar 2 baje", "subah 8 baje", "kal dopahar 3 baje", "shaam 7:30 baje"]
TIMES_HI_DEV = ["5 बजे", "कल शाम 6 बजे", "2 घंटे में", "परसों सुबह 10 बजे", "रात 9 बजे", "दोपहर 2 बजे", "सुबह 8 बजे"]

def generate_samples():
    templates = get_templates()
    samples = []

    # 1. Generate Positive Commitments
    for cat, (en_list, hi_lat_list, hi_dev_list) in templates["pos"].items():
        for tpl in en_list:
            for t in TIMES_EN:
                samples.append({
                    "text": tpl.format(time=t),
                    "is_commitment": 1,
                    "category": cat
                })
        for tpl in hi_lat_list:
            for t in TIMES_HI_LAT:
                samples.append({
                    "text": tpl.format(time=t),
                    "is_commitment": 1,
                    "category": cat
                })
        for tpl in hi_dev_list:
            for t in TIMES_HI_DEV:
                samples.append({
                    "text": tpl.format(time=t),
                    "is_commitment": 1,
                    "category": cat
                })

    pos_count = len(samples)
    print(f"Generated {pos_count} positive commitment samples.")

    # 2. Generate Negative Non-Commitments
    neg_samples = []
    # Events
    for tpl in templates["neg"]["events"]:
        t_list = TIMES_HI_DEV if any('\u0900' <= c <= '\u097f' for c in tpl) else (TIMES_HI_LAT if "baje" in tpl or "kal" in tpl else TIMES_EN)
        for t in t_list:
            neg_samples.append({
                "text": tpl.format(time=t) if "{time}" in tpl else tpl,
                "is_commitment": 0,
                "category": "None"
            })
    # Third Person
    for tpl in templates["neg"]["third_person"]:
        t_list = TIMES_HI_DEV if any('\u0900' <= c <= '\u097f' for c in tpl) else (TIMES_HI_LAT if "baje" in tpl or "kal" in tpl else TIMES_EN)
        for t in t_list:
            neg_samples.append({
                "text": tpl.format(time=t) if "{time}" in tpl else tpl,
                "is_commitment": 0,
                "category": "None"
            })
    # Questions
    for tpl in templates["neg"]["questions"]:
        t_list = TIMES_HI_DEV if any('\u0900' <= c <= '\u097f' for c in tpl) else (TIMES_HI_LAT if "baje" in tpl or "kal" in tpl else TIMES_EN)
        for t in t_list:
            neg_samples.append({
                "text": tpl.format(time=t) if "{time}" in tpl else tpl,
                "is_commitment": 0,
                "category": "None"
            })
    # Past
    for tpl in templates["neg"]["past"]:
        t_list = TIMES_HI_DEV if any('\u0900' <= c <= '\u097f' for c in tpl) else (TIMES_HI_LAT if "baje" in tpl or "kal" in tpl else TIMES_EN)
        for t in t_list:
            neg_samples.append({
                "text": tpl.format(time=t) if "{time}" in tpl else tpl,
                "is_commitment": 0,
                "category": "None"
            })
    # Casual
    for txt in templates["neg"]["casual"]:
        neg_samples.append({
            "text": txt,
            "is_commitment": 0,
            "category": "None"
        })

    # Replicate casual to have good balance
    for _ in range(5):
        for txt in templates["neg"]["casual"]:
            neg_samples.append({
                "text": txt,
                "is_commitment": 0,
                "category": "None"
            })

    print(f"Generated {len(neg_samples)} negative samples.")
    all_samples = samples + neg_samples
    random.shuffle(all_samples)

    # Deduplicate by text
    seen = set()
    deduped = []
    for s in all_samples:
        clean = s["text"].strip().lower()
        if clean not in seen:
            seen.add(clean)
            deduped.append(s)

    random.shuffle(deduped)
    print(f"Total deduplicated samples: {len(deduped)}")

    # Split: 80% train, 10% val, 10% test
    n = len(deduped)
    n_train = int(n * 0.8)
    n_val = int(n * 0.1)

    train_data = deduped[:n_train]
    val_data = deduped[n_train:n_train + n_val]
    test_data = deduped[n_train + n_val:]

    print(f"Train: {len(train_data)}, Val: {len(val_data)}, Test: {len(test_data)}")

    os.makedirs("ml/data", exist_ok=True)
    for name, data in [("train.jsonl", train_data), ("val.jsonl", val_data), ("test.jsonl", test_data)]:
        path = os.path.join("ml/data", name)
        with open(path, "w", encoding="utf-8") as f:
            for item in data:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
        print(f"Saved {path}")

if __name__ == "__main__":
    generate_samples()

