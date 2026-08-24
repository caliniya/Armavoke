package caliniya.armavoke.annotation;

import caliniya.armavoke.annotation.tool.AElement;
import caliniya.armavoke.annotation.tool.AMethod;
import caliniya.armavoke.annotation.tool.AType;
import caliniya.armavoke.annotation.tool.AVar;
import caliniya.armavoke.base.tool.Ar;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 注解处理器基类，提供通用功能
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class Processor extends AbstractProcessor {

    public static Types typeUtils;
    public static Elements elementUtils;
    public static Filer filer;
    public static Messager messager;

    private int round = 0;
    private int maxRounds = 1;
    protected RoundEnvironment roundEnv;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (round++ >= maxRounds) {
            return false;
        }
        this.roundEnv = roundEnv;
        try {
            process();
        } catch (Exception e) {
            error("Processing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 子类实现具体的处理逻辑
     */
    protected abstract void process() throws Exception;

    /**
     * 获取所有标注了指定注解的元素
     */
    public Ar<AElement<?>> elements(Class<? extends Annotation> annotation) {
        return Ar.with(roundEnv.getElementsAnnotatedWith(annotation))
                .map(AElement::new);
    }

    /**
     * 获取所有标注了指定注解的类型
     */
    public Ar<AType> types(Class<? extends Annotation> annotation) {
        return Ar.with(roundEnv.getElementsAnnotatedWith(annotation))
                .select(e -> e instanceof TypeElement)
                .map(e -> new AType((TypeElement) e));
    }

    /**
     * 获取所有标注了指定注解的方法
     */
    public Ar<AMethod> methods(Class<? extends Annotation> annotation) {
        return Ar.with(roundEnv.getElementsAnnotatedWith(annotation))
                .select(e -> e instanceof ExecutableElement)
                .map(e -> new AMethod((ExecutableElement) e));
    }

    /**
     * 获取所有标注了指定注解的字段
     */
    public Ar<AVar> fields(Class<? extends Annotation> annotation) {
        return Ar.with(roundEnv.getElementsAnnotatedWith(annotation))
                .select(e -> e instanceof VariableElement)
                .map(e -> new AVar((VariableElement) e));
    }

    /**
     * 创建新的源文件
     */
    public Writer createSourceFile(String className) throws IOException {
        return filer.createSourceFile(className).openWriter();
    }

    /**
     * 创建新的源文件（带注解元素）
     */
    public Writer createSourceFile(String className, Element... originatingElements) throws IOException {
        return filer.createSourceFile(className, originatingElements).openWriter();
    }

    /**
     * 输出信息
     */
    public void info(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * 输出警告
     */
    public void warning(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message);
    }

    /**
     * 输出错误
     */
    public void error(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    /**
     * 输出错误（带元素信息）
     */
    public void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * 输出错误（带元素包装）
     */
    public void error(String message, AElement<?> element) {
        error(message, element.e);
    }

    /**
     * 获取包路径下的类型元素
     */
    public TypeElement getTypeElement(String className) {
        return elementUtils.getTypeElement(className);
    }

    /**
     * 判断是否为基础类型
     */
    public static boolean isPrimitive(String typeName) {
        return typeName.equals("boolean") || typeName.equals("byte") ||
               typeName.equals("short") || typeName.equals("int") ||
               typeName.equals("long") || typeName.equals("float") ||
               typeName.equals("double") || typeName.equals("char");
    }

    /**
     * 获取基础类型的大小（字节）
     */
    public static int primitiveSize(String typeName) {
        switch (typeName) {
            case "boolean":
            case "byte":
                return 1;
            case "short":
            case "char":
                return 2;
            case "int":
            case "float":
                return 4;
            case "long":
            case "double":
                return 8;
            default:
                throw new IllegalArgumentException("Not a primitive type: " + typeName);
        }
    }

    /**
     * 获取基础类型的默认值
     */
    public static String primitiveDefault(String typeName) {
        switch (typeName) {
            case "boolean":
                return "false";
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "char":
                return "0";
            default:
                return "null";
        }
    }

    /**
     * 获取简单类名（不含包名）
     */
    public static String simpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    /**
     * 获取包名
     */
    public static String packageName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : "";
    }
}