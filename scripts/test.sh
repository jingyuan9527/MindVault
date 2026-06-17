#!/usr/bin/env bash
# MindVault v0.1 集成测试脚本
# 用法: bash scripts/test.sh
# 前置条件: PostgreSQL 运行在 localhost:5432

set -e

BASE_URL="http://localhost:8080/api/v1"
PASS=0
FAIL=0

check() {
  local desc="$1"
  local expected="$2"
  local actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo "  PASS: $desc"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $desc (expected '$expected')"
    FAIL=$((FAIL + 1))
  fi
}

echo "====== MindVault v0.1 集成测试 ======"
echo ""

# 1. 健康检查 - 获取模型列表
echo "[1/8] 健康检查 (GET /models)"
RESP=$(curl -s "$BASE_URL/models")
check "返回 code=0" '"code":0' "$RESP"

# 2. 添加模型配置
echo "[2/8] 添加模型配置"
RESP=$(curl -s -X POST "$BASE_URL/models" \
  -H 'Content-Type: application/json' \
  -d '{"provider":"ALIYUN","modelName":"qwen-turbo","apiKey":"test-key","modelType":"CHAT","isPrimary":false,"isEnabled":true}')
check "模型添加成功" '"code":0' "$RESP"

# 3. 设置主模型
echo "[3/8] 设置主模型"
RESP=$(curl -s -X PATCH "$BASE_URL/models/1/primary")
check "设置主模型成功" '"code":0' "$RESP"

# 4. 添加知识
echo "[4/8] 添加知识"
RESP=$(curl -s -X POST "$BASE_URL/knowledge" \
  -H 'Content-Type: application/json' \
  -d '{"title":"虚拟线程入门","content":"JDK 21 引入虚拟线程（Virtual Threads），一种轻量级线程实现。"}')
check "知识添加成功" '"code":0' "$RESP"

# 5. 搜索知识
echo "[5/8] 搜索知识"
RESP=$(curl -s -G "$BASE_URL/knowledge/search" --data-urlencode "q=虚拟线程")
check "搜索返回结果" '"code":0' "$RESP"

# 6. 获取知识列表
echo "[6/8] 获取知识列表"
RESP=$(curl -s "$BASE_URL/knowledge")
check "列表返回结果" '"code":0' "$RESP"

# 7. 创建聊天会话
echo "[7/8] 创建聊天会话"
RESP=$(curl -s -X POST "$BASE_URL/chat/sessions")
check "会话创建成功" '"code":0' "$RESP"

# 8. 查询操作日志
echo "[8/8] 查询操作日志"
RESP=$(curl -s "$BASE_URL/operation-logs")
check "日志返回结果" '"code":0' "$RESP"

echo ""
echo "====== 结果 ======"
echo "通过: $PASS / 失败: $FAIL"
echo "=================="

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi