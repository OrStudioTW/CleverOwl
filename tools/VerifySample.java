import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.stream.*;

/** Offline assertions against the original APK, using dexlib2 from JADX 1.5.6.
 * Usage: java -Xmx2g -cp jadx-1.5.6-all.jar tools/VerifySample.java base.apk
 * This does NOT execute Android hooks or establish server-side effects.
 */
class VerifySample {
    static final String TICK = "Lcom/duolingo/data/session/state/RampupTimerState$Tick;";
    static final Map<String, ClassDef> classes = new HashMap<>();
    static int checks;
    public static void main(String[] args) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            byte[] buffer = new byte[65536];
            for (int n; (n = input.read(buffer)) != -1;) hash.update(buffer, 0, n);
        }
        check(HexFormat.of().formatHex(hash.digest()).equals("30d915c7e55423f071157e6ce9064bae8cd7e65ed09bb99383bae505709f456c"), "Exact inspected APK SHA256");
        var dex = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.getDefault());
        for (var entry : dex.getDexEntryNames()) for (var cls : dex.getEntry(entry).getDexFile().getClasses()) classes.put(cls.getType(), cls);
        var matches = new ArrayList<Method>();
        for (var cls : classes.values()) for (var method : cls.getMethods()) {
            if (method.getName().equals("toString") && method.getParameterTypes().isEmpty()
                    && method.getReturnType().equals("Ljava/lang/String;")
                    && refs(method).containsAll(List.of("XpBoostMultiplierInputs(hasActiveXpBoostItem=", ", highestActiveXpBoostMultiplier="))) matches.add(method);
        }
        check(matches.size() == 1 && matches.get(0).getDefiningClass().equals("Leqb0;"), "Unique XP anchors across all DEX classes");
        var xp = classes.get(matches.get(0).getDefiningClass());
        check(fields(xp).size() == 2 && fields(xp).values().containsAll(List.of("Z", "Ljava/lang/Double;")), "XP input field shape");
        method(xp, "<init>", List.of("Ljava/lang/Double;", "Z"), "V");
        check(refs(method(classes.get("Ldqb0;"), "a", List.of("Lcom/duolingo/data/user/User;"), "D")).contains("Leqb0;->b:Ljava/lang/Double;"), "XP calculation consumes inspected multiplier");
        // Every deoptimized method must be an exact direct caller in this APK.
        Path profile = Path.of(args.length > 1 ? args[1] : "app/src/main/java/dev/local/duolingoxposed/XpCallers2488.java");
        Set<String> declaredCallers = new TreeSet<>();
        for (String line : Files.readAllLines(profile)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"L") && trimmed.endsWith("\","))
                declaredCallers.add(trimmed.substring(1, trimmed.length() - 2));
        }
        Set<String> actualCallers = new TreeSet<>();
        Set<String> xpTargets = Set.of("Ldqb0;->a(Lcom/duolingo/data/user/User;)D",
                "Ldqb0;->b(Lcom/duolingo/data/user/User;)Leqb0;", "Leqb0;-><init>(Ljava/lang/Double;Z)V");
        for (var cls : classes.values()) for (var m : cls.getMethods()) {
            if (!m.getName().equals("<clinit>") && refs(m).stream().anyMatch(xpTargets::contains)) actualCallers.add(m.toString());
        }
        check(declaredCallers.size() == 53 && declaredCallers.equals(actualCallers), "Exact profile covers all 53 ordinary XP call sites; static initializer excluded");
        for (String key : declaredCallers) {
            String owner = key.substring(0, key.indexOf("->"));
            check(methods(classes.get(owner)).stream().anyMatch(m -> m.toString().equals(key)), "XP caller exact descriptor " + key);
        }

        var user = fields(classes.get("Lcom/duolingo/data/user/User;"));
        check(user.entrySet().containsAll(Map.of("B0", "Lcom/duolingo/data/user/SubscriberLevel;", "y", "Z", "Q0", "Z", "R0", "Z", "S0", "Z", "T0", "I", "Y0", "Z").entrySet()), "Membership profile field names and types");
        var userClass = classes.get("Lcom/duolingo/data/user/User;");
        for (int count : new int[]{85, 109}) {
            var constructors = methods(userClass).stream().filter(m -> m.getName().equals("<init>") && m.getParameterTypes().size() == count).toList();
            check(constructors.size() == 1, "Unique User constructor with " + count + " parameters");
            Method constructor = constructors.get(0);
            int offset = count == 85 ? 0 : 4;
            check(constructor.getParameterTypes().get(24 + offset).toString().equals("Z")
                    && constructor.getParameterTypes().get(79 + offset).toString().equals("Lcom/duolingo/data/user/SubscriberLevel;"), "User argument types match revised profile " + count);
            check(directParameterStore(constructor, "y", 24 + offset), "hasPlus is written from argument " + (24 + offset));
            check(directParameterStore(constructor, "B0", 79 + offset), "SubscriberLevel is written from argument " + (79 + offset));
        }
        String[] practiceClasses = {"Lwt70;", "Lxt70;", "Lyt70;"};
        int[] counts = {6, 15, 7};
        int[] timePositions = {0, 0, 1};
        String[] timeFields = {"a", "a", "b"};
        String[] anchors = {"TimedLightningPractice(initialSessionTime=", "TimedMatchPractice(initialSessionTime=", "TimedMultiSessionPractice(sessionXp="};
        for (int i = 0; i < practiceClasses.length; i++) {
            var model = classes.get(practiceClasses[i]);
            var constructors = methods(model).stream().filter(m -> m.getName().equals("<init>")).toList();
            check(constructors.size() == 1 && constructors.get(0).getParameterTypes().size() == counts[i], "Practice constructor shape " + model.getType());
            check(refs(method(model, "toString", List.of(), "Ljava/lang/String;")).contains(anchors[i]), "Practice anchor " + model.getType());
            check(directParameterStore(constructors.get(0), timeFields[i], timePositions[i]), "Practice time argument " + model.getType());
        }
        var enums = classes.get("Lcom/duolingo/data/user/SubscriberLevel;");
        var names = new HashSet<String>();
        for (var f : enums.getStaticFields()) names.add(f.getName());
        check(names.containsAll(List.of("PREMIUM", "GOLD")), "Super and Max enum values exist");

        var preview = classes.get("Lenr;");
        var previewConstructor = method(preview, "<init>", List.of("Lcom/duolingo/feature/newpath/ui/model/NewPathPopupData$Action$Verb;", "I", "Z"), "V");
        check(directParameterStore(previewConstructor, "b", 1), "Path button expected XP argument maps to field b");
        var producer = method(classes.get("Lqvr;"), "invokeSuspend", List.of("Ljava/lang/Object;"), "Ljava/lang/Object;");
        check(refs(producer).containsAll(List.of("Leqb0;->a:Z", "Leqb0;->b:Ljava/lang/Double;", previewConstructor.toString())), "Path preview producer consumes shared XP inputs and builds Learn action");
        var calculator = method(classes.get("Lksb0;"), "c", List.of("Leu8;", "Z", "Luzh;", "D", "Z", "Ljz8;", "Lcom/duolingo/core/experiments/ExperimentsRepository$TreatmentRecord;"), "I");
        check(refs(calculator).contains("Ljava/lang/Math;->ceil(D)D")
                && instructions(calculator).values().stream().anyMatch(i -> i.getOpcode().name.startsWith("mul-double")), "XP preview calculation multiplies original reward then rounds up");
        var renderer = method(classes.get("Lm0m;"), "e", List.of("Lean;", "Lnnr;", "Z", "Lo18;", "I"), "V");
        check(refs(renderer).contains("Lenr;->b:I") && instructions(renderer).values().stream().anyMatch(i -> i instanceof WideLiteralInstruction l && l.getWideLiteral() == 0x7f11003e), "Continue +XP button renders the action expected XP value");
        String surrogate = "Lcom/duolingo/feature/session/CompletedSessionSurrogate;";
        var surrogateFields = fields(classes.get(surrogate));
        check(surrogateFields.entrySet().containsAll(Map.of("B", "Ljava/lang/Double;", "C", "Ljava/lang/Boolean;", "G", "Ljava/lang/Integer;", "O", "Ljava/lang/Integer;").entrySet()), "Diagnostic XP submission scalar field types");
        var serialize = method(classes.get("Lms7;"), "serialize", List.of("Lkotlinx/serialization/encoding/Encoder;", "Ljava/lang/Object;"), "V");
        check(refs(serialize).containsAll(List.of(surrogate + "->B:Ljava/lang/Double;", surrogate + "->C:Ljava/lang/Boolean;", surrogate + "->G:Ljava/lang/Integer;", surrogate + "->O:Ljava/lang/Integer;")), "Serialization reads the four observed fields");
        var sessionFactory = methods(classes.get("Lfs7;")).stream().filter(m -> m.getName().equals("a")
                && m.getReturnType().equals("Lcom/duolingo/core/session/c;") && m.getParameterTypes().size() == 60).toList();
        check(sessionFactory.size() == 1 && sessionFactory.get(0).getParameterTypes().get(0).toString().equals("Lfs7;")
                && sessionFactory.get(0).getParameterTypes().get(10).toString().equals("D")
                && sessionFactory.get(0).getParameterTypes().get(11).toString().equals("Z")
                && sessionFactory.get(0).getParameterTypes().get(58).toString().equals("I")
                && sessionFactory.get(0).getParameterTypes().get(59).toString().equals("I"), "Unique session factory boost arguments 10/11 and default masks 58/59");
        var completedModel = classes.get("Lcom/duolingo/core/session/c;");
        check(fields(completedModel).entrySet().containsAll(Map.of("i", "D", "j", "Z").entrySet()), "Completed-session multiplier and boost flag fields");
        var completedConstructors = methods(completedModel).stream().filter(m -> m.getName().equals("<init>")
                && m.getParameterTypes().size() == 35 && m.getParameterTypes().get(34).toString().equals("Ljava/util/List;")).toList();
        check(completedConstructors.size() == 1, "Unique canonical completed-session constructor");
        check(directParameterStore(completedConstructors.get(0), "i", 8), "Completed multiplier is stored from argument 8");
        check(directParameterStore(completedConstructors.get(0), "j", 9), "Completed boost flag is stored from argument 9");
        var completedEncoder = method(classes.get("Lls7;"), "serialize", List.of("Lkotlinx/serialization/encoding/Encoder;", "Ljava/lang/Object;"), "V");
        check(refs(completedEncoder).containsAll(List.of("Lcom/duolingo/core/session/c;->i:D", "Lcom/duolingo/core/session/c;->j:Z"))
                && refs(completedEncoder).stream().anyMatch(r -> r.startsWith(surrogate + "-><init>")), "Completed-session serializer reads both boost values and builds submission surrogate");
        method(classes.get("Lltb0;"), "deserialize", List.of("Lkotlinx/serialization/encoding/Decoder;"), "Ljava/lang/Object;");
        check(fields(classes.get("Lcom/duolingo/core/xpsummaries/XpSummary;")).entrySet().containsAll(Map.of("a", "I", "b", "J").entrySet()), "Decoded XP summary numeric fields");
        method(classes.get("Lokhttp3/internal/http/CallServerInterceptor;"), "intercept", List.of("Lokhttp3/Interceptor$Chain;"), "Lokhttp3/Response;");
        method(classes.get("Lokhttp3/Response;"), "code", List.of(), "I");
        method(classes.get("Lokhttp3/Request;"), "method", List.of(), "Ljava/lang/String;");
        method(classes.get("Lokhttp3/Request;"), "url", List.of(), "Lokhttp3/HttpUrl;");
        method(classes.get("Lokhttp3/HttpUrl;"), "host", List.of(), "Ljava/lang/String;");
        method(classes.get("Lokhttp3/HttpUrl;"), "encodedPath", List.of(), "Ljava/lang/String;");
        String composerType = "Lo18;", modifierType = "Lz5q;", functionType = "Lkotlin/jvm/functions/Function0;";
        String positionType = "Lcom/duolingo/core/design/compose/components/GroupedListItemPosition;";
        var accountRow = method(classes.get("Lcom/duolingo/settings/main/c;"), "a",
                List.of("Lcom/duolingo/settings/main/AccountItem;", "Loy10;", "Lqqh;", positionType, composerType, "I"), "V");
        method(classes.get("Lcom/duolingo/settings/main/c;"), "b", List.of("Loy10;", "Lqqh;", composerType, "I"), "V");
        check(refs(accountRow).contains("privacySettingsRowItem")
                && refs(accountRow).contains("Lok10;-><init>(ILqqh;)V")
                && instructions(accountRow).values().stream().anyMatch(i -> i instanceof WideLiteralInstruction v && v.getWideLiteral() == 20), "Privacy row uses inspected native router callback 20");
        check(methods(classes.get("Lcom/duolingo/settings/main/AccountItem;")).stream().anyMatch(m -> m.getName().equals("values"))
                && java.util.stream.StreamSupport.stream(classes.get("Lcom/duolingo/settings/main/AccountItem;").getStaticFields().spliterator(), false).anyMatch(f -> f.getName().equals("PRIVACY")), "Privacy account item exists");
        method(classes.get("Loup;"), "v", List.of("Ljava/lang/String;", positionType, modifierType, "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Ljava/lang/Integer;", "Ljava/lang/String;", "F", functionType, "Lerh;", composerType, "I", "I"), "V");
        method(classes.get("Loup;"), "B", List.of("Ljava/lang/String;", modifierType, "Z", composerType, "I", "I"), "V");
        method(classes.get("Loup;"), "C", List.of("I", composerType, modifierType, "Ljava/lang/String;"), "V");
        var nativeToggle = method(classes.get("Loup;"), "F", List.of("Z", "Lqqh;", modifierType, "Lcom/duolingo/core/design/compose/components/inputs/ToggleState;", composerType, "I", "I"), "V");
        method(classes.get("Loup;"), "z", List.of("I", composerType, modifierType, "Ljava/lang/String;", functionType, "Z"), "V");
        var nativeRadioGroup = method(classes.get("Loup;"), "A", List.of("I", "Luv7;", composerType, modifierType), "V");
        method(classes.get("Loup;"), "p", List.of("Ljava/lang/String;", modifierType, "Ljava/lang/String;", functionType, functionType, composerType, "I", "I"), "V");
        var nativePage = method(classes.get("Luxp;"), "w", List.of("Luv7;", modifierType, "Luv7;", composerType, "I", "I"), "V");
        check(classes.get("Lw5q;").getInterfaces().contains(modifierType)
                && java.util.stream.StreamSupport.stream(classes.get("Lw5q;").getStaticFields().spliterator(), false)
                .anyMatch(f -> f.getName().equals("a") && f.getType().equals("Lw5q;")), "Native empty Modifier singleton has the expected interface and field");
        String toggleStateType = "Lcom/duolingo/core/design/compose/components/inputs/ToggleState;";
        check(java.util.stream.StreamSupport.stream(classes.get(toggleStateType).getStaticFields().spliterator(), false)
                .anyMatch(f -> f.getName().equals("ENABLED") && f.getType().equals(toggleStateType)), "Native enabled ToggleState exists");
        var toggleCode = instructions(nativeToggle);
        check(toggleCode.get(39).getOpcode() == Opcode.AND_INT_LIT8
                && ((TwoRegisterInstruction) toggleCode.get(39)).getRegisterB() == 25
                && ((WideLiteralInstruction) toggleCode.get(39)).getWideLiteral() == 4
                && toggleCode.get(62).getOpcode() == Opcode.AND_INT_LIT8
                && ((TwoRegisterInstruction) toggleCode.get(62)).getRegisterB() == 25
                && ((WideLiteralInstruction) toggleCode.get(62)).getWideLiteral() == 8,
                "Toggle default mask bit4 selects Modifier and bit8 selects state");
        String nativeModifier = "Lw5q;->a:Lw5q;", nativeState = toggleStateType + "->ENABLED:" + toggleStateType;
        var oldDefaults = toggleDefaults(nativeToggle, 8, null, null);
        check(oldDefaults.get(0) == null && nativeState.equals(oldDefaults.get(1)),
                "Regression: original null/null/mask8 preserves the null Modifier that caused the device crash");
        check(toggleDefaults(nativeToggle, 0, "providedModifier", "providedState").equals(List.of("providedModifier", "providedState")),
                "Explicit Modifier and state with mask0 survive native default selection");
        check(toggleDefaults(nativeToggle, 12, null, null).equals(List.of(nativeModifier, nativeState)),
                "Both default bits together select native Modifier and enabled state");
        String groupHelper = "Lnm9;->B(ILuv7;Lcuh;Z)Z";
        check(refs(nativeRadioGroup).contains(groupHelper), "Native radio group uses the inspected callback helper");
        var groupCallback = method(classes.get("Lnm9;"), "B", List.of("I", "Luv7;", "Lcuh;", "Z"), "Z");
        check(refs(groupCallback).contains("Luv7;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                && !refs(groupCallback).contains("Luv7;->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
                "Radio group requires Function2, not a scope Function3");
        check(refs(nativePage).contains("Luv7;->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
                "Native settings page body requires scope Function3");
        method(classes.get("Luv7;"), "<init>", List.of("Ljava/lang/Object;", "Z", "I"), "V");
        method(classes.get("Luv7;"), "invoke", List.of("Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;");
        method(classes.get("Lerh;"), "invoke", List.of("Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;");
        method(classes.get("Lgrh;"), "invoke", List.of("Ljava/lang/Object;", "Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;");
        method(classes.get("Lqqh;"), "invoke", List.of("Ljava/lang/Object;"), "Ljava/lang/Object;");
        method(classes.get("Loaz;"), "i", List.of(composerType, "I"), "Ljava/lang/String;");
        method(classes.get("Lok10;"), "<init>", List.of("I", "Lqqh;"), "V");
        method(classes.get("Lcom/duolingo/core/common/compose/DuoComposeView2;"), "<init>", List.of("Landroid/content/Context;"), "V");
        method(classes.get("Lcom/duolingo/core/common/compose/DuoComposeView2;"), "setContent", List.of("Lerh;"), "V");
        method(classes.get("Lp1;"), "e", List.of(), "V");
        method(classes.get("Li2c;"), "a", List.of(composerType), "Lf2c;");
        check(classes.get("Lf2c;").getInterfaces().contains("Lzal;"), "Native theme inherits background color contract");
        method(classes.get("Lzal;"), "u", List.of(), "J");
        method(classes.get("Ldjb0;"), "f", List.of(modifierType, "J", "Lnc20;"), modifierType);
        method(classes.get("Lcom/duolingo/core/mvvm/view/MvvmComposeFragment;"), "onViewCreated", List.of("Landroid/view/View;", "Landroid/os/Bundle;"), "V");
        check(java.util.stream.StreamSupport.stream(classes.get("Luay;").getStaticFields().spliterator(), false).anyMatch(f -> f.getName().equals("N") && f.getType().equals("Luv7;")), "Native privacy navigation arrow composable exists");
        var inventory = classes.get("Lcom/duolingo/data/shop/InventoryItem;");
        List<Method> remaining = methods(inventory).stream().filter(m -> m.getParameterTypes().isEmpty() && m.getReturnType().equals("J")
                && refs(m).contains("Landroid/os/SystemClock;->elapsedRealtime()J")).toList();
        check(remaining.size() == 1, "Unique monotonic remaining-time method");
        var rem = remaining.get(0);
        Set<String> longFields = new HashSet<>();
        for (var i : instructions(rem).values()) if (i instanceof ReferenceInstruction ri && ri.getReference() instanceof FieldReference f
                && f.getDefiningClass().equals(inventory.getType()) && f.getType().equals("J")) longFields.add(f.toString());
        check(longFields.equals(Set.of(inventory.getType() + "->g:J")), "Unique inventory expiry field");
        check(methods(inventory).stream().filter(m -> m.getParameterTypes().isEmpty() && m.getReturnType().equals("Z") && refs(m).contains(rem.toString())).count() == 1, "Unique active check calls remaining time");
        method(classes.get("Lcom/duolingo/core/xpboost/XpBoostSource$Predefined;"), "getLegacyId", List.of(), "Ljava/lang/String;");

        var tick = classes.get(TICK);
        check(fields(tick).equals(Map.of("b", "I")) && tick.getSuperclass().equals("Lcom/duolingo/data/session/state/RampupTimerState;"), "Timer shape and parent");
        method(tick, "<init>", List.of("I"), "V");
        check(refs(method(tick, "toString", List.of(), "Ljava/lang/String;")).contains("Tick(remainingSeconds="), "Timer text anchor");
        var code = instructions(method(classes.get("Lrr10;"), "accept", List.of("Ljava/lang/Object;"), "V"));
        var one = code.get(5);
        check(one.getOpcode() == Opcode.CONST_4 && ((OneRegisterInstruction) one).getRegisterA() == 2 && ((WideLiteralInstruction) one).getWideLiteral() == 1, "Reducer threshold register is constant 1");
        var read = code.get(410);
        check(read.getOpcode() == Opcode.IGET && ((ReferenceInstruction) read).getReference().toString().equals(TICK + "->b:I")
                && ((TwoRegisterInstruction) read).getRegisterA() == 0, "Reducer reads Tick remaining seconds");
        var branch = code.get(412);
        check(branch.getOpcode() == Opcode.IF_GT && ((TwoRegisterInstruction) branch).getRegisterA() == 0
                && ((TwoRegisterInstruction) branch).getRegisterB() == 2 && 412 + ((OffsetInstruction) branch).getCodeOffset() == 417,
                "Reducer decrements only when remaining > 1");
        check(code.get(414).getOpcode() == Opcode.SGET_OBJECT && ((ReferenceInstruction) code.get(414)).getReference().toString().contains("RampupTimerState$Completed;->b:"), "Expiry branch selects Completed");
        check(code.get(417).getOpcode() == Opcode.ADD_INT_LIT8 && ((WideLiteralInstruction) code.get(417)).getWideLiteral() == -1
                && ((ReferenceInstruction) code.get(421)).getReference().toString().equals(TICK + "-><init>(I)V"), "Countdown constructs a new Tick after decrement");
        System.out.println("PASS: " + checks + " static APK assertions; runtime injection and server effects NOT verified.");
    }
    /** Interpret only the exact sample's default-selection block, without executing Compose. */
    static List<Object> toggleDefaults(Method method, int mask, Object modifier, Object state) {
        var code = instructions(method);
        var registers = new HashMap<Integer,Object>();
        registers.put(4, mask & 4); registers.put(7, mask & 8);
        registers.put(6, modifier); registers.put(22, state);
        for (int pc = 108, steps = 0; pc < 123 && steps < 20; steps++) {
            var ins = code.get(pc);
            int next = pc + ins.getCodeUnits();
            switch (ins.getOpcode()) {
                case IF_EQZ -> {
                    Object value = registers.get(((OneRegisterInstruction) ins).getRegisterA());
                    if (value == null || value instanceof Number n && n.intValue() == 0)
                        next = pc + ((OffsetInstruction) ins).getCodeOffset();
                }
                case GOTO -> next = pc + ((OffsetInstruction) ins).getCodeOffset();
                case MOVE_OBJECT, MOVE_OBJECT_FROM16 -> {
                    var move = (TwoRegisterInstruction) ins;
                    registers.put(move.getRegisterA(), registers.get(move.getRegisterB()));
                }
                case SGET_OBJECT -> registers.put(((OneRegisterInstruction) ins).getRegisterA(), ((ReferenceInstruction) ins).getReference().toString());
                default -> throw new AssertionError("Native toggle default-selection block changed");
            }
            pc = next;
        }
        return Arrays.asList(registers.get(13), registers.get(6));
    }
    static boolean directParameterStore(Method method, String field, int parameter) {
        int words = 1;
        for (var t : method.getParameterTypes()) words += t.toString().equals("J") || t.toString().equals("D") ? 2 : 1;
        int register = method.getImplementation().getRegisterCount() - words + 1;
        var origins = new HashMap<Integer,Integer>();
        int index = 0;
        for (var t : method.getParameterTypes()) {
            origins.put(register, index++);
            register += t.toString().equals("J") || t.toString().equals("D") ? 2 : 1;
        }
        for (var ins : method.getImplementation().getInstructions()) {
            String op = ins.getOpcode().name;
            if (op.startsWith("move") && !op.startsWith("move-result") && ins instanceof TwoRegisterInstruction r) {
                Integer source = origins.get(r.getRegisterB());
                if (source == null) origins.remove(r.getRegisterA()); else origins.put(r.getRegisterA(), source);
            } else if (op.startsWith("iput") && ins instanceof ReferenceInstruction ri && ri.getReference() instanceof FieldReference f
                    && f.getName().equals(field) && f.getDefiningClass().equals(method.getDefiningClass())) {
                return Integer.valueOf(parameter).equals(origins.get(((TwoRegisterInstruction) ins).getRegisterA()));
            }
        }
        return false;
    }
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; System.out.println("PASS " + label); }
    static Map<String,String> fields(ClassDef cls) { var out = new HashMap<String,String>(); for (var f : cls.getInstanceFields()) out.put(f.getName(), f.getType()); return out; }
    static List<Method> methods(ClassDef cls) { var out = new ArrayList<Method>(); cls.getMethods().forEach(out::add); return out; }
    static Method method(ClassDef cls, String name, List<String> params, String returns) {
        var found = methods(cls).stream().filter(m -> m.getName().equals(name) && m.getReturnType().equals(returns)
                && m.getParameterTypes().stream().map(Object::toString).toList().equals(params)).toList();
        check(found.size() == 1, "Unique signature " + cls.getType() + "->" + name + params + returns); return found.get(0);
    }
    static Map<Integer,Instruction> instructions(Method method) {
        var out = new LinkedHashMap<Integer,Instruction>(); int pc = 0;
        if (method.getImplementation() != null) for (var i : method.getImplementation().getInstructions()) { out.put(pc, i); pc += i.getCodeUnits(); }
        return out;
    }
    static Set<String> refs(Method method) {
        var out = new HashSet<String>(); for (var i : instructions(method).values()) if (i instanceof ReferenceInstruction ri) out.add(ri.getReference().toString()); return out;
    }
}
