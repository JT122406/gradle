/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.verification.verifier;

import org.gradle.internal.logging.text.TreeFormatter;
import org.jspecify.annotations.NullMarked;

import java.io.File;

@NullMarked
public class InvalidSignature extends AbstractVerificationFailure {
    private final File signatureFile;

    public InvalidSignature(File affectedFile, File signatureFile) {
        super(affectedFile);
        this.signatureFile = signatureFile;
    }

    @Override
    public boolean isFatal() {
        return false;
    }

    @Override
    public File getSignatureFile() {
        return signatureFile;
    }

    @Override
    public void explainTo(TreeFormatter formatter) {
        formatter.append("artifact has signature file, but it contains no valid signatures");
    }
}
