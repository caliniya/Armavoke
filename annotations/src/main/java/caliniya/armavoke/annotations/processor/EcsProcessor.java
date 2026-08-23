package caliniya.armavoke.annotations.processor;

import caliniya.armavoke.annotations.AccessMode;
import caliniya.armavoke.annotations.Component;
import caliniya.armavoke.annotations.EntityDef;
import caliniya.armavoke.annotations.FieldOpt;
import caliniya.armavoke.annotations.Import;
import caliniya.armavoke.annotations.Lifecycle;
import caliniya.armavoke.annotations.Storage;
import caliniya.armavoke.annotations.SystemDef;
import caliniya.armavoke.annotations.ThreadDef;
import caliniya.armavoke.annotations.UpdateMethod;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@Generated("caliniya.armavoke")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("caliniya.armavoke.annotations.*")
public final class EcsProcessor extends AbstractProcessor {
  private static final String generatedPackage = "caliniya.armavoke.ecs.generated";
  private static final String accessPackage = generatedPackage + ".access";

  private final Map<String, ThreadModel> threads = new LinkedHashMap<>();
  private final Map<String, ComponentModel> components = new LinkedHashMap<>();
  private final Map<String, ComponentModel> componentsByType = new LinkedHashMap<>();
  private final Map<String, SystemModel> systems = new LinkedHashMap<>();
  private final Map<String, EntityModel> entities = new LinkedHashMap<>();
  private final Map<String, Integer> componentIndexes = new LinkedHashMap<>();

  private Filer filer;
  private Messager messager;
  private Elements elementUtils;
  private Types typeUtils;
  private boolean emitted;
  private boolean invalid;

  @Override
  public synchronized void init(javax.annotation.processing.ProcessingEnvironment environment) {
    super.init(environment);
    filer = environment.getFiler();
    messager = environment.getMessager();
    elementUtils = environment.getElementUtils();
    typeUtils = environment.getTypeUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (emitted || round.processingOver()) return false;

    invalid = false;
    collectThreads(round);
    collectComponents(round);
    collectSystems(round);
    collectEntities(round);
    if (threads.isEmpty() && components.isEmpty() && systems.isEmpty() && entities.isEmpty()) {
      return false;
    }

    validate();
    if (!invalid) {
      try {
        indexComponents();
        for (ComponentModel component : components.values()) generateAccess(component);
        for (EntityModel entity : entities.values()) generateEntity(entity);
        generateRegistry();
      } catch (IOException exception) {
        error(null, "ECS source generation failed: " + exception.getMessage());
      }
    }
    emitted = true;
    return false;
  }

  private void collectThreads(RoundEnvironment round) {
    for (Element element : round.getElementsAnnotatedWith(ThreadDef.class)) {
      if (!(element instanceof TypeElement type)) continue;
      ThreadDef definition = type.getAnnotation(ThreadDef.class);
      ThreadModel model =
          new ThreadModel(
              type,
              definition.name(),
              definition.workers(),
              definition.priority(),
              definition.interruptible());
      putUnique(threads, model.name, model, type, "thread");
    }
  }

  private void collectComponents(RoundEnvironment round) {
    for (Element element : round.getElementsAnnotatedWith(Component.class)) {
      if (!(element instanceof TypeElement type)) continue;
      Component definition = type.getAnnotation(Component.class);
      ComponentModel model =
          new ComponentModel(
              type,
              definition.name(),
              type.getQualifiedName().toString(),
              definition.updateBy(),
              definition.pure(),
              definition.pooled(),
              definition.storage(),
              classValues(type, Component.class.getCanonicalName(), "requires"));

      for (Element enclosed : type.getEnclosedElements()) {
        if (enclosed instanceof VariableElement field
            && !field.getModifiers().contains(Modifier.STATIC)) {
          FieldOpt options = field.getAnnotation(FieldOpt.class);
          model.fields.add(
              new FieldModel(
                  field,
                  field.getSimpleName().toString(),
                  field.asType().toString(),
                  field.asType().getKind(),
                  options != null && options.volatileField(),
                  options != null && options.readonly(),
                  options == null ? "" : options.defaultValue(),
                  options == null || options.persist(),
                  isEnum(field.asType())));
        } else if (enclosed instanceof ExecutableElement method) {
          UpdateMethod update = method.getAnnotation(UpdateMethod.class);
          if (update != null) {
            model.methods.add(
                new MethodModel(
                    method,
                    method.getSimpleName().toString(),
                    update.order(),
                    update.lifecycle(),
                    method.getParameters().stream()
                        .map(parameter -> parameter.asType().toString())
                        .collect(Collectors.toList())));
          }
        }
      }

      for (Import imported : type.getAnnotationsByType(Import.class)) {
        String componentType;
        try {
          componentType = imported.component().getCanonicalName();
        } catch (MirroredTypeException mirrored) {
          componentType = mirrored.getTypeMirror().toString();
        }
        model.imports.add(
            new ImportModel(componentType, List.of(imported.fields()), imported.mode()));
      }

      putUnique(components, model.name, model, type, "component");
      ComponentModel previous = componentsByType.putIfAbsent(model.typeName, model);
      if (previous != null) error(type, "Duplicate component type: " + model.typeName);
    }
  }

