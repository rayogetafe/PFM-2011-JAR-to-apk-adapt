import java.util.zip.*;
import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;

/** Checks stack and local-variable types on the actual patched legacy bytecode. */
public class VerifyCore {
    public static void main(String[] args)throws Exception{
        try(ZipFile zip=new ZipFile(args[0])){
            for(String name:new String[]{"bb","ca","pfmSubstitutions"}){
                ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry(name+".class"))).accept(node,0);
                int hooks=0;
                for(MethodNode method:node.methods){
                    boolean changed=false;
                    for(AbstractInsnNode insn:method.instructions)if(insn instanceof MethodInsnNode&&((MethodInsnNode)insn).owner.equals("PfmMatchEngineBridge"))hooks++;
                    for(AbstractInsnNode insn:method.instructions)if(insn instanceof MethodInsnNode&&((MethodInsnNode)insn).owner.equals("PfmMatchEngineBridge"))changed=true;
                    if(changed)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);
                }
                if(hooks!=(name.equals("ca")?2:1))throw new AssertionError(name+" hook count "+hooks);
                System.out.println(name+": verified, unified engine hooks "+hooks);
            }
        }
        if(args.length>1){
            Path path=Paths.get(args[1],"ca.class");ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(node,0);
            for(FieldNode f:node.fields){if(f.name.equals("scene")||f.name.equals("homeDisplay"))f.name="a";else if(f.name.equals("awayDisplay"))f.name="b";}
            for(MethodNode m:node.methods)for(AbstractInsnNode insn:m.instructions)if(insn instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)insn;
                if(f.owner.equals("ca")){if(f.name.equals("scene")||f.name.equals("homeDisplay"))f.name="a";else if(f.name.equals("awayDisplay"))f.name="b";}}
            ClassWriter writer=new ClassWriter(0);node.accept(writer);Files.write(path,writer.toByteArray());
        }
    }
}
