/*
 * Copyright 2014 the original author or authors.
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
package org.gradle.language.rc.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.Cast;
import org.gradle.internal.operations.logging.BuildOperationLogger;
import org.gradle.internal.operations.logging.BuildOperationLoggerFactory;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.base.internal.compile.CompilerUtil;
import org.gradle.language.nativeplatform.internal.incremental.IncrementalCompilerBuilder;
import org.gradle.language.rc.internal.DefaultWindowsResourceCompileSpec;
import org.gradle.nativeplatform.internal.BuildOperationLoggingCompilerDecorator;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainInternal;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Compiles Windows Resource scripts into .res files.
 */
@Incubating
@DisableCachingByDefault(because = "Not made cacheable, yet")
public abstract class WindowsResourceCompile extends DefaultTask {

    private File outputDir;
    private Map<String, String> macros = new LinkedHashMap<String, String>();

    // Don't serialize the compiler. It holds state that is mostly only required at execution time and that can be calculated from the other fields of this task
    // after being deserialized. However, it is also required to calculate the producers of the header files to calculate the work graph.
    // It would be better to provide some way for a task to express these things separately.
    private transient IncrementalCompilerBuilder.IncrementalCompiler incrementalCompiler;

    public WindowsResourceCompile() {
        getInputs().property("outputType", new Callable<String>() {
            @Override
            public String call() {
                NativeToolChainInternal nativeToolChain = (NativeToolChainInternal) getToolChain().get();
                NativePlatformInternal nativePlatform = (NativePlatformInternal) getTargetPlatform().get();
                return NativeToolChainInternal.Identifier.identify(nativeToolChain, nativePlatform);
            }
        });
    }

    private IncrementalCompilerBuilder.IncrementalCompiler getIncrementalCompiler() {
        if (incrementalCompiler == null) {
            incrementalCompiler = getIncrementalCompilerBuilder().newCompiler(this, getSource(), getIncludes(), macros, Providers.FALSE);
        }
        return incrementalCompiler;
    }

    @Inject
    public abstract IncrementalCompilerBuilder getIncrementalCompilerBuilder();

    @Inject
    public abstract BuildOperationLoggerFactory getOperationLoggerFactory();

    @TaskAction
    public void compile(InputChanges inputs) {
        BuildOperationLogger operationLogger = getOperationLoggerFactory().newOperationLogger(getName(), getTemporaryDir());

        NativeCompileSpec spec = new DefaultWindowsResourceCompileSpec();
        spec.setTempDir(getTemporaryDir());
        spec.setObjectFileDir(getOutputDir());
        spec.include(getIncludes());
        spec.source(getSource());
        spec.setMacros(getMacros());
        spec.args(getCompilerArgs().get());
        spec.setIncrementalCompile(inputs.isIncremental());
        spec.setOperationLogger(operationLogger);

        NativeToolChainInternal nativeToolChain = (NativeToolChainInternal) getToolChain().get();
        NativePlatformInternal nativePlatform = (NativePlatformInternal) getTargetPlatform().get();
        PlatformToolProvider platformToolProvider = nativeToolChain.select(nativePlatform);
        WorkResult result = doCompile(spec, platformToolProvider);
        setDidWork(result.getDidWork());
    }

    private <T extends NativeCompileSpec> WorkResult doCompile(T spec, PlatformToolProvider platformToolProvider) {
        Class<T> specType = Cast.uncheckedCast(spec.getClass());
        Compiler<T> baseCompiler = platformToolProvider.newCompiler(specType);
        Compiler<T> incrementalCompiler = getIncrementalCompiler().createCompiler(baseCompiler);
        Compiler<T> loggingCompiler = BuildOperationLoggingCompilerDecorator.wrap(incrementalCompiler);
        return CompilerUtil.castCompiler(loggingCompiler).execute(spec);
    }

    /**
     * The tool chain used for compilation.
     *
     * @since 4.7
     */
    @Internal
    public abstract Property<NativeToolChain> getToolChain();

    /**
     * The platform being compiled for.
     *
     * @since 4.7
     */
    @Nested
    public abstract Property<NativePlatform> getTargetPlatform();

    /**
     * The directory where object files will be generated.
     */
    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Returns the header directories to be used for compilation.
     */
    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getIncludes();

    /**
     * Add directories where the compiler should search for header files.
     */
    public void includes(Object includeRoots) {
        getIncludes().from(includeRoots);
    }

    /**
     * Returns the source files to be compiled.
     */
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSource();

    /**
     * Adds a set of source files to be compiled. The provided sourceFiles object is evaluated as per {@link Project#files(Object...)}.
     */
    public void source(Object sourceFiles) {
        getSource().from(sourceFiles);
    }

    /**
     * Macros that should be defined for the compiler.
     */
    @Input
    public Map<String, String> getMacros() {
        return macros;
    }

    public void setMacros(Map<String, String> macros) {
        this.macros = macros;
    }

    /**
     * <em>Additional</em> arguments to provide to the compiler.
     *
     * @since 5.1
     */
    @Input
    public abstract ListProperty<String> getCompilerArgs();


    /**
     * The set of dependent headers. This is used for up-to-date checks only.
     *
     * @since 4.5
     */
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    protected FileCollection getHeaderDependencies() {
        return getIncrementalCompiler().getHeaderFiles();
    }
}
