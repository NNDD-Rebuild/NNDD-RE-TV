# NNDD-RE TV

NNDD-RE(ニコニコ動画ダウンローダー、PC版はElectron製)のAndroid TV版。起動するとQRコードを表示し、同一Wi-Fi内のスマホ/PCブラウザで読み取って操作する。TVアプリ自体がニコニコ動画APIを直接叩くクライアントであり、ブラウザは操作パネルとして機能する。バックエンドサーバーは存在しない(TVアプリ内蔵のHTTPサーバーのみ)。

## 主な機能

- **QRペアリング**: TV画面のQRコードをスマホ/PCブラウザで読み取るだけで接続。複数端末から同時にペアリング可能、QRはトップページに常時表示され期限切れ時も自動更新される。
- **ニコニコアカウントログイン**: ブラウザのフォームからID/パスワードを入力(2段階認証対応)。ログインセッションは端末再起動後も保持される。
- **ランキング / 検索 / マイリスト**: TVはD-padのフォーカス操作で一覧・選択。検索語の入力はブラウザ側で行う(表示順は再生回数順)。
- **動画再生**: ExoPlayerによるHLS再生+ニコニコ互換コメント描画。画質切り替え、D-pad操作(再生/一時停止・±10秒シーク)に対応。
- **ブラウザ操作パネル**: トップページに再生中の動画の操作(一時停止・シークバー・画質切替)を統合表示。別画面への遷移は不要。
- **PC版NNDD-REライブラリ連携**: PC版で稼働しているローカル共有サーバー(デフォルトポート12345)に接続し、ダウンロード済みライブラリをTV・ブラウザ双方から閲覧・再生できる。接続先はブラウザの設定画面から入力する。

## 動作要件

- Android TV(実機またはエミュレータ、API 37相当)
- TVとスマホ/PCが同一Wi-Fi(LAN)上にあること
- (任意)PC版NNDD-REのライブラリ共有機能を使う場合は、PC側でサーバーを起動しておく

## セットアップ・ビルド

```bash
# ビルド(Git Bash / PowerShellどちらでも可)
./gradlew.bat assembleDebug

# Kotlinのinline関数(NicoHttpClient.postJson等)を変更した場合は
# インクリメンタルビルドが正しく反映されないことがあるためクリーンビルド推奨
./gradlew.bat clean assembleDebug
```

## インストール方法

ビルド済みAPKは [Releases](https://github.com/NNDD-Rebuild/NNDD-RE-TV/releases) から `app-debug.apk` をダウンロードできる(タグを作ってGitHub Releaseを公開すると、`.github/workflows/release.yml` が自動でビルドしてこのAPKを添付する)。自分でビルドする場合は `app/build/outputs/apk/debug/app-debug.apk` が生成される。

デバッグ署名のAPKなのでGoogle Playは使わず `adb` でサイドロードする。

1. Android TV側で開発者オプションを有効化: 設定 → デバイス情報 → ビルド番号を7回タップ
2. 設定 → 開発者オプション → 「USBデバッグ」(またはネットワーク経由のデバッグ)をON
3. PCとTVを接続する
   - USB接続の場合はそのまま `adb devices` で認識される
   - 同一Wi-Fi経由の場合、TVのIPアドレスを確認して `adb connect <TVのIP>:5555`
4. インストール・起動:

```bash
adb install -r app-debug.apk

adb shell am start -n com.nnddrebuild.nndd_re_tv/.MainActivity
# インストール直後はGoogle TVのセットアップ画面が前面に来ることがあるため、
# startコマンドを再度実行しないとアプリが表示されない場合がある
```

エミュレータで動作確認する場合、TVアプリの内蔵サーバー(既定ポート23456)へホストPCから直接アクセスするには `adb forward tcp:<任意のローカルポート> tcp:23456` でポートフォワードし、`http://localhost:<ポート>/` へアクセスする。

## 使い方

1. TVアプリを起動するとQRコードが表示される。
2. 同一Wi-Fi内のスマホ/PCブラウザでQRコードを読み取る(または表示されているURLへ直接アクセス)。
3. ペアリングが完了するとTV側はトップページ(ランキング/検索/マイリスト/設定などの導線)に遷移する。ブラウザ側のトップページにも同じ導線とペアリング用QRが表示される。
4. 必要に応じてブラウザの「ニコニコアカウントでログイン」からログインする(未ログインでもランキング・検索・PC版ライブラリの閲覧は可能)。
5. ブラウザで動画を選び「TVで再生」を押すとTV側で再生が始まり、ブラウザのトップページにその場で操作パネル(一時停止・シーク・画質切替)が表示される。
6. PC版NNDD-REのライブラリを使う場合は、ブラウザの設定画面からPC側のIPアドレスとポート(既定12345)を入力・保存する。接続済みならホーム画面に「NNDD-REライブラリ」の導線が表示される。

## アーキテクチャ概要

```
app/src/main/java/com/nnddrebuild/nndd_re_tv/
├── nico/            ニコニコ本体APIを直接叩くクライアント層(TVアプリの中核)
│   ├── NicoHttpClient.kt    OkHttp+CookieJar+kotlinx.serializationの共通HTTPラッパー
│   ├── NicoCookieJar.kt     user_session等のCookie保持(メモリ内)
│   ├── NicoCookieStore.kt   ログインCookieのDataStore永続化
│   ├── auth/                フォームログイン代行・MFA(NicoFormLoginClient, AuthManager)
│   └── ranking/ search/ mylist/ watch/ comment/  各API機能ごとのクライアント
├── library/         PC版NNDD-REライブラリ共有サーバーのクライアント層(nico/とは独立)
├── server/          Ktor CIO内蔵HTTPサーバー(ブラウザ向け)
│   ├── NnddTvServer.kt      サーバー起動、ルーティング登録の起点
│   ├── PairingManager.kt    QRペアリングのトークン発行・セッション管理
│   ├── BrowserNavigateBus.kt TV操作をブラウザの画面遷移として伝える単方向バス
│   └── routes/               /api/* のエンドポイント定義
├── playback/        再生コマンド/状態をTV UIとブラウザ間で伝えるバス(PlaybackCommandBus/PlaybackStateBus)
└── ui/screen/       TV側Compose UI。各画面はserver経由ではなくnico/・library/クライアントを直接呼ぶ
    ├── pairing/ home/ ranking/ search/ mylist/ library/ settings/
    └── player/          Media3 ExoPlayer + 独自ニコニコ互換コメントオーバーレイ

app/src/main/assets/web/   ブラウザ側UI。フレームワークレスの素のHTML/CSS/JS
```

より詳細な設計・データフローの説明は [CLAUDE.md](./CLAUDE.md) を参照。

## 既知の制約

- `androidx.tv:tv-material` のコンポーネントはフォーカスベース操作が前提で、タッチタップには反応しない(D-padでのフォーカス移動→決定が必要)。
- PC版ライブラリのサムネイル表示は、TVとブラウザが同一LAN上の実IPアドレスで到達し合える環境を前提にしている(Androidエミュレータでの検証時、TV側から見たホストPCのアドレスとブラウザから見たアドレスが異なるため、エミュレータ環境ではサムネイルのみ表示されないことがある)。
- 共有ライブラリ・PC版共有サーバーはいずれも同一LAN内であれば無認証でアクセスできる設計。外部ネットワークへの公開は想定していない。
