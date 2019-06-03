/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

interface ScriptDefinitionProvider {
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?
    fun isScript(fileName: String): Boolean
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun findScriptCompilationConfiguration(fileName: String): ScriptCompilationConfiguration?
    fun findScriptEvaluationConfiguration(fileName: String): ScriptEvaluationConfiguration?

    fun getDefaultScriptCompilationConfiguration(): ScriptCompilationConfiguration
    fun getDefaultScriptEvaluationConfiguration(): ScriptEvaluationConfiguration

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider? =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}


