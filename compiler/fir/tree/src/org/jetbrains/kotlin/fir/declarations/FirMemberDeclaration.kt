/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

abstract class FirMemberDeclaration(
    session: FirSession,
    psi: PsiElement?,
    name: Name
) : FirTypeParameterContainer, FirNamedDeclaration(session, psi, name), FirAnnotationContainer {
    abstract val status: FirDeclarationStatus

    val visibility: Visibility get() = status.visibility

    val modality: Modality? get() = status.modality

    val isExpect: Boolean get() = status.isExpect

    val isActual: Boolean get() = status.isActual

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitMemberDeclaration(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        acceptAnnotations(visitor, data)
        for (typeParameter in typeParameters) {
            typeParameter.accept(visitor, data)
        }
        status.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}
