application_id := "com.gomsoo.saigil"

# 레시피 목록
default:
    @just --list

# debug 앱 빌드 + 테스트 코드 컴파일
[group('build')]
build: (_build "false")

# debug 앱 빌드
[group('build')]
build-app:
    ./gradlew :app:assembleDebug

# release 앱 빌드 (루트에 keystore.properties가 있으면 release 키로 서명, 없으면 unsigned)
[group('build')]
build-release:
    ./gradlew :app:assembleRelease

# 전체 모듈 clean
[group('build')]
clean:
    ./gradlew clean

# clean + build를 gradle 한 번 호출로 실행
[group('build')]
clean-build: (_build "true")

# 테스트 코드 컴파일 (unit test, androidTest). 테스트를 실행하지는 않음
[group('build')]
compile-test:
    ./gradlew :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin

_build clean:
    ./gradlew {{ if clean == "true" { "clean " } else { "" } }}:app:assembleDebug :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin

# 기기를 쓰는 레시피는 마지막 인자로 기기 serial(`adb devices`의 첫 열)을 받는다. 예: just install emulator-5554
# adb와 AGP의 install/uninstall/connected task가 모두 ANDROID_SERIAL을 따르므로 그 환경 변수로 기기를 고른다.
# serial을 생략하면 ANDROID_SERIAL을 건드리지 않아, 셸에 이미 설정한 값이 있으면 그대로 쓰인다.

# debug 앱 설치 후 실행
[group('install')]
install serial="": (_install "false" serial)

# debug 앱 삭제
[group('install')]
uninstall serial="":
    {{ if serial != "" { "ANDROID_SERIAL=" + serial + " " } else { "" } }}./gradlew :app:uninstallDebug

# debug 앱 삭제 후 재설치하고 실행
[group('install')]
reinstall serial="": (_install "true" serial)

# launcher activity 이름에 의존하지 않도록 monkey로 실행한다.
_install reinstall serial:
    {{ if serial != "" { "ANDROID_SERIAL=" + serial + " " } else { "" } }}./gradlew {{ if reinstall == "true" { ":app:uninstallDebug " } else { "" } }}:app:installDebug
    {{ if serial != "" { "ANDROID_SERIAL=" + serial + " " } else { "" } }}adb shell monkey -p {{ application_id }} -c android.intent.category.LAUNCHER 1 > /dev/null

# 전체 unit test 실행
[group('test')]
test-unit:
    ./gradlew :app:testDebugUnitTest

# 전체 android test 실행 (연결된 기기 또는 에뮬레이터 필요)
[group('test')]
test-android serial="":
    {{ if serial != "" { "ANDROID_SERIAL=" + serial + " " } else { "" } }}./gradlew :app:connectedDebugAndroidTest

# unit test와 android test 모두 실행 (연결된 기기 또는 에뮬레이터 필요)
[group('test')]
test-all serial="":
    {{ if serial != "" { "ANDROID_SERIAL=" + serial + " " } else { "" } }}./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest

# lint 실행
[group('test')]
lint:
    ./gradlew :app:lintDebug
