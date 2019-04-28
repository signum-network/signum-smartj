package bt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * @author jjos
 */
public class EmulatorWarningProcessor extends AbstractProcessor {

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(EmulatorWarning.class.getName());
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(EmulatorWarning.class);

        for (Element element : elements) {
            processingEnv.getMessager().printMessage(WARNING,
                "@EmulatorWarning: method for the emulator only, do not use it!", element);
        }

        return true;
    }
}