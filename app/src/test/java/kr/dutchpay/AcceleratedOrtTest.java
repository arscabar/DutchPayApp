package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class AcceleratedOrtTest {

    @Test
    public void bucketWidthAlignsToFixedNpuShapes() {
        assertEquals(320, AcceleratedOrt.bucketWidth(1));
        assertEquals(320, AcceleratedOrt.bucketWidth(150));
        assertEquals(320, AcceleratedOrt.bucketWidth(320));
        assertEquals(480, AcceleratedOrt.bucketWidth(321));
        assertEquals(480, AcceleratedOrt.bucketWidth(480));
        assertEquals(640, AcceleratedOrt.bucketWidth(481));
        assertEquals(960, AcceleratedOrt.bucketWidth(800));
        assertEquals(1280, AcceleratedOrt.bucketWidth(1000));
        // 1280을 초과하는 매우 긴 텍스트는 128 단위로 올림 정렬
        assertEquals(1408, AcceleratedOrt.bucketWidth(1300));
    }

    @Test
    public void createOptimalOptionsSafelyReturnsUsableSessionOptions() {
        try {
            AcceleratedOrt.Config config = AcceleratedOrt.createOptimalOptions();
            assertNotNull(config);
            assertNotNull(config.options);
            assertNotNull(config.backend);
            assertTrue(config.backend.description.length() > 0);
            config.options.close();
        } catch (LinkageError e) {
            // 안드로이드 네이티브 JNI(.so)가 없는 호스트 데스크톱 JVM 환경에서의 안전한 통과
        }
    }
}
