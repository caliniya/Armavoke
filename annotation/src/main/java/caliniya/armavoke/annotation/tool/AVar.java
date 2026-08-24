package caliniya.armavoke.annotation.tool;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;

/**
 * 变量元素包装（字段、参数、局部变量）
 */
public class AVar extends AElement<VariableElement> {

    public AVar(VariableElement element) {
        super(element);
    }

    /**
     * 判断是否为字段
     */
    public boolean isField() {
        return e.getKind() == ElementKind.FIELD;
    }

    /**
     * 判断是否为参数
     */
    public boolean isParameter() {
        return e.getKind() == ElementKind.PARAMETER;
    }

    /**
     * 判断是否为局部变量
     */
    public boolean isLocalVariable() {
        return e.getKind() == ElementKind.LOCAL_VARIABLE;
    }

    /**
     * 判断是否为静态字段
     */
    public boolean isStatic() {
        return is(Modifier.STATIC);
    }

    /**
     * 判断是否为 final 字段
     */
    public boolean isFinal() {
        return is(Modifier.FINAL);
    }

    /**
     * 判断是否为 transient 字段
     */
    public boolean isTransient() {
        return is(Modifier.TRANSIENT);
    }

    /**
     * 判断是否为 volatile 字段
     */
    public boolean isVolatile() {
        return is(Modifier.VOLATILE);
    }

    /**
     * 获取变量类型
     */
    public TypeMirror type() {
        return e.asType();
    }

    /**
     * 获取变量类型名称
     */
    public String typeName() {
        return type().toString();
    }

    /**
     * 获取初始值（仅常量字段有效）
     */
    public Object constantValue() {
        return e.getConstantValue();
    }

    /**
     * 判断是否有常量初始值
     */
    public boolean hasConstantValue() {
        return e.getConstantValue() != null;
    }

    /**
     * 获取封闭类型
     */
    public AType enclosingType() {
        Element parent = up();
        if (!(parent instanceof TypeElement)) {
            throw new IllegalStateException("Variable not enclosed in a type: " + e);
        }
        return new AType((TypeElement) parent);
    }

    /**
     * 判断是否为枚举常量
     */
    public boolean isEnumConstant() {
        return e.getKind() == ElementKind.ENUM_CONSTANT;
    }

    /**
     * 获取字段描述字符串
     */
    public String descString() {
        return up().toString() + "#" + e.toString();
    }

    /**
     * 判断是否为基本类型
     */
    public boolean isPrimitive() {
        return type().getKind().isPrimitive();
    }

    /**
     * 判断是否为数组类型
     */
    public boolean isArray() {
        return type().getKind() == javax.lang.model.type.TypeKind.ARRAY;
    }

    /**
     * 判断是否为集合类型
     */
    public boolean isCollection() {
        String typeName = typeName();
        return typeName.startsWith("java.util.Collection") ||
               typeName.startsWith("java.util.List") ||
               typeName.startsWith("java.util.Set") ||
               typeName.startsWith("java.util.Map");
    }
}