package kr.dutchpay;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

public final class Ocr {
    public static TextRecognizer create() {
        return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
    }
}
