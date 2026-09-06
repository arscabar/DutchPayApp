# DutchPay

한국어 영수증을 휴대폰에서 읽고, 선택한 품목의 수량·비율·할인을 적용해 비고를 만드는 Android 앱입니다. 현재 버전은 **v0.7 OCR 시제품**이며 Android 8.0(API 26) 이상에서 실행됩니다.

## 현재 기능

- 사진 선택 후 품목명·단가·수량·행 금액 추출, 원본과 OCR 결과 확인
- 항목 선택, 적용 수량·비율 편집, 할인·쿠폰·구성품 제거를 별도 행으로 처리
- 인쇄 합계·추출 합계·선택 합계 비교, 비고 미리보기·복사
- 메뉴 영역 재인식과 후보 선택, 누락 수량 재인식, 불확실한 항목 확인 표시

인쇄된 행 금액을 보존하고, 수량·비율에 따른 안분은 원 단위 HALF_UP으로 계산합니다. 이미 반영된 할인은 중복 차감하지 않습니다. 수량을 읽지 못하면 빈칸으로 남기며, 원본에서 확인할 수 없는 값을 합계에 맞춰 채우지 않습니다.

**Notion API 전송, 앱 내 직접 촬영, 편집 내용 영구 저장은 아직 구현하지 않았습니다.** 사진을 바꾸거나 앱이 종료되면 편집 내역이 사라집니다. 목표는 선택 내역의 합계를 Notion 한 행에, 계산 상세를 비고에 저장하는 것입니다.

## 인식 방식

Paddle v6 small 검출기와 v5 한국어 인식기를 기본으로 사용하며, ML Kit 한국어 인식과 소형 영문 모델로 필요한 영역을 다시 확인합니다. 모델은 APK에 포함되고 앱에 인터넷 권한이 없어 사진 처리는 기기 내부에서 수행됩니다.

v0.7 검증에서 누락 수량은 25개에서 0개로 줄었고, 메뉴 이름 5개 행을 개선했습니다. 다만 일부 메뉴 오독은 남아 있습니다. 측정은 Android 에뮬레이터에서 수행했으며 실제 ARM 휴대폰의 속도는 아직 측정하지 않았습니다. 표본·평가 기준·처리 시간은 [검증 결과](docs/VALIDATION.md)를 참고하세요.

## 준비 및 빌드

JDK 17 이상, Python 3.9 이상, Android SDK Platform 35와 Build-Tools 36.0.0이 필요합니다. 저장소 루트의 `local.properties`에 설치 경로를 지정합니다.

```properties
sdk.dir=C:/Android/Sdk
```

처음에는 인터넷 연결로 Gradle 의존성과 고정 버전 모델·SDK를 준비합니다. 다음 명령은 저장소 루트에서 순서대로 실행합니다.

```powershell
python -m pip install PyYAML
python scripts/prepare_paddle_detector.py
python scripts/prepare_korean_model.py
python scripts/prepare_v6_detector.py
python scripts/menu_english_setup.py --install
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
python -m unittest discover -s scripts -p 'evaluate_menu*_test.py'
```

macOS/Linux에서는 `./gradlew`를 사용합니다. APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 모델이 포함되어 v0.7 디버그 APK 크기는 약 281MB입니다.

준비 스크립트는 `.tools/paddle-sdk`와 앱·테스트 모델 assets를 생성하고 다운로드 파일의 해시를 검증합니다. 이 과정에서 영수증을 업로드하지 않습니다. Windows Java의 `Unable to establish loopback connection` 오류는 빌드 프로세스의 `JAVA_TOOL_OPTIONS`에 `-Djdk.net.unixdomain.tmpdir=존재하지않는경로`를 지정해 우회할 수 있습니다.

## 소스와 검증 자료

| 경로 | 내용 |
|---|---|
| `app/src/main` | Android 화면, OCR, 표 해석, 금액 계산 |
| `app/src/test` | 사진 없이 실행하는 JVM 단위 테스트 |
| `app/src/androidTest` | 기기·에뮬레이터에서 실행하는 OCR·화면 테스트 |
| `app/src/paddleTest` | `-PfullPaddle`로 추가하는 비교 실험 |
| `scripts`, `experiments` | 모델 준비, 평가, 개발 당시 실험 도구 |

원본 사진·거래별 정답·OCR 기록(`evidence/`), 모델, SDK 다운로드, APK는 Git에 포함하지 않습니다. 실제 영수증 테스트는 지정된 비공개 원본과 정답을 요구하므로 임의 사진으로 같은 수치를 재현할 수 없습니다. 일부 연구 스크립트는 개발 환경의 로컬 경로를 사용합니다. 공개 소스만으로 실행 가능한 검증은 위의 JVM·Python 테스트이며, 과거 배치 평가에는 별도 데이터와 경로 설정이 필요합니다.