  private void collectSystems(RoundEnvironment round) {
    for (Element element : round.getElementsAnnotatedWith(SystemDef.class)) {
      if (!(element instanceof TypeElement type)) continue;
      SystemDef definition = type.getAnnotation(SystemDef.class);
      SystemModel model =
          new SystemModel(
              type,
              definition.name(),
              type.getQualifiedName().toString(),
              definition.thread(),
              definition.priority(),
              Math.max(1, definition.interval()),
              definition.parallel(),
              classValues(type, SystemDef.class.getCanonicalName(), "reads"),
              classValues(type, SystemDef.class.getCanonicalName(), "writes"),
              List.of(definition.after()));
      putUnique(systems, model.name, model, type, "system");
    }
  }

  private void collectEntities(RoundEnvironment round) {
    for (Element element : round.getElementsAnnotatedWith(EntityDef.class)) {
      if (!(element instanceof TypeElement type)) continue;
      EntityDef definition = type.getAnnotation(EntityDef.class);
      String generatedClass =
          definition.generatedClass().isBlank()
              ? javaName(definition.name()) + "EcsEntity"
              : definition.generatedClass();
      EntityModel model =
          new EntityModel(
              type,
              definition.name(),
              type.getQualifiedName().toString(),
              generatedClass,
              definition.pooled(),
              definition.serializable(),
               definition.constructor(),
               classValues(type, EntityDef.class.getCanonicalName(), "components"),
               classValues(type, EntityDef.class.getCanonicalName(), "abilities"),
               classValues(type, EntityDef.class.getCanonicalName(), "modules"),
               java.util.Arrays.asList(definition.interfaces()));
      putUnique(entities, model.name, model, type, "entity");
    }
  }

  private <T> void putUnique(
      Map<String, T> map, String name, T value, Element element, String kind) {
    if (name == null || name.isBlank()) {
      error(element, "ECS " + kind + " name cannot be blank");
    } else if (map.putIfAbsent(name, value) != null) {
      error(element, "Duplicate ECS " + kind + " name: " + name);
    }
  }

  private void validate() {
    if (components.size() > 63) {
      error(null, "At most 63 components are supported by the generated component bitmap");
    }

    for (ThreadModel thread : threads.values()) {
      if (thread.workers < 1) error(thread.element, "Thread workers must be at least 1");
      if (thread.priority < Thread.MIN_PRIORITY || thread.priority > Thread.MAX_PRIORITY) {
        error(thread.element, "Thread priority must be between 1 and 10");
      }
    }

    for (ComponentModel component : components.values()) {
      for (String required : component.requires) {
        if (!componentsByType.containsKey(required)) {
          error(component.element, "Unknown required component: " + required);
        }
      }
      if (!component.updateBy.isBlank() && !systems.containsKey(component.updateBy)) {
        error(component.element, "Unknown update system: " + component.updateBy);
      }
      if (component.pure && !component.methods.isEmpty()) {
        error(component.element, "Pure component cannot declare @UpdateMethod methods");
      }
      if (component.storage != Storage.Reference && !component.methods.isEmpty()) {
        error(component.element, "Components with update methods must use Reference storage");
      }
      if (component.storage == Storage.Reference
          && !component.element.getModifiers().contains(Modifier.PUBLIC)) {
        error(component.element, "Reference component must be public");
      }

      Set<String> fieldNames =
          component.fields.stream()
              .map(field -> field.name)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      for (ImportModel imported : component.imports) {
        ComponentModel target = componentsByType.get(imported.componentType);
        if (target == null) {
          error(component.element, "Unknown imported component: " + imported.componentType);
          continue;
        }
        Set<String> targetFields =
            target.fields.stream().map(field -> field.name).collect(Collectors.toSet());
        for (String field : imported.fields) {
          if (!targetFields.contains(field)) {
            error(component.element, "Unknown imported field " + imported.componentType + "." + field);
          }
        }
      }

      for (MethodModel method : component.methods) {
        if (!method.element.getModifiers().contains(Modifier.PUBLIC)) {
          error(method.element, "@UpdateMethod must be public");
        }
        if (method.lifecycle == Lifecycle.Update) {
          if (method.parameterTypes.size() > 1
              || (method.parameterTypes.size() == 1
                  && !method.parameterTypes.get(0).equals("float"))) {
            error(method.element, "Update method must accept no arguments or one float delta");
          }
        } else if (!method.parameterTypes.isEmpty()) {
          error(method.element, "Initialize/destroy methods cannot accept arguments");
        }
      }

      for (FieldModel field : component.fields) {
        if (component.storage == Storage.Reference
            && !field.element.getModifiers().contains(Modifier.PUBLIC)) {
          error(field.element, "Fields in a Reference component must be public");
        }
        if (field.readonly && field.volatileField) {
          error(field.element, "Readonly fields cannot be double-buffered");
        }
        if (field.persist && !isPersistable(field)) {
          warning(field.element, "Unsupported persisted field type; mark persist=false: " + field.type);
        }
      }
      if (fieldNames.size() != component.fields.size()) {
        error(component.element, "Duplicate component field name");
      }
    }

    detectComponentCycles();

    for (SystemModel system : systems.values()) {
      if (!threads.containsKey(system.thread)) {
        error(system.element, "Unknown thread group: " + system.thread);
      }
      validateComponentTypes(system.element, system.reads);
      validateComponentTypes(system.element, system.writes);
      for (String dependency : system.after) {
        if (!systems.containsKey(dependency)) {
          error(system.element, "Unknown system dependency: " + dependency);
        }
      }
    }
    detectSystemCycles();
    warnParallelConflicts();

    Set<String> generatedNames = new HashSet<>();
    for (EntityModel entity : entities.values()) {
      if (!generatedNames.add(entity.generatedClass)) {
        error(entity.element, "Duplicate generated entity class: " + entity.generatedClass);
      }
      validateComponentTypes(entity.element, entity.components);
      Set<String> included = new HashSet<>(entity.components);
      for (String componentType : entity.components) {
        ComponentModel component = componentsByType.get(componentType);
        if (component == null) continue;
        for (String required : component.requires) {
          if (!included.contains(required)) {
            error(
                entity.element,
                "Entity "
                    + entity.name
                    + " is missing dependency "
                    + required
                    + " required by "
                    + component.name);
          }
        }
      }
    }
  }

