# 使用 Spring AI 開發 MCP Server 教學

## 下載專案


從 Spring Initializr 下載專案 [範例專案](https://start.spring.io/#!type=gradle-project&language=java&platformVersion=3.4.4&packaging=jar&jvmVersion=21&groupId=io.github.samzhu&artifactId=gcp-auditmate-mcp-server&name=gcp-auditmate-mcp-server&description=Demo%20project%20for%20Spring%20Boot&packageName=io.github.samzhu.auditmate&dependencies=web,spring-ai-mcp-server,native,devtools,sbom-cyclone-dx)

下載後解壓縮，使用 Visual Studio Code 開啟。

## 開啟專案


``` bash
gcloud auth application-default login
gcloud config set project csd-ct-lab
```


``` bash
sdk install java 24.1.2.r23-nik

sdk use java 24.1.2.r23-nik
```

通過以下命令確認 native-image 已正確安裝
``` bash
native-image --version
```

通過以下命令編譯 native image executable 並在 build/native/nativeCompile 目錄下生成可執行文件
``` bash
./gradlew clean nativeCompile

./build-native.sh
```

build/native/nativeCompile/gcp-auditmate-mcp-server

/Users/samzhu/workspace/github-samzhu/gcp-auditmate-mcp-server/build/native/nativeCompile/gcp-auditmate-mcp-server



路徑

/Users/samzhu/Library/Logs/Claude/mcp.log
/Users/samzhu/Library/Logs/Claude/mcp-server-gcp-auditmate-mcp-server.log



dubug

找出
``` bash
lsof -i :8080
```

下載執行檔 到你習慣的目錄 例如 /Users/samzhu

```
https://blog.samzhu.dev/gcp-auditmate-mcp-server/gcp-auditmate-mcp-server
```

接著打開 Claude desktop 設定檔, 例如我的 MAC 上路徑如下 `/Users/samzhu/Library/Application Support/Claude/claude_desktop_config.json`

新增以下設定, 把 gcp-auditmate-mcp-server 新增到 mcpServers 設定中, command 記得改成你下載的執行檔路徑

``` json
{
    "mcpServers": {
        "gcp-auditmate-mcp-server": {
            "command": "/Users/samzhu/gcp-auditmate-mcp-server"
        }
    }
}
```

接著打開 Claude desktop, 你會看到 gcp-auditmate-mcp-server 已經在 mcpServers 設定中, 查看會像這樣

