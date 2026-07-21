#!/usr/bin/env node
/**
 * OpenAPI 契约结构 lint 脚本。
 *
 * 用途:校验 docs-SuMon/OpenApi-SuMon/*.json 是否符合 OpenAPI 3.0 最低结构要求。
 *
 * 校验项(只校验结构性必填,不校验业务字段):
 *   1. 顶层必含 openapi / info / paths
 *   2. openapi 版本必须是 3.0.x
 *   3. info.title 与 info.version 必须非空字符串
 *   4. paths 至少含有一条端点
 *
 * 设计原则:
 *   - 纯只读:不修改任何 JSON 文件
 *   - 离线:不依赖网络
 *   - 零依赖:仅用 Node 内置模块
 *   - 退出码:0 全通过,1 有失败(CI 友好)
 *
 * 用法:`npm run openapi:check`
 */
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))
/**
 * 项目根目录 = web-vue-SuMon 的上一级,即仓库根 SuSuMonitor(Jvav)/。
 * scripts/ 在 web-vue-SuMon/scripts/,故向上两级。
 */
const REPO_ROOT = resolve(__dirname, '..', '..')
const OPENAPI_DIR = join(REPO_ROOT, 'docs-SuMon', 'OpenApi-SuMon')

/**
 * 从原始对象读取字符串字段,允许任意路径。
 * 若路径不存在或不是非空字符串,返回 null。
 */
function readString(obj, ...keys) {
  let cur = obj
  for (const k of keys) {
    if (cur === null || cur === undefined || typeof cur !== 'object') {
      return null
    }
    cur = cur[k]
  }
  if (typeof cur !== 'string' || cur.length === 0) {
    return null
  }
  return cur
}

/**
 * 校验单个 OpenAPI JSON 文件。
 */
function validateFile(filePath) {
  const fileName = filePath.split(/[\\/]/).pop() || filePath
  const errors = []
  let raw
  try {
    raw = readFileSync(filePath, 'utf8')
  } catch (e) {
    return {
      file: fileName,
      ok: false,
      errors: [`无法读取文件: ${e.message}`]
    }
  }
  let parsed
  try {
    parsed = JSON.parse(raw)
  } catch (e) {
    return {
      file: fileName,
      ok: false,
      errors: [`JSON 解析失败: ${e.message}`]
    }
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return {
      file: fileName,
      ok: false,
      errors: ['顶层 JSON 必须是对象']
    }
  }
  const root = parsed

  // 1. 顶层必含 openapi / info / paths
  for (const key of ['openapi', 'info', 'paths']) {
    if (!(key in root)) {
      errors.push(`顶层缺少必填字段: ${key}`)
    }
  }

  // 2. openapi 必须是 3.0.x
  const openapiVer = readString(root, 'openapi')
  if (openapiVer !== null && !/^3\.0\.\d+$/.test(openapiVer)) {
    errors.push(
      `openapi 版本必须是 3.0.x,实际为 "${openapiVer}"`
    )
  }

  // 3. info.title / info.version 非空
  const title = readString(root, 'info', 'title')
  if (title === null && 'info' in root) {
    errors.push('info.title 缺失或为空')
  }
  const version = readString(root, 'info', 'version')
  if (version === null && 'info' in root) {
    errors.push('info.version 缺失或为空')
  }

  // 4. paths 至少含一条端点
  let endpointCount = 0
  const paths = root.paths
  if (paths !== undefined) {
    if (paths === null || typeof paths !== 'object' || Array.isArray(paths)) {
      errors.push('paths 必须是对象')
    } else {
      for (const pathKey of Object.keys(paths)) {
        const pathItem = paths[pathKey]
        if (
          pathItem !== null &&
          typeof pathItem === 'object' &&
          !Array.isArray(pathItem)
        ) {
          for (const method of Object.keys(pathItem)) {
            // OpenAPI 路径项允许的非端点字段(参数定义等)跳过
            if (
              ['parameters', 'summary', 'description', 'servers'].includes(method)
            ) {
              continue
            }
            if (
              ['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(
                method.toLowerCase()
              )
            ) {
              endpointCount += 1
            }
          }
        }
      }
    }
  }
  if (endpointCount === 0) {
    errors.push('paths 中未找到任何 HTTP 端点(get/post/put/delete/patch 等)')
  }

  return {
    file: fileName,
    ok: errors.length === 0,
    errors,
    endpointCount
  }
}

/**
 * 主流程:扫描 OpenAPI 目录、逐个校验、打印报告、返回退出码。
 */
function main() {
  let stat
  try {
    stat = statSync(OPENAPI_DIR)
  } catch (e) {
    process.stdout.write(
      `[openapi:check] 目录不存在: ${OPENAPI_DIR}\n` +
        `(错误: ${e.message})\n`
    )
    return 1
  }
  if (!stat.isDirectory()) {
    process.stdout.write(`[openapi:check] 路径不是目录: ${OPENAPI_DIR}\n`)
    return 1
  }

  const files = readdirSync(OPENAPI_DIR)
    .filter((name) => name.endsWith('.json'))
    .sort()
  if (files.length === 0) {
    process.stdout.write(
      `[openapi:check] 目录中无 .json 文件: ${OPENAPI_DIR}\n`
    )
    return 1
  }

  const results = files.map((name) =>
    validateFile(join(OPENAPI_DIR, name))
  )

  process.stdout.write('OpenAPI 契约结构校验\n')
  process.stdout.write('================================\n\n')
  for (const r of results) {
    if (r.ok) {
      const count = r.endpointCount || 0
      process.stdout.write(
        `✓ ${r.file.padEnd(32)} (${count} endpoint${count === 1 ? '' : 's'})\n`
      )
    } else {
      process.stdout.write(`✗ ${r.file}  失败:\n`)
      for (const err of r.errors) {
        process.stdout.write(`    - ${err}\n`)
      }
    }
  }
  process.stdout.write('\n================================\n')
  const passed = results.filter((r) => r.ok).length
  process.stdout.write(`${passed} / ${results.length} 通过\n`)
  return passed === results.length ? 0 : 1
}

process.exit(main())