  private void validateComponentTypes(Element origin, Collection<String> types) {
    for (String type : types) {
      if (!componentsByType.containsKey(type)) {
        error(origin, "Unknown component type: " + type);
      }
    }
  }

  private void detectComponentCycles() {
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();
    for (ComponentModel component : components.values()) {
      visitComponent(component, visited, visiting);
    }
  }

  private void visitComponent(
      ComponentModel component, Set<String> visited, Set<String> visiting) {
    if (visited.contains(component.typeName)) return;
    if (!visiting.add(component.typeName)) {
      error(component.element, "Circular component dependency involving " + component.name);
      return;
    }
    for (String required : component.requires) {
      ComponentModel next = componentsByType.get(required);
      if (next != null) visitComponent(next, visited, visiting);
    }
    visiting.remove(component.typeName);
    visited.add(component.typeName);
  }

  private void detectSystemCycles() {
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();
    for (SystemModel system : systems.values()) visitSystem(system, visited, visiting);
  }

  private void visitSystem(SystemModel system, Set<String> visited, Set<String> visiting) {
    if (visited.contains(system.name)) return;
    if (!visiting.add(system.name)) {
      error(system.element, "Circular system dependency involving " + system.name);
      return;
    }
    for (String dependency : system.after) {
      SystemModel next = systems.get(dependency);
      if (next != null) visitSystem(next, visited, visiting);
    }
    visiting.remove(system.name);
    visited.add(system.name);
  }

  private void warnParallelConflicts() {
    List<SystemModel> all = new ArrayList<>(systems.values());
    for (int i = 0; i < all.size(); i++) {
      SystemModel left = all.get(i);
      if (!left.parallel) continue;
      for (int j = i + 1; j < all.size(); j++) {
        SystemModel right = all.get(j);
        if (!right.parallel || !left.thread.equals(right.thread)) continue;
        if (intersects(left.writes, right.reads)
            || intersects(left.writes, right.writes)
            || intersects(right.writes, left.reads)) {
          warning(
              right.element,
              "Parallel systems "
                  + left.name
                  + " and "
                  + right.name
                  + " have a component read/write conflict");
        }
      }
    }
  }

  private boolean intersects(Collection<String> left, Collection<String> right) {
    for (String item : left) if (right.contains(item)) return true;
    return false;
  }

  private void indexComponents() {
    List<ComponentModel> sorted = new ArrayList<>(components.values());
    sorted.sort(Comparator.comparing(component -> component.name));
    for (int i = 0; i < sorted.size(); i++) {
      componentIndexes.put(sorted.get(i).typeName, i);
    }
  }

