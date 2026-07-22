/**
 * 错误码常量,与后端 ErrorCode.java 和 OpenAPI ErrorResponse 严格对齐。
 * 任何后端错误码变更必须先改 OpenAPI JSON,再同步本文件。
 */

export const ErrorCode = {
  SUCCESS: 0,
  BAD_REQUEST: 40000,
  INVALID_USERNAME_OR_PASSWORD: 40001,
  INVALID_REQUEST_PARAMETER: 40002,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
  RESOURCE_NOT_FOUND: 40400,
  RESOURCE_CONFLICT: 40900,
  INTERNAL_SERVER_ERROR: 50000,
  DATABASE_ERROR: 50001
} as const

export type ErrorCodeValue = (typeof ErrorCode)[keyof typeof ErrorCode]