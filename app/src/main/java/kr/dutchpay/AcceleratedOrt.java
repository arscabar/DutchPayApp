package kr.dutchpay;

import ai.onnxruntime.OrtSession.SessionOptions;
import java.util.Collections;

/**
 * 갤럭시 S26 Ultra 등 고성능 모바일 기기의 NPU 및 하드웨어 가속기(QNN/NNAPI/XNNPACK)를
 * 안전하게 적용하고, 미지원 환경에서는 점진적으로 CPU로 Fallback하는 SessionOptions 팩토리입니다.
 */
public final class AcceleratedOrt {
    public enum Backend {
        NPU_NNAPI("NPU/NNAPI 가속"),
        XNNPACK("XNNPACK(ARM NEON) 가속"),
        CPU("CPU 멀티스레드");

        public final String description;
        Backend(String description) { this.description = description; }
    }

    public static final class Config {
        public final SessionOptions options;
        public final Backend backend;

        public Config(SessionOptions options, Backend backend) {
            this.options = options;
            this.backend = backend;
        }
    }

    /**
     * 버킷 패딩에 사용할 고정 너비 목록 (NPU Static Shape 최적화용)
     */
    public static final int[] BUCKET_WIDTHS = {320, 480, 640, 960, 1280};

    /**
     * 임의의 가로 해상도를 NPU 처리에 적합한 고정 크기 버킷 너비로 올림합니다.
     */
    public static int bucketWidth(int width) {
        for (int b : BUCKET_WIDTHS) {
            if (width <= b) return b;
        }
        // 최대 버킷보다 크면 128의 배수로 올림하여 Shape 가짓수 최소화
        return ((width + 127) / 128) * 128;
    }

    /**
     * 사용 가능한 최적의 하드웨어 가속 옵션을 안전하게 생성합니다.
     * NPU/NNAPI -> XNNPACK -> CPU 멀티스레드 순으로 시도합니다.
     */
    public static Config createOptimalOptions() {
        // 1순위: NPU / NNAPI 하드웨어 가속 시도
        try {
            SessionOptions options = new SessionOptions();
            options.setIntraOpNumThreads(2);
            options.addNnapi();
            return new Config(options, Backend.NPU_NNAPI);
        } catch (Throwable nnapiFailure) {
            // NPU/NNAPI 실패 시 다음 단계로
        }

        // 2순위: XNNPACK (ARM NEON 벡터 가속) 시도
        try {
            SessionOptions options = new SessionOptions();
            options.setIntraOpNumThreads(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
            try {
                options.addXnnpack(Collections.emptyMap());
            } catch (NoSuchMethodError | Exception e) {
                try {
                    SessionOptions.class.getMethod("addXnnpack").invoke(options);
                } catch (Throwable ignored) {
                    options.close();
                    throw new IllegalStateException("XNNPACK not supported");
                }
            }
            return new Config(options, Backend.XNNPACK);
        } catch (Throwable xnnpackFailure) {
            // XNNPACK 실패 시 CPU로 Fallback
        }

        // 3순위: 순수 CPU 멀티스레드 Fallback
        SessionOptions options = new SessionOptions();
        int threads = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        try {
            options.setIntraOpNumThreads(threads);
        } catch (Throwable ignored) {}
        return new Config(options, Backend.CPU);
    }
}
