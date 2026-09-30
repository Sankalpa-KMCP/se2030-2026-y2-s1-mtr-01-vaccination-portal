package com.se2030.vaccinationportal.architecture;

import com.se2030.vaccinationportal.VaccinationPortalApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.ConstantDynamic;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Handle;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.RecordComponentVisitor;
import org.springframework.asm.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiled dependency rules using the ASM already supplied by Spring.
 * Unlike import-text checks, this inspects signatures, annotations, field access,
 * method calls, class literals and lambda bootstrap arguments, including fully qualified types.
 * No additional build dependency is needed.
 */
class FeatureBoundaryTests {
    private static final String ROOT = "com/se2030/vaccinationportal/";
    private static final Set<String> FEATURES = Set.of(
            "appointment", "assistant", "campaign", "inventory", "screening", "vaccination");
    private static final Set<String> APPLICATION_SERVICES = Set.of(
            ROOT + "shared/auth/service/CitizenProfileService",
            ROOT + "vaccination/certificate/service/DigitalCertificateService");
    private static Map<String, ClassFacts> classes;

    @BeforeAll
    static void readProductionClasses() throws Exception {
        Path output = Path.of(VaccinationPortalApplication.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        classes = new HashMap<>();
        try (var files = Files.walk(output.resolve(ROOT))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassFacts facts = read(Files.readAllBytes(file));
                classes.put(facts.name, facts);
            }
        }
        assertThat(classes).isNotEmpty();
    }

    @Test
    void featuresDoNotAccessAnotherOwnersRepositoriesOrEntities() {
        List<String> violations = new ArrayList<>();
        for (ClassFacts source : classes.values()) {
            for (String dependency : source.dependencies) {
                ClassFacts target = classes.get(dependency);
                if (target != null && differentOwners(source.name, target.name)
                        && (target.entity || target.repository)) {
                    violations.add(source.name + " -> " + target.name);
                }
            }
        }
        assertThat(violations).as("cross-feature persistence access").isEmpty();
    }

    @Test
    void sharedInfrastructureDoesNotDependOnBusinessFeatures() {
        assertThat(edges((source, target) -> owner(source).equals("shared")
                && FEATURES.contains(owner(target)))).isEmpty();
    }

    @Test
    void publicFeatureApisDoNotDependOnConsumerFeaturesOrPersistence() {
        assertThat(edges((source, target) -> source.contains("/api/")
                && FEATURES.contains(owner(source))
                && ((FEATURES.contains(owner(target)) && differentOwners(source, target))
                || (classes.containsKey(target)
                    && (classes.get(target).entity || classes.get(target).repository))
                || target.contains("/service/") || target.contains("/adapter/")
                || simpleName(target).startsWith("Jpa")))).isEmpty();
    }

    @Test
    void assistantUsesPublicQueriesAndTheTwoRetainedApplicationServices() {
        assertThat(edges((source, target) -> owner(source).equals("assistant")
                && !owner(target).equals("assistant")
                && (target.contains("/service/") || simpleName(target).startsWith("Jpa"))
                && !APPLICATION_SERVICES.contains(target))).isEmpty();
    }

    @Test
    void businessFeaturesDoNotDependOnAssistantAndCampaignDoesNotDependOnAppointment() {
        assertThat(edges((source, target) -> (FEATURES.contains(owner(source))
                && !owner(source).equals("assistant") && owner(target).equals("assistant"))
                || (owner(source).equals("campaign") && owner(target).equals("appointment")))).isEmpty();
    }

    @Test
    void crossFeatureInternalServicesAreNotUsedExceptDocumentedApplicationApis() {
        assertThat(edges((source, target) -> differentOwners(source, target)
                && (target.contains("/service/") || simpleName(target).startsWith("Jpa"))
                && !(owner(source).equals("assistant") && APPLICATION_SERVICES.contains(target)))).isEmpty();
    }

    @Test
    void crossFeatureEntityQueriesHaveOnlyTheDocumentedSessionImpactException() {
        Set<String> exceptions = new HashSet<>();
        List<String> violations = new ArrayList<>();
        String approved = ROOT + "appointment/persistence/repository/AppointmentRepository"
                + "#countBySessionIdAndStatusIn -> " + ROOT + "campaign/session/planning/model/TimeSlot";
        for (ClassFacts source : classes.values()) {
            for (var query : source.queries.entrySet()) {
                for (ClassFacts target : classes.values()) {
                    if (!target.entity || !differentOwners(source.name, target.name)) continue;
                    Pattern entityName = Pattern.compile("\\b" + Pattern.quote(target.entityName) + "\\b");
                    if (query.getValue().stream().anyMatch(q -> entityName.matcher(q).find())) {
                        String edge = source.name + "#" + query.getKey() + " -> " + target.name;
                        if (edge.equals(approved)) exceptions.add(edge); else violations.add(edge);
                    }
                }
            }
        }
        assertThat(violations).as("undocumented JPQL entity joins across features").isEmpty();
        // Retain the original single-statement impact count under Campaign's session lock.
        // Replacing this with separate slot-ID/count reads would alter its consistency semantics.
        assertThat(exceptions).containsExactly(approved);
    }

    @Test
    void collectorDetectsFullyQualifiedPersistenceFieldsWithoutImportStatements() throws IOException {
        String resource = "/" + getClass().getName().replace('.', '/') + "$PersistenceFixture.class";
        try (var input = getClass().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            ClassFacts facts = read(input.readAllBytes());
            assertThat(facts.dependencies).contains(
                    ROOT + "appointment/persistence/repository/AppointmentRepository",
                    ROOT + "campaign/core/model/VaccinationCampaign",
                    ROOT + "screening/submission/model/ScreeningSubmission");
        }
    }

