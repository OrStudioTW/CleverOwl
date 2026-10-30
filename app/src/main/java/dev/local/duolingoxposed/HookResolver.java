package dev.local.duolingoxposed;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/** Each resolver must return one unambiguous structural match or fail closed. */
final class HookResolver {
    static final String USER = "com.duolingo.data.user.User";
    static final String LEVEL = "com.duolingo.data.user.SubscriberLevel";
    static final String INVENTORY = "com.duolingo.data.shop.InventoryItem";
    static final String TICK = "com.duolingo.data.session.state.RampupTimerState$Tick";

    static Class<?> user(DexKitBridge bridge, ClassLoader loader) throws ReflectiveOperationException {
        if (bridge == null) return Class.forName(USER, false, loader);
        return unique(bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .name("toString").paramCount(0).returnType("java.lang.String")
                .usingStrings("User(adsConfig=")))).getClassInstance(loader);
    }

    static Constructor<?> xpPreview(DexKitBridge bridge, ClassLoader loader) throws ReflectiveOperationException {
        if (bridge == null) return Class.forName("enr", false, loader).getDeclaredConstructor(
                Class.forName("com.duolingo.feature.newpath.ui.model.NewPathPopupData$Action$Verb", false, loader), int.class, boolean.class);
        Class<?> model = unique(bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .name("toString").paramCount(0).returnType("java.lang.String")
                .usingStrings("Learn(verb=", ", expectedXp=", ", isBoosted=")))).getClassInstance(loader);
        return model.getDeclaredConstructor(Class.forName("com.duolingo.feature.newpath.ui.model.NewPathPopupData$Action$Verb", false, loader),
                int.class, boolean.class);
    }

    static Constructor<?> practice(DexKitBridge bridge, ClassLoader loader, String anchor, int[] primitivePositions,
                                    int parameterCount) throws ReflectiveOperationException {
        String profileName;
        switch (anchor) {
            case "TimedLightningPractice(initialSessionTime=": profileName = "wt70"; break;
            case "TimedMatchPractice(initialSessionTime=": profileName = "xt70"; break;
            case "TimedMultiSessionPractice(sessionXp=": profileName = "yt70"; break;
            default: throw new IllegalArgumentException("Unknown practice anchor");
        }
        Class<?> model = bridge == null ? Class.forName(profileName, false, loader) : unique(bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                .name("toString").paramCount(0).returnType("java.lang.String").usingStrings(anchor)))).getClassInstance(loader);
        List<Constructor<?>> candidates = new ArrayList<>();
        for (Constructor<?> c : model.getDeclaredConstructors()) {
            Class<?>[] types = c.getParameterTypes();
            if (types.length != parameterCount) continue;
            boolean valid = true;
            for (int position : primitivePositions) valid &= types[position] == int.class;
            if (valid) candidates.add(c);
        }
        return unique(candidates);
    }

    static Constructor<?> xpInputs(DexKitBridge bridge, ClassLoader loader) throws ReflectiveOperationException {
        MethodData text = bridge == null ? null : unique(bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().name("toString").paramCount(0).returnType("java.lang.String")
                        .usingStrings("XpBoostMultiplierInputs(hasActiveXpBoostItem=", ", highestActiveXpBoostMultiplier="))));
        Class<?> model = bridge == null ? Class.forName("eqb0", false, loader) : text.getClassInstance(loader);
        List<Field> fields = instanceFields(model);
        if (fields.size() != 2 || countType(fields, boolean.class) != 1 || countType(fields, Double.class) != 1)
            throw new IllegalStateException("XP model changed");
        return model.getDeclaredConstructor(Double.class, boolean.class);
    }

    static Constructor<?> timer(DexKitBridge bridge, ClassLoader loader) throws ReflectiveOperationException {
        if (bridge == null) return Class.forName(TICK, false, loader).getDeclaredConstructor(int.class);
        MethodData text = unique(bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().declaredClass(TICK).name("toString").paramCount(0)
                        .returnType("java.lang.String").usingStrings("Tick(remainingSeconds="))));
        Class<?> type = text.getClassInstance(loader);
        List<Field> fields = instanceFields(type);
        if (fields.size() != 1 || fields.get(0).getType() != int.class)
            throw new IllegalStateException("Timer model changed");
        if (!type.getSuperclass().getName().equals("com.duolingo.data.session.state.RampupTimerState"))
            throw new IllegalStateException("Timer parent changed");
        return type.getDeclaredConstructor(int.class);
    }

    record InventoryAccess(Method remaining, Method active, Field expiry, List<Field> strings, Set<String> boostIds) {}

    static InventoryAccess inventory(DexKitBridge bridge, ClassLoader loader) throws ReflectiveOperationException {
        if (bridge == null) {
            Class<?> model = Class.forName(INVENTORY, false, loader);
            Method remaining = model.getDeclaredMethod("b");
            Method active = model.getDeclaredMethod("c");
            Field expiry = model.getDeclaredField("g");
            if (remaining.getReturnType() != long.class || active.getReturnType() != boolean.class || expiry.getType() != long.class)
                throw new IllegalStateException("Inventory profile changed");
            return inventoryAccess(remaining, active, expiry, loader);
        }
        MethodData remaining = unique(bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().declaredClass(INVENTORY).paramCount(0).returnType("long")))
                .stream().filter(m -> m.getInvokes().stream().anyMatch(call ->
                        call.getDescriptor().equals("Landroid/os/SystemClock;->elapsedRealtime()J"))).collect(Collectors.toList()));
        Method method = remaining.getMethodInstance(loader);
        List<Field> expiryFields = new ArrayList<>();
        for (var access : remaining.getUsingFields()) {
            Field field = access.getField().getFieldInstance(loader);
            if (field.getDeclaringClass() == method.getDeclaringClass() && field.getType() == long.class
                    && !Modifier.isStatic(field.getModifiers()) && !expiryFields.contains(field)) expiryFields.add(field);
        }
        Field expiry = unique(expiryFields);
        expiry.setAccessible(true);
        MethodData active = unique(bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().declaredClass(INVENTORY).paramCount(0).returnType("boolean")))
                .stream().filter(m -> m.getInvokes().stream().anyMatch(call ->
                        call.getDescriptor().equals(remaining.getDescriptor()))).collect(Collectors.toList()));
        return inventoryAccess(method, active.getMethodInstance(loader), expiry, loader);
    }

    private static InventoryAccess inventoryAccess(Method method, Method active, Field expiry, ClassLoader loader) throws ReflectiveOperationException {
        expiry.setAccessible(true);
        List<Field> strings = new ArrayList<>();
        for (Field f : instanceFields(method.getDeclaringClass())) if (f.getType() == String.class) {
            f.setAccessible(true);
            strings.add(f);
        }
        if (strings.isEmpty()) throw new IllegalStateException("Inventory item identifier missing");
        Set<String> ids = new HashSet<>();
        ids.add("xp_boost_stackable");
        Class<?> predefined = Class.forName("com.duolingo.core.xpboost.XpBoostSource$Predefined", false, loader);
        Method legacyId = predefined.getDeclaredMethod("getLegacyId");
        if (legacyId.getReturnType() != String.class || !predefined.isEnum())
            throw new IllegalStateException("XP source model changed");
        for (Object source : predefined.getEnumConstants()) {
            Object id = legacyId.invoke(source);
            if (id instanceof String && !((String) id).isEmpty()) ids.add((String) id);
        }
        return new InventoryAccess(method, active, expiry, strings, ids);
    }

    static boolean isBoost(InventoryAccess access, Object item) throws IllegalAccessException {
        int matches = 0;
        for (Field f : access.strings()) if (access.boostIds().contains(f.get(item))) matches++;
        return matches == 1;
    }

    private static List<Field> instanceFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers())) fields.add(f);
        return fields;
    }

    private static long countType(List<Field> fields, Class<?> type) {
        return fields.stream().filter(f -> f.getType() == type).count();
    }

    private static <T> T unique(List<T> matches) {
        if (matches.size() != 1) throw new IllegalStateException("Expected one match, got " + matches.size());
        return matches.get(0);
    }
}
