# CLAUDE.md

## 프로젝트 개요
Android 앱 ↔ macOS Electron 앱을 USB CDC / TCP Socket으로 연동하는 포트폴리오 프로젝트.

## 핵심 설계 의도
- DataTransport 인터페이스로 USB/TCP 교체 가능한 추상화 레이어 구현
- 탭 구조 앱의 모듈화 및 DI 강화 (포트폴리오용 다기능 앱)
- 확장성 고려: 향후 TCP/IP 외 다른 통신 방식 추가 가능한 구조

## 디렉토리 구조
- android-app/ : Kotlin, Google Maps SDK, USB Host API, TCP Socket
- electron-app/ : Electron, Maps JS API, serialport, net.Server
- docs/ : 아키텍처, 패킷 프로토콜 명세
- .github/workflows/ : Android / Electron CI 자동빌드

## 현재 개발 Phase
- Phase 1: 안드로이드 앱 리팩토링 (진행 예정)
  - DataTransport 인터페이스 설계 및 구현
  - UsbTransport / TcpTransport 구현체
  - 기존 Google Maps 코드 리팩토링
  - 탭 구조 DI 정리
- Phase 2: Electron 앱 빌드 (Phase 1 완료 후)
- Phase 3: 양단 연동 테스트

## 주요 기술 결정
- Electron 채택 이유: Maps JS API 직접 사용 가능, serialport 안정적, macOS/Windows 이식 용이
- PyQt6 대신 Electron 선택
- USB 자동 연결 (BroadcastReceiver), TCP는 설정 UI에서 IP/Port 입력

## 패킷 프로토콜
- docs/packet-protocol.md 참고
- STX + TYPE + LENGTH + PAYLOAD + ETX + CHECKSUM
- PAYLOAD: latitude(8) + longitude(8) + image_size(4) + image_data(N)

## API 키 관리
- Android: local.properties (gitignore)
- Electron: .env (gitignore)
- CI: GitHub Secrets → MAPS_API_KEY

## 작업 시 주의사항
- DataTransport 인터페이스 변경 시 양쪽 구현체 모두 확인
- 패킷 구조 변경 시 docs/packet-protocol.md 동기화
- .env, local.properties 절대 커밋 금지
