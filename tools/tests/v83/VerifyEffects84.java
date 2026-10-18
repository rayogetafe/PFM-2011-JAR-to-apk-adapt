import java.nio.file.*;
import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;

public class VerifyEffects84 {
 public static void main(String[] args)throws Exception{
  int references=0,methods=0;try(ZipFile jar=new ZipFile(args[0])){
   var entries=jar.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();if(!entry.getName().endsWith(".class"))continue;
    ClassNode c=new ClassNode();new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(c,0);
    for(MethodNode m:c.methods){boolean changed=false;for(AbstractInsnNode instruction:m.instructions)if(instruction instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)instruction;boolean eligibility=call.name.equals("starts")||call.name.equals("minutesForRosterIndex")||call.name.equals("benchImpact");
      if(eligibility&&call.owner.equals("pfmAvailable70"))throw new AssertionError("Unrouted eligibility: "+c.name+"."+m.name);
      if(call.owner.equals("PfmEffectsBridge84")){references++;changed=true;}}
     if(changed){new Analyzer<BasicValue>(new BasicVerifier()).analyze(c.name,m);methods++;}
    }
   }
  }
  if(references<5)throw new AssertionError("Missing eligibility hooks: "+references);
  System.out.println("Legacy effect hooks: "+references+" calls in "+methods+" methods; descriptors and stacks verified");
 }
}
