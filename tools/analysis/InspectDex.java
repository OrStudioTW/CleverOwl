import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

class InspectDex {
    public static void main(String[] args) throws Exception {
        var container = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.getDefault());
        var pattern = Pattern.compile(args[1]);
        boolean dump = args.length > 2 && args[2].equals("dump");
        for (var entry : container.getDexEntryNames()) {
            for (var cls : container.getEntry(entry).getDexFile().getClasses()) {
                if (dump && !pattern.matcher(cls.getType()).find()) continue;
                if (dump) {
                    System.out.println("CLASS " + cls.getType());
                    for (var f : cls.getFields()) System.out.println("FIELD " + f);
                }
                for (var method : cls.getMethods()) {
                    var impl = method.getImplementation();
                    if (impl == null) continue;
                    if (dump) System.out.println("METHOD " + method + " registers=" + impl.getRegisterCount());
                    int offset = 0;
                    for (var ins : impl.getInstructions()) {
                        var reference = ins instanceof ReferenceInstruction ri ? ri.getReference() : null;
                        String ref = reference == null ? "" : reference.toString();
                        if (dump) {
                            String regs = "";
                            if (ins instanceof ThreeRegisterInstruction r) regs = "v"+r.getRegisterA()+",v"+r.getRegisterB()+",v"+r.getRegisterC();
                            else if (ins instanceof TwoRegisterInstruction r) regs = "v"+r.getRegisterA()+",v"+r.getRegisterB();
                            else if (ins instanceof OneRegisterInstruction r) regs = "v"+r.getRegisterA();
                            if (ins instanceof FiveRegisterInstruction r) regs = "count="+r.getRegisterCount()+" {"+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG()+"}";
                            if (ins instanceof RegisterRangeInstruction r) regs = "start=v"+r.getStartRegister()+" count="+r.getRegisterCount();
                            String literal = ins instanceof WideLiteralInstruction l ? " literal="+l.getWideLiteral() : "";
                            String target = ins instanceof OffsetInstruction o ? " target="+(offset+o.getCodeOffset()) : "";
                            System.out.println(offset+" "+ins.getOpcode().name+" "+regs+" "+ref+literal+target);
                        } else if (pattern.matcher(ref).find()) {
                            System.out.println(entry+"\t"+method+"\t"+ins.getOpcode().name+"\t"+ref);
                        }
                        offset += ins.getCodeUnits();
                    }
                }
            }
        }
    }
}
