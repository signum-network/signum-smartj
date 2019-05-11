package bt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * @author jjos
 */
public class WarningProcessor extends AbstractProcessor {

    public static final CompilerVersion currentVersion = CompilerVersion.v0_0_0;

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(EmulatorWarning.class.getName(), TargetCompilerVersion.class.getName()));
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(EmulatorWarning.class)) {
            processingEnv.getMessager().printMessage(WARNING, "@EmulatorWarning: method for the emulator only, do not use it!", element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(TargetCompilerVersion.class)) {
            TargetCompilerVersion targetCompilerVersion = element.getAnnotation(TargetCompilerVersion.class);
            if (targetCompilerVersion == null) {
                processingEnv.getMessager().printMessage(WARNING, "WARNING: Target compiler version not specified", element);
            } else if (targetCompilerVersion.value() != currentVersion) {
                if (targetCompilerVersion.value().ordinal() > currentVersion.ordinal())
                    processingEnv.getMessager().printMessage(WARNING, "WARNING: Target compiler version newer than compiler version. Newer features may not compile or work.", element);
                if (targetCompilerVersion.value().ordinal() < currentVersion.ordinal())
                    processingEnv.getMessager().printMessage(WARNING, "WARNING: Target compiler version older than compiler version. Contract source code may be incompatible.", element);
            }
        }

        return true;
    }
}
