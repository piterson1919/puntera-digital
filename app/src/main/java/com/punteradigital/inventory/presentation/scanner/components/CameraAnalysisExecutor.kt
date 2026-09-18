package com.punteradigital.inventory.presentation.scanner.components

import java.util.concurrent.Executor
import java.util.concurrent.Executors

object CameraAnalysisExecutor {
    val executor: Executor = Executors.newFixedThreadPool(2)
}