    private static class PersistenceFixture {
        private com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository repository;
        private com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign campaign;
        private List<com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission> submissions;
    }

    private interface EdgeRule { boolean violates(String source, String target); }

    private static List<String> edges(EdgeRule rule) {
        List<String> result = new ArrayList<>();
        for (ClassFacts source : classes.values()) {
            for (String target : source.dependencies) {
                if (target.startsWith(ROOT) && rule.violates(source.name, target)) {
                    result.add(source.name + " -> " + target);
                }
            }
        }
        return result;
    }

    private static String owner(String name) {
        return name.startsWith(ROOT) ? name.substring(ROOT.length()).split("/")[0] : "external";
    }

    private static boolean differentOwners(String source, String target) {
        return !owner(source).equals(owner(target));
    }

    private static String simpleName(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    private static final class ClassFacts {
        String name;
        boolean entity;
        boolean repository;
        String entityName;
        final Set<String> dependencies = new HashSet<>();
        final Map<String, List<String>> queries = new HashMap<>();
    }

    private static ClassFacts read(byte[] bytes) {
        ClassFacts facts = new ClassFacts();
        new ClassReader(bytes).accept(new DependencyCollector(facts), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return facts;
    }

    private static final class DependencyCollector extends ClassVisitor {
        private final ClassFacts facts;

        DependencyCollector(ClassFacts facts) { super(Opcodes.ASM9); this.facts = facts; }

        private void dependency(String name) {
            if (name != null) facts.dependencies.add(name);
        }

        private void descriptor(String descriptor) {
            if (descriptor == null) return;
            type(Type.getType(descriptor));
        }

        private void type(Type type) {
            switch (type.getSort()) {
                case Type.OBJECT -> dependency(type.getInternalName());
                case Type.ARRAY -> type(type.getElementType());
                case Type.METHOD -> {
                    type(type.getReturnType());
                    for (Type argument : type.getArgumentTypes()) type(argument);
                }
                default -> { }
            }
        }

        private void signature(String signature) {
            if (signature == null) return;
            // JVMS generic signatures encode class names after L; type arguments
            // start with <. Capture each class token, including nested type arguments.
            var names = Pattern.compile("L([^;:<.]+)").matcher(signature);
            while (names.find()) dependency(names.group(1));
        }

        private void constant(Object value) {
            if (value instanceof Type type) type(type);
            if (value instanceof Handle handle) { dependency(handle.getOwner()); descriptor(handle.getDesc()); }
            if (value instanceof ConstantDynamic dynamic) {
                descriptor(dynamic.getDescriptor()); constant(dynamic.getBootstrapMethod());
                for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) constant(dynamic.getBootstrapMethodArgument(i));
            }
        }

        private AnnotationVisitor annotation(String descriptor, String method) {
            descriptor(descriptor);
            boolean entity = descriptor.equals("Ljakarta/persistence/Entity;");
            boolean query = descriptor.equals("Lorg/springframework/data/jpa/repository/Query;");
            if (entity) facts.entity = true;
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override public void visit(String name, Object value) {
                    constant(value);
                    if (entity && name.equals("name") && value instanceof String s && !s.isBlank()) facts.entityName = s;
                    if (query && name.equals("value") && value instanceof String s) {
                        facts.queries.computeIfAbsent(method, key -> new ArrayList<>()).add(s);
                    }
                }
                @Override public void visitEnum(String name, String descriptor, String value) { descriptor(descriptor); }
                @Override public AnnotationVisitor visitArray(String name) { return this; }
                @Override public AnnotationVisitor visitAnnotation(String name, String descriptor) { return annotation(descriptor, method); }
            };
        }

        @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            facts.name = name; facts.entityName = simpleName(name);
            facts.repository = name.contains("/repository/");
            dependency(superName); signature(signature);
            for (String item : interfaces) {
                dependency(item);
                if (item.startsWith("org/springframework/data/repository/")
                        || item.startsWith("org/springframework/data/jpa/repository/")) facts.repository = true;
            }
        }

        @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) { return annotation(descriptor, ""); }

        @Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            descriptor(descriptor); signature(signature); constant(value);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) { return annotation(descriptor, ""); }
            };
        }

        @Override public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
            descriptor(descriptor); signature(signature);
            return new RecordComponentVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) { return annotation(descriptor, ""); }
            };
        }

        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            descriptor(descriptor); signature(signature);
            if (exceptions != null) for (String exception : exceptions) dependency(exception);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) { return annotation(descriptor, name); }
                @Override public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) { return annotation(descriptor, name); }
                @Override public void visitTypeInsn(int opcode, String type) { if (type.startsWith("[")) descriptor(type); else dependency(type); }
                @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) { dependency(owner); descriptor(descriptor); }
                @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) { dependency(owner); descriptor(descriptor); }
                @Override public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethod, Object... arguments) {
                    descriptor(descriptor); constant(bootstrapMethod); for (Object argument : arguments) constant(argument);
                }
                @Override public void visitLdcInsn(Object value) { constant(value); }
                @Override public void visitMultiANewArrayInsn(String descriptor, int dimensions) { descriptor(descriptor); }
                @Override public void visitTryCatchBlock(org.springframework.asm.Label start, org.springframework.asm.Label end,
                        org.springframework.asm.Label handler, String type) { dependency(type); }
            };
        }
    }
}
