# 휴대폰만으로 APK 만들기

현재 ZIP은 Android 소스 프로젝트입니다. 이 환경에는 Android SDK/Gradle이 없고 외부 다운로드도 막혀 있어 APK 자체를 여기서 컴파일할 수 없습니다. 대신 GitHub Actions가 클라우드에서 APK를 빌드하도록 설정했습니다.

## 갤럭시에서 진행
1. GitHub 계정으로 로그인합니다.
2. 브라우저에서 https://github.com/new 를 열어 새 저장소를 만듭니다. 이름은 `MontertoImageViewer`로 하고 Public 또는 Private 중 선택한 뒤 생성합니다.
3. 저장소 화면에서 **Add file → Upload files**를 선택합니다.
4. ZIP을 휴대폰에서 먼저 압축 해제한 다음, 압축 해제된 `MontertoImageViewer` 폴더 안의 파일과 폴더를 저장소에 업로드합니다. `.github` 폴더도 반드시 포함해야 합니다.
5. **Commit changes**를 누릅니다. 업로드 후 저장소의 **Actions** 탭에서 `Build Android APK` 실행이 시작됐는지 확인합니다. 시작되지 않았다면 **Build Android APK → Run workflow**를 누릅니다.
6. 실행이 초록색 체크로 끝나면 해당 실행을 열고 아래쪽 **Artifacts**에서 `monterto-image-viewer-debug-apk`를 다운로드합니다.
7. 내려받은 APK ZIP을 압축 해제하고 `app-debug.apk`를 눌러 설치합니다. Android가 안내하면 이 브라우저/파일 앱의 **알 수 없는 앱 설치 허용**을 켭니다.

빌드가 실패하면 Actions 실행 화면의 오류 로그를 보내 주세요.
