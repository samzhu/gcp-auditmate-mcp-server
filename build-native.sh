#!/bin/bash

# 設置 JAVA_TOOL_OPTIONS 環境變數增加記憶體
export JAVA_TOOL_OPTIONS="-Xmx12g -XX:MaxMetaspaceSize=1g"

# 清理並構建
./gradlew clean nativeCompile