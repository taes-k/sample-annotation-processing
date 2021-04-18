package com.taes.lombok;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class CustomGetterProcessor extends AbstractProcessor
{
    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> set = new HashSet<>();
        set.add(CustomGetter.class.getName());

        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        Set<? extends Element> elemenets = roundEnv.getElementsAnnotatedWith(CustomGetter.class);
        List<FieldSpec> fieldSpecList = new ArrayList<>();
        List<MethodSpec> methodSpecList = new ArrayList<>();

        for (Element element : elemenets)
        {
            if (element.getKind() != ElementKind.CLASS)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "CustomGetter have to annotated on class");
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "CustomGetter process : " + element.getSimpleName());

            TypeElement typeElement = (TypeElement) element;

            for (Element field : typeElement.getEnclosedElements())
            {
                if (field.getKind() == ElementKind.FIELD)
                {
                    String fieldNm = field.getSimpleName().toString();
                    TypeName fieldTypeName = TypeName.get(field.asType());

                    FieldSpec fieldSpec = FieldSpec.builder(fieldTypeName, fieldNm)
                        .addModifiers(Modifier.PRIVATE)
                        .build();
                    fieldSpecList.add(fieldSpec);

                    String methodNm = String.format("get%s", StringUtils.capitalize(fieldNm));
                    String returnStatement = "return " + fieldNm;
                    MethodSpec methodSpec = MethodSpec.methodBuilder(methodNm)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(fieldTypeName)
                        .addStatement(returnStatement)
                        .build();

                    methodSpecList.add(methodSpec);

                }
            }
            ClassName className = ClassName.get(typeElement);
            String getterClassName = String.format("%sGetter", className.simpleName());


            TypeSpec getterClass = TypeSpec.classBuilder(getterClassName)
                .addModifiers(Modifier.PUBLIC)
                .addFields(fieldSpecList)
                .addMethods(methodSpecList)
                .build();

            try
            {
                JavaFile.builder(className.packageName(), getterClass)
                    .build()
                    .writeTo(processingEnv.getFiler());
            }
            catch (IOException e)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ERROR : " + e);
            }
        }

        return true;
    }
}
