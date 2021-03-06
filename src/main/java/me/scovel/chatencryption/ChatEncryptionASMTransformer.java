package me.scovel.chatencryption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ChatEncryptionASMTransformer implements IClassTransformer {
	
	public static final Logger TransformLogger = LogManager.getLogger("ChatEncryption");
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
	
		if("net.minecraft.client.entity.EntityPlayerSP".equals(transformedName)){
			boolean obfuscated = !name.equals(transformedName);
			ClassNode nodeler = new ClassNode();
			ClassReader readler = new ClassReader(basicClass);
			readler.accept(nodeler, 0);

			final String methodName = obfuscated ? "g" : "sendChatMessage";
			
			AbstractInsnNode instruction2 = null;
			
			for(MethodNode methodler : nodeler.methods){
				if(methodler.name.equals(methodName) && methodler.desc.equals("(Ljava/lang/String;)V")){
					for(AbstractInsnNode instruction : methodler.instructions.toArray()){
						if(instruction.getOpcode() == Opcodes.ALOAD){
							if(((VarInsnNode)instruction).var == 1 && instruction.getNext().getOpcode() == Opcodes.INVOKESPECIAL){
								instruction2 = instruction;
								break;
							}
						}
					}
					
					if(instruction2 != null){
						methodler.instructions.insert(instruction2, new MethodInsnNode(Opcodes.INVOKESTATIC, "me/scovel/chatencryption/ChatEncryptionHooks", "encrypt", "(Ljava/lang/String;)Ljava/lang/String;", false));
						TransformLogger.info("injected bytecode into: "+name);
					}	
					else{
						throw new RuntimeException("An eagler messed up sendChatMessage's bytecode! Could not find instruction ALOAD_1! Uninstall any new mods");
					}
					break;
				}
			}
			
			ClassWriter writeler = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			nodeler.accept(writeler);
			return writeler.toByteArray();
		}
	
		return basicClass;
	}
}
