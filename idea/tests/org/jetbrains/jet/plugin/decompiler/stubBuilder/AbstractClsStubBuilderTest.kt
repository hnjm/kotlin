/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.decompiler.stubBuilder

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.lang.psi.stubs.elements.JetFileStubBuilder
import org.jetbrains.jet.JetTestUtils
import java.io.File
import org.jetbrains.jet.MockLibraryUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.indexing.FileContentImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.google.common.io.Files
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Assert
import org.jetbrains.jet.plugin.stubs.AbstractStubBuilderTest
import java.util.LinkedHashSet
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.jet.utils.addIfNotNull
import com.intellij.openapi.util.io.FileUtil

public abstract class AbstractClsStubBuilderTest : LightCodeInsightFixtureTestCase() {
    fun doTest(sourcePath: String) {
        val classFile = getClassFileToDecompile(sourcePath)
        val txtFilePath = File("$sourcePath/${lastSegment(sourcePath)}.txt")
        testClsStubsForFile(classFile, txtFilePath)
    }

    protected fun testClsStubsForFile(classFile: VirtualFile, txtFile: File?) {
        val stubTreeFromCls = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(classFile))!!
        val psiFile = myFixture.getPsiManager().findFile(classFile)
        val stubTreeFromDecompiledText = JetFileStubBuilder().buildStubTree(psiFile)
        val expectedText = stubTreeFromDecompiledText.serializeToString()
        Assert.assertEquals(expectedText, stubTreeFromCls.serializeToString())
        if (txtFile != null) {
            JetTestUtils.assertEqualsToFile(txtFile, expectedText)
        }
    }

    private fun getClassFileToDecompile(sourcePath: String): VirtualFile {
        val outDir = FileUtil.createTempDirectory("libForStubTest", getTestName(true))
        MockLibraryUtil.compileKotlin(sourcePath, outDir)
        val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outDir)
        return root.findClassFileByName(lastSegment(sourcePath))
    }

    private fun lastSegment(sourcePath: String): String {
        return Files.getNameWithoutExtension(sourcePath.split("/").last())!!
    }
}

private fun StubElement<out PsiElement>.serializeToString(): String {
    return AbstractStubBuilderTest.serializeStubToString(this)
}

fun VirtualFile.findClassFileByName(className: String): VirtualFile {
    val files = LinkedHashSet<VirtualFile>()
    VfsUtilCore.iterateChildrenRecursively(
            this,
            { virtualFile -> virtualFile.isDirectory() || virtualFile.getName().equals("$className.class") },
            { virtualFile -> if (!virtualFile.isDirectory()) files.addIfNotNull(virtualFile); true })

    return files.single()
}