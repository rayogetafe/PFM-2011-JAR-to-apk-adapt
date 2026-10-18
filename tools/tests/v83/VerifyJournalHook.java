import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;
public class VerifyJournalHook {
    public static void main(String[] args)throws Exception{
        try(ZipFile zip=new ZipFile(args[0])){
            ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry("pfmMatchEvents.class"))).accept(node,0);int count=0;
            for(MethodNode method:node.methods)for(AbstractInsnNode insn:method.instructions)if(insn instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)insn;
                if(call.owner.equals("PfmJournalBridge83")){if(!method.name.equals("finishMatchday")||!call.name.equals("sortTableAndRecord")||!call.desc.equals("()V"))throw new AssertionError("Wrong journal hook");new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);count++;}}
            if(count!=1)throw new AssertionError("Expected one post-ledger hook, found "+count);
            System.out.println("Core journal hook: exact original post-match call wrapped, stack verified");
        }
    }
}