  private void generateAccess(ComponentModel component) throws IOException {
    String className = accessName(component);
    StringBuilder source = new StringBuilder(1024);
    source
        .append("package ")
        .append(accessPackage)
        .append(";\n\n")
        .append("@javax.annotation.processing.Generated(\"")
        .append(EcsProcessor.class.getName())
        .append("\")\n")
        .append("public interface ")
        .append(className)
        .append(" {\n");

    if (component.storage == Storage.Reference) {
      source
          .append("  ")
          .append(component.typeName)
          .append(' ')
          .append(componentPrefix(component))
          .append("Component();\n");
    } else {
      for (FieldModel field : component.fields) {
        String method = fieldMethod(component, field);
        source.append("  ").append(field.type).append(' ').append(method).append("();\n");
        if (!field.readonly) {
          source
              .append("  void ")
              .append(method)
              .append('(')
              .append(field.type)
              .append(" value);\n");
        }
        if (field.volatileField) {
          source
              .append("  ")
              .append(field.type)
              .append(' ')
              .append(method)
              .append("Back();\n")
              .append("  void ")
              .append(method)
              .append("Back(")
              .append(field.type)
              .append(" value);\n")
              .append("  default void ")
              .append(method)
              .append("Swap(){ ")
              .append(field.type)
              .append(" value = ")
              .append(method)
              .append("(); ")
              .append(method)
              .append('(')
              .append(method)
              .append("Back()); ")
              .append(method)
              .append("Back(value); }\n");
        }
      }
    }
    source.append("}\n");
    writeSource(accessPackage + "." + className, source.toString(), component.element);
  }

  private void generateEntity(EntityModel entity) throws IOException {
    List<ComponentModel> included =
        entity.components.stream()
            .map(componentsByType::get)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    long mask = mask(entity.components);
    String className = entity.generatedClass;
    StringBuilder source = new StringBuilder(8192);
    source
        .append("package ")
        .append(generatedPackage)
        .append(";\n\n")
        .append("@javax.annotation.processing.Generated(\"")
        .append(EcsProcessor.class.getName())
        .append("\")\n")
        .append("public final class ")
        .append(className)
        .append(" extends caliniya.armavoke.ecs.runtime.EcsEntity");
    if (!included.isEmpty() || !entity.interfaces.isEmpty()) {
      source.append(" implements ");
      boolean wroteInterface = false;
      for (String interfaceName : entity.interfaces) {
        if (wroteInterface) source.append(", ");
        source.append(interfaceName);
        wroteInterface = true;
      }
      for (int i = 0; i < included.size(); i++) {
        if (wroteInterface) source.append(", ");
        source.append(accessPackage).append('.').append(accessName(included.get(i)));
        wroteInterface = true;
      }
    }
    source.append(" {\n");
    if (entity.pooled) {
      source
          .append("  private static final java.util.concurrent.ConcurrentLinkedQueue<")
          .append(className)
          .append("> pool = new java.util.concurrent.ConcurrentLinkedQueue<>();\n");
    }

    for (ComponentModel component : included) {
      if (component.storage == Storage.Reference) {
        source
            .append("  private final ")
            .append(component.typeName)
            .append(' ')
            .append(componentPrefix(component))
            .append("Component = new ")
            .append(component.typeName)
            .append("();\n");
      } else {
        for (FieldModel field : component.fields) {
          String variable = fieldMethod(component, field);
          source.append("  private ").append(field.type).append(' ').append(variable).append(";\n");
          if (field.volatileField) {
            source
                .append("  private ")
                .append(field.type)
                .append(' ')
                .append(variable)
                .append("Back;\n");
          }
        }
      }
    }

    source
        .append("\n  private ")
        .append(className)
        .append("(){ super(\"")
        .append(escape(entity.name))
        .append("\", ")
        .append(mask)
        .append("L); prepareForUse(); }\n\n")
        .append("  public static ")
        .append(className)
        .append(" create(){\n");
    if (entity.pooled) {
      source
          .append("    ")
          .append(className)
          .append(" entity = pool.poll();\n")
          .append("    if(entity == null) return new ")
          .append(className)
          .append("();\n")
          .append("    entity.prepareForUse();\n")
          .append("    return entity;\n");
    } else {
      source.append("    return new ").append(className).append("();\n");
    }
    source.append("  }\n\n");
    source
        .append("  public static void free(")
        .append(className)
        .append(" entity){\n")
        .append("    if(entity == null) return;\n")
        .append("    entity.releaseForPool();\n");
    if (entity.pooled) source.append("    pool.offer(entity);\n");
    source.append("  }\n\n");

    for (ComponentModel component : included) {
      if (component.storage == Storage.Reference) {
        String method = componentPrefix(component) + "Component";
        source
            .append("  @Override public ")
            .append(component.typeName)
            .append(' ')
            .append(method)
            .append("(){ return ")
            .append(method)
            .append("; }\n");
      } else {
        for (FieldModel field : component.fields) {
          String method = fieldMethod(component, field);
          source
              .append("  @Override public ")
              .append(field.type)
              .append(' ')
              .append(method)
              .append("(){ return ")
              .append(method)
              .append("; }\n");
          if (!field.readonly) {
            source
                .append("  @Override public void ")
                .append(method)
                .append('(')
                .append(field.type)
                .append(" value){ this.")
                .append(method)
                .append(" = value; }\n");
          }
          if (field.volatileField) {
            source
                .append("  @Override public ")
                .append(field.type)
                .append(' ')
                .append(method)
                .append("Back(){ return ")
                .append(method)
                .append("Back; }\n")
                .append("  @Override public void ")
                .append(method)
                .append("Back(")
                .append(field.type)
                .append(" value){ this.")
                .append(method)
                .append("Back = value; }\n");
          }
        }
      }
    }

    source.append("\n  @Override protected void resetComponents(){\n");
    for (ComponentModel component : included) {
      for (FieldModel field : component.fields) {
        String expression = fieldExpression(component, field);
        String defaultValue = defaultValue(field);
        source.append("    ").append(expression).append(" = ").append(defaultValue).append(";\n");
        if (component.storage == Storage.Inline && field.volatileField) {
          source
              .append("    this.")
              .append(fieldMethod(component, field))
              .append("Back = ")
              .append(defaultValue)
              .append(";\n");
        }
      }
    }
    source.append("  }\n\n");

    if (entity.serializable) {
      source
          .append("  @Override public void write(java.io.DataOutput output) throws java.io.IOException{\n")
          .append("    writeBase(output);\n");
      int ordinal = 0;
      for (ComponentModel component : included) {
        for (FieldModel field : component.fields) {
          if (field.persist && isPersistable(field)) {
            appendWrite(source, field, fieldExpression(component, field));
          }
        }
      }
      source.append("  }\n\n");
      source
          .append("  @Override public void read(java.io.DataInput input) throws java.io.IOException{\n")
          .append("    readBase(input);\n");
      for (ComponentModel component : included) {
        for (FieldModel field : component.fields) {
          if (field.persist && isPersistable(field)) {
            ordinal = appendRead(source, field, fieldExpression(component, field), ordinal);
          }
        }
      }
      source.append("  }\n");
    } else {
      source
          .append("  @Override public void write(java.io.DataOutput output){ throw new UnsupportedOperationException(\"")
          .append(escape(entity.name))
          .append(" is not serializable\"); }\n")
          .append("  @Override public void read(java.io.DataInput input){ throw new UnsupportedOperationException(\"")
          .append(escape(entity.name))
          .append(" is not serializable\"); }\n");
    }
    source.append("}\n");
    writeSource(generatedPackage + "." + className, source.toString(), entity.element);
  }

