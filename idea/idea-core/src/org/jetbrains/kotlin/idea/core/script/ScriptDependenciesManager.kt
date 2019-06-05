/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.dependencies.SyncScriptDependenciesLoader
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import kotlin.script.experimental.api.valueOrNull


// NOTE: this service exists exclusively because ScriptDependencyManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptDependenciesProvider(
    private val scriptDependenciesManager: ScriptDependenciesManager
) : ScriptDependenciesProvider {
    override fun getScriptConfigurationResult(file: VirtualFile): ScriptCompilationConfigurationResult? = scriptDependenciesManager.getRefinedCompilationConfiguration(file)
}

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class ScriptDependenciesManager internal constructor(
    private val cacheUpdater: ScriptsCompilationConfigurationUpdater,
    private val cache: ScriptsCompilationConfigurationCache
) {
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> =
        toVfsRoots(cacheUpdater.getCurrentCompilationConfiguration(file)?.valueOrNull()?.dependenciesClassPath.orEmpty())

    fun getRefinedCompilationConfiguration(file: VirtualFile): ScriptCompilationConfigurationResult? =
        cacheUpdater.getCurrentCompilationConfiguration(file)

    fun getScriptSdk(file: VirtualFile): Sdk? = Companion.getScriptSdk(getRefinedCompilationConfiguration(file)?.valueOrNull())

    fun getScriptDependenciesClassFilesScope(file: VirtualFile) = cache.scriptDependenciesClassFilesScope(file)

    fun getAllScriptsSdks() = cache.allSdks

    fun getAllScriptsDependenciesClassFilesScope() = cache.allDependenciesClassFilesScope
    fun getAllScriptDependenciesSourcesScope() = cache.allDependenciesSourcesScope

    fun getAllScriptsDependenciesClassFiles() = cache.allDependenciesClassFiles
    fun getAllScriptDependenciesSources() = cache.allDependenciesSources

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScriptDependenciesManager =
            ServiceManager.getService(project, ScriptDependenciesManager::class.java)

        fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
            // workaround for mismatched gradle wrapper and plugin version
            val javaHome = try {
                compilationConfiguration?.javaHome?.canonicalPath
            } catch (e: Throwable) {
                null
            }

            return getAllProjectSdks().find { javaHome != null && File(it.homePath).canonicalPath == javaHome }
        }


        fun getScriptDefaultSdk(project: Project): Sdk? =
            ProjectRootManager.getInstance(project).projectSdk ?: getAllProjectSdks().firstOrNull()

        fun getProjectSdk(project: Project) = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }

        fun toVfsRoots(roots: Iterable<File>): List<VirtualFile> {
            return roots.mapNotNull { it.classpathEntryToVfs() }
        }

        private fun Sdk.canBeUsedForScript() = sdkType is JavaSdkType

        private fun File.classpathEntryToVfs(): VirtualFile? {
            val res = when {
                !exists() -> null
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath)
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        internal val log = Logger.getInstance(ScriptDependenciesManager::class.java)

        @TestOnly
        fun updateScriptDependenciesSynchronously(virtualFile: VirtualFile, project: Project) {
            val loader = SyncScriptDependenciesLoader(project)
            loader.loadDependencies(virtualFile)
            loader.notifyRootsChanged()
        }
    }
}
