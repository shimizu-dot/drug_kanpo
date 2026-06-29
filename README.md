# 漢方PDF取込アプリ (kanpo)

漢方製剤PDFを取り込み、抽出内容を確認・編集・検索できるSpring Bootアプリケーションです。

## 主要機能

- PDFアップロードによる漢方製剤情報の抽出
- 抽出内容の確認・編集・保存
- コード／成分／摘要による検索
- 全件一覧表示
- H2 バックアップ機能

## 技術スタック

- Java 21
- Spring Boot 4.x (スナップショット)
- Thymeleaf
- Spring Web MVC
- MyBatis
- H2 Database
- Apache PDFBox
- Lombok

## 前提条件

- JDK 21
- H2 Database
- Docker不要（ローカル実行）

## セットアップ

1. 依存関係を取得して起動

```bash
./mvnw spring-boot:run
```

## 実行方法

### macOS / Linux

```bash
./mvnw spring-boot:run
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

起動後に次のURLを開きます。

- `http://localhost:8080/kampo/import`
- `http://localhost:8080/kampo/search`

### ポート変更

8080が使用中の場合は、以下のように別ポートで起動できます。

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 環境変数

アプリは `application.properties` で次の環境変数をサポートします。

- `SPRING_DATABASE_URL`
- `SPRING_DATABASE_USERNAME`
- `SPRING_DATABASE_PASSWORD`
- `SPRING_SQL_INIT_MODE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JDBC_DATABASE_URL`
- `JDBC_DATABASE_USERNAME`
- `JDBC_DATABASE_PASSWORD`
- `PORT`

ローカル起動時のデフォルトは `jdbc:h2:file:./data/kanpo` です。

## Renderデプロイ

このリポジトリには `render.yaml` が含まれています。Renderでのデプロイ時は以下の環境変数をRenderダッシュボードで設定してください。

- `SPRING_DATABASE_URL`
- `SPRING_DATABASE_USERNAME`
- `SPRING_DATABASE_PASSWORD`
- `SPRING_SQL_INIT_MODE`

`SPRING_SQL_INIT_MODE` は通常 `always` を指定し、起動時に `schema.sql` を自動適用します。

## 画面構成

- `取込` : PDFアップロードと抽出
- `確認・編集` : 抽出内容の確認・編集
- `検索` : コード／成分／摘要検索
- `一覧` : 全件表示
- `バックアップ` : SQLバックアップ出力

## 参考

- データ設計: `docs/kampo_table_design.md`
- スキーマ: `src/main/resources/schema.sql`

---

## 既知の注意点

- `application.properties` に未設定の場合、デフォルトで H2 ファイル DB `./data/kanpo` を使用します。
- Render で運用する場合は、H2 の永続化方針を別途決めてください。