  private void generateRegistry() throws IOException {
    String className = "GeneratedEcsRegistry";
    StringBuilder source = new StringBuilder(16384);
    source
        .append("package ")
        .append(generatedPackage)
        .append(";\n\n")
        .append("@javax.annotation.processing.Generated(\"")
        .append(EcsProcessor.class.getName())
        .append("\")\n")
        .append("public final class ")
        .append(className)
        .append(" {\n")
        .append("  private ")
        .append(className)
        .append("(){}\n")
        .append("  public static void register(caliniya.armavoke.ecs.runtime.EcsRegistry registry){\n");

    List<ThreadModel> sortedThreads = new ArrayList<>(threads.values());
    sortedThreads.sort(Comparator.comparing(thread -> thread.name));
    for (ThreadModel thread : sortedThreads) {
      source
          .append("    registry.registerThread(new caliniya.armavoke.ecs.runtime.EcsRegistry.ThreadConfig(\"")
          .append(escape(thread.name))
          .append("\", ")
          .append(thread.workers)
          .append(", ")
          .append(thread.priority)
          .append(", ")
          .append(thread.interruptible)
          .append("));\n");
    }

    List<ComponentModel> sortedComponents = new ArrayList<>(components.values());
    sortedComponents.sort(Comparator.comparing(component -> component.name));
    for (ComponentModel component : sortedComponents) {
      source
          .append("    registry.registerComponent(new caliniya.armavoke.ecs.runtime.EcsRegistry.ComponentConfig(\"")
          .append(escape(component.name))
          .append("\", \"")
          .append(escape(component.typeName))
          .append("\", ")
          .append(componentIndexes.get(component.typeName))
          .append(", ")
          .append(mask(component.requires))
          .append("L, ")
          .append(component.pure)
          .append(", ")
          .append(component.pooled)
          .append(", \"")
          .append(component.storage)
          .append("\", \"")
          .append(escape(component.updateBy))
          .append("\", ")
          .append(invoker(component, Lifecycle.Update))
          .append(", ")
          .append(invoker(component, Lifecycle.Initialize))
          .append(", ")
          .append(invoker(component, Lifecycle.Destroy))
          .append(", ")
          .append(fieldConfigs(component))
          .append(", ")
          .append(importConfigs(component))
          .append("));\n");
    }

    List<SystemModel> sortedSystems = new ArrayList<>(systems.values());
    sortedSystems.sort(
        Comparator.comparing((SystemModel system) -> system.thread)
            .thenComparingInt(system -> system.priority)
            .thenComparing(system -> system.name));
    for (SystemModel system : sortedSystems) {
      source
          .append("    registry.registerSystem(new caliniya.armavoke.ecs.runtime.EcsRegistry.SystemConfig(\"")
          .append(escape(system.name))
          .append("\", \"")
          .append(escape(system.thread))
          .append("\", ")
          .append(system.priority)
          .append(", ")
          .append(mask(system.reads))
          .append("L, ")
          .append(mask(system.writes))
          .append("L, ")
          .append(system.parallel)
          .append(", ")
          .append(system.interval)
          .append(", ")
          .append(stringArray(system.after))
          .append(", new ")
          .append(system.typeName)
          .append("()));\n");
    }

    List<EntityModel> sortedEntities = new ArrayList<>(entities.values());
    sortedEntities.sort(Comparator.comparing(entity -> entity.name));
    for (EntityModel entity : sortedEntities) {
      source
          .append("    registry.registerEntity(new caliniya.armavoke.ecs.runtime.EcsRegistry.EntityConfig(\"")
          .append(escape(entity.name))
          .append("\", \"")
          .append(generatedPackage)
          .append('.')
          .append(entity.generatedClass)
          .append("\", ")
          .append(mask(entity.components))
          .append("L, ")
          .append(entity.pooled)
          .append(", ")
          .append(entity.serializable)
          .append(", \"")
          .append(escape(entity.constructor))
          .append("\", ")
          .append(stringArray(entity.abilities))
          .append(", ")
          .append(stringArray(entity.modules))
          .append(", ")
          .append(entity.generatedClass)
          .append("::create, value -> ")
          .append(entity.generatedClass)
          .append(".free((")
          .append(entity.generatedClass)
          .append(")value)));\n");
    }

    source.append("    registry.freeze();\n  }\n}\n");
    writeSource(generatedPackage + "." + className, source.toString());
  }

