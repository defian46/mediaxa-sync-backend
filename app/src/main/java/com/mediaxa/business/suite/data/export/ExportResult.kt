package com.mediaxa.business.suite.data.export

data class ExportResult(
    val fileName: String,
    val filePath: String,
    val format: String,
    val sizeBytes: Long
)