  private String invoker(ComponentModel component, Lifecycle lifecycle) {
    List<MethodModel> methods =
        component.methods.stream()
            .filter(method -> method.lifecycle == lifecycle)
            .sorted(Comparator.comparingInt(method -> method.order))
            .collect(Collectors.toList());
    if (methods.isEmpty()) return "null";
    String access = accessPackage + "." + accessName(component);
    String accessor = componentPrefix(component) + "Component()";
    StringBuilder code =
        new StringBuilder("(entity, delta) -> { if(entity instanceof ")
            .append(access)
            .append(" access){ ");
    for (MethodModel method : methods) {
      code
          .append("access.")
          .append(accessor)
          .append('.')
          .append(method.name)
          .append('(');
      if (method.parameterTypes.size() == 1) code.append("delta");
      code.append("); ");
    }
    return code.append("} }").toString();
  }

  private String fieldConfigs(ComponentModel component) {
    StringBuilder code =
        new StringBuilder("new caliniya.armavoke.ecs.runtime.EcsRegistry.FieldConfig[]{");
    for (int i = 0; i < component.fields.size(); i++) {
      FieldModel field = component.fields.get(i);
      if (i > 0) code.append(", ");
      code
          .append("new caliniya.armavoke.ecs.runtime.EcsRegistry.FieldConfig(\"")
          .append(escape(field.name))
          .append("\", \"")
          .append(escape(field.type))
          .append("\", ")
          .append(field.volatileField)
          .append(", ")
          .append(field.readonly)
          .append(", ")
          .append(field.persist)
          .append(", \"")
          .append(escape(defaultValue(field)))
          .append("\")");
    }
    return code.append('}').toString();
  }

  private String importConfigs(ComponentModel component) {
    StringBuilder code =
        new StringBuilder("new caliniya.armavoke.ecs.runtime.EcsRegistry.ImportConfig[]{");
    for (int i = 0; i < component.imports.size(); i++) {
      ImportModel imported = component.imports.get(i);
      if (i > 0) code.append(", ");
      code
          .append("new caliniya.armavoke.ecs.runtime.EcsRegistry.ImportConfig(\"")
          .append(escape(imported.componentType))
          .append("\", ")
          .append(stringArray(imported.fields))
          .append(", \"")
          .append(imported.mode)
          .append("\")");
    }
    return code.append('}').toString();
  }

  private void appendWrite(StringBuilder source, FieldModel field, String expression) {
    switch (field.kind) {
      case BOOLEAN -> source.append("    output.writeBoolean(").append(expression).append(");\n");
      case BYTE -> source.append("    output.writeByte(").append(expression).append(");\n");
      case SHORT -> source.append("    output.writeShort(").append(expression).append(");\n");
      case INT -> source.append("    output.writeInt(").append(expression).append(");\n");
      case LONG -> source.append("    output.writeLong(").append(expression).append(");\n");
      case CHAR -> source.append("    output.writeChar(").append(expression).append(");\n");
      case FLOAT -> source.append("    output.writeFloat(").append(expression).append(");\n");
      case DOUBLE -> source.append("    output.writeDouble(").append(expression).append(");\n");
      default -> {
        if (field.type.equals("java.lang.String")) {
          source
              .append("    output.writeBoolean(")
              .append(expression)
              .append(" != null);\n")
              .append("    if(")
              .append(expression)
              .append(" != null) output.writeUTF(")
              .append(expression)
              .append(");\n");
        } else if (field.enumType) {
          source
              .append("    output.writeInt(")
              .append(expression)
              .append(" == null ? -1 : ")
              .append(expression)
              .append(".ordinal());\n");
        }
      }
    }
  }

  private int appendRead(
      StringBuilder source, FieldModel field, String expression, int ordinalIndex) {
    switch (field.kind) {
      case BOOLEAN -> source.append("    ").append(expression).append(" = input.readBoolean();\n");
      case BYTE -> source.append("    ").append(expression).append(" = input.readByte();\n");
      case SHORT -> source.append("    ").append(expression).append(" = input.readShort();\n");
      case INT -> source.append("    ").append(expression).append(" = input.readInt();\n");
      case LONG -> source.append("    ").append(expression).append(" = input.readLong();\n");
      case CHAR -> source.append("    ").append(expression).append(" = input.readChar();\n");
      case FLOAT -> source.append("    ").append(expression).append(" = input.readFloat();\n");
      case DOUBLE -> source.append("    ").append(expression).append(" = input.readDouble();\n");
      default -> {
        if (field.type.equals("java.lang.String")) {
          source
              .append("    ")
              .append(expression)
              .append(" = input.readBoolean() ? input.readUTF() : null;\n");
        } else if (field.enumType) {
          String variable = "enumOrdinal" + ordinalIndex++;
          source
              .append("    int ")
              .append(variable)
              .append(" = input.readInt();\n")
              .append("    ")
              .append(expression)
              .append(" = ")
              .append(variable)
              .append(" < 0 ? null : ")
              .append(field.type)
              .append(".values()[")
              .append(variable)
              .append("];\n");
        }
      }
    }
    return ordinalIndex;
  }

  private boolean isPersistable(FieldModel field) {
    return field.kind.isPrimitive()
        || field.type.equals("java.lang.String")
        || field.enumType;
  }

  private String fieldExpression(ComponentModel component, FieldModel field) {
    if (component.storage == Storage.Reference) {
      return "this." + componentPrefix(component) + "Component." + field.name;
    }
    return "this." + fieldMethod(component, field);
  }

  private String defaultValue(FieldModel field) {
    if (!field.defaultValue.isBlank()) return field.defaultValue;
    return switch (field.kind) {
      case BOOLEAN -> "false";
      case BYTE, SHORT, INT -> "0";
      case LONG -> "0L";
      case CHAR -> "'\\0'";
      case FLOAT -> "0f";
      case DOUBLE -> "0d";
      default -> "null";
    };
  }

  private String accessName(ComponentModel component) {
    return javaName(component.name) + "Access";
  }

  private String componentPrefix(ComponentModel component) {
    String name = javaName(component.name);
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  private String fieldMethod(ComponentModel component, FieldModel field) {
    return componentPrefix(component) + capitalize(field.name);
  }

  private String javaName(String value) {
    StringBuilder result = new StringBuilder();
    boolean uppercase = true;
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      if (!Character.isJavaIdentifierPart(character)) {
        uppercase = true;
      } else {
        result.append(uppercase ? Character.toUpperCase(character) : character);
        uppercase = false;
      }
    }
    if (result.isEmpty()) return "Generated";
    if (!Character.isJavaIdentifierStart(result.charAt(0))) result.insert(0, '_');
    return result.toString();
  }

  private String capitalize(String value) {
    if (value.isEmpty()) return value;
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  private long mask(Collection<String> componentTypes) {
    long result = 0L;
    for (String type : componentTypes) {
      Integer index = componentIndexes.get(type);
      if (index != null) result |= 1L << index;
    }
    return result;
  }

  private String stringArray(Collection<String> values) {
    return values.stream()
        .map(value -> "\"" + escape(value) + "\"")
        .collect(Collectors.joining(", ", "new String[]{", "}"));
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private List<String> classValues(Element element, String annotationName, String memberName) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(annotationName)) continue;
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
          elementUtils.getElementValuesWithDefaults(mirror).entrySet()) {
        if (!entry.getKey().getSimpleName().contentEquals(memberName)) continue;
        Object raw = entry.getValue().getValue();
        if (!(raw instanceof List<?> values)) return List.of();
        List<String> result = new ArrayList<>(values.size());
        for (Object value : values) {
          Object item = ((AnnotationValue) value).getValue();
          if (item instanceof TypeMirror type) result.add(type.toString());
        }
        return result;
      }
    }
    return List.of();
  }

  private boolean isEnum(TypeMirror type) {
    Element element = typeUtils.asElement(type);
    return element != null && element.getKind() == ElementKind.ENUM;
  }

  private void writeSource(String qualifiedName, String source, Element... origins)
      throws IOException {
    JavaFileObject file = filer.createSourceFile(qualifiedName, origins);
    try (Writer writer = file.openWriter()) {
      writer.write(source);
    }
  }

  private void error(Element element, String message) {
    invalid = true;
    if (element == null) messager.printMessage(Diagnostic.Kind.ERROR, message);
    else messager.printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  private void warning(Element element, String message) {
    if (element == null) messager.printMessage(Diagnostic.Kind.WARNING, message);
    else messager.printMessage(Diagnostic.Kind.WARNING, message, element);
  }

  private static final class ThreadModel {
    final TypeElement element;
    final String name;
    final int workers;
    final int priority;
    final boolean interruptible;

    ThreadModel(
        TypeElement element, String name, int workers, int priority, boolean interruptible) {
      this.element = element;
      this.name = name;
      this.workers = workers;
      this.priority = priority;
      this.interruptible = interruptible;
    }
  }

  private static final class ComponentModel {
    final TypeElement element;
    final String name;
    final String typeName;
    final String updateBy;
    final boolean pure;
    final boolean pooled;
    final Storage storage;
    final List<String> requires;
    final List<FieldModel> fields = new ArrayList<>();
    final List<MethodModel> methods = new ArrayList<>();
    final List<ImportModel> imports = new ArrayList<>();

    ComponentModel(
        TypeElement element,
        String name,
        String typeName,
        String updateBy,
        boolean pure,
        boolean pooled,
        Storage storage,
        List<String> requires) {
      this.element = element;
      this.name = name;
      this.typeName = typeName;
      this.updateBy = updateBy;
      this.pure = pure;
      this.pooled = pooled;
      this.storage = storage;
      this.requires = requires;
    }
  }

  private static final class FieldModel {
    final VariableElement element;
    final String name;
    final String type;
    final TypeKind kind;
    final boolean volatileField;
    final boolean readonly;
    final String defaultValue;
    final boolean persist;
    final boolean enumType;

    FieldModel(
        VariableElement element,
        String name,
        String type,
        TypeKind kind,
        boolean volatileField,
        boolean readonly,
        String defaultValue,
        boolean persist,
        boolean enumType) {
      this.element = element;
      this.name = name;
      this.type = type;
      this.kind = kind;
      this.volatileField = volatileField;
      this.readonly = readonly;
      this.defaultValue = defaultValue;
      this.persist = persist;
      this.enumType = enumType;
    }
  }

  private static final class MethodModel {
    final ExecutableElement element;
    final String name;
    final int order;
    final Lifecycle lifecycle;
    final List<String> parameterTypes;

    MethodModel(
        ExecutableElement element,
        String name,
        int order,
        Lifecycle lifecycle,
        List<String> parameterTypes) {
      this.element = element;
      this.name = name;
      this.order = order;
      this.lifecycle = lifecycle;
      this.parameterTypes = parameterTypes;
    }
  }

  private static final class ImportModel {
    final String componentType;
    final List<String> fields;
    final AccessMode mode;

    ImportModel(String componentType, List<String> fields, AccessMode mode) {
      this.componentType = componentType;
      this.fields = fields;
      this.mode = mode;
    }
  }

  private static final class SystemModel {
    final TypeElement element;
    final String name;
    final String typeName;
    final String thread;
    final int priority;
    final int interval;
    final boolean parallel;
    final List<String> reads;
    final List<String> writes;
    final List<String> after;

    SystemModel(
        TypeElement element,
        String name,
        String typeName,
        String thread,
        int priority,
        int interval,
        boolean parallel,
        List<String> reads,
        List<String> writes,
        List<String> after) {
      this.element = element;
      this.name = name;
      this.typeName = typeName;
      this.thread = thread;
      this.priority = priority;
      this.interval = interval;
      this.parallel = parallel;
      this.reads = reads;
      this.writes = writes;
      this.after = after;
    }
  }

  private static final class EntityModel {
    final TypeElement element;
    final String name;
    final String typeName;
    final String generatedClass;
    final boolean pooled;
    final boolean serializable;
    final String constructor;
    final List<String> components;
    final List<String> abilities;
    final List<String> modules;
    final List<String> interfaces;

    EntityModel(
        TypeElement element,
        String name,
        String typeName,
        String generatedClass,
        boolean pooled,
        boolean serializable,
        String constructor,
        List<String> components,
        List<String> abilities,
        List<String> modules,
        List<String> interfaces) {
      this.element = element;
      this.name = name;
      this.typeName = typeName;
      this.generatedClass = generatedClass;
      this.pooled = pooled;
      this.serializable = serializable;
      this.constructor = constructor;
      this.components = components;
      this.abilities = abilities;
      this.modules = modules;
      this.interfaces = interfaces;
    }
  }
